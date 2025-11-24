(ns ladder.sim)

;; 最小可运行的梯形图执行器, 聚焦 NO/NC/RE/FE/TON/COIL 指令,
;; 以便在 Clerk 中做教学演示. 结构遵循 notebooks/plc/design.org 中的约定.

(def ^:private base->ms
  {:ms 1 :10ms 10 :100ms 100 :sec 1000 :min 60000})

(def demo-program
  {:meta {:name "ton-demo" :desc "NO→TON→COIL 计时示例"}
   :networks
   [{:id 0
     :rows 1
     :cols 3
     :cells [[{:code :NO   :data [{:type :I :value 0}] :bar? false :state false}
              {:code :TON  :data [{:type :T :value 0}
                                   {:type :K :value 3}
                                   {:type :BASE :value :sec}]
               :bar? false :state false}
              {:code :COIL :data [{:type :Q :value 0}] :bar? false :state false}]]}]})

(defn- assoc-bit
  "将布尔值写入指定索引, 不足部分用 false 填充."
  [bits idx v]
  (let [need (inc (long idx))
        padded (if (< (count bits) need)
                 (into bits (repeat (- need (count bits)) false))
                 bits)]
    (assoc padded idx (boolean v))))

(defn- bit-get [coll k idx]
  (get (get coll k []) (long idx) false))

(defn- bool-from-arg
  "从当前或上一周期读取布尔参数."
  [ctx {:keys [type value]} prev?]
  (case type
    :I (bit-get (if prev? (:prev-io ctx) (:io ctx)) :I value)
    :Q (bit-get (if prev? (:prev-io ctx) (:io ctx)) :Q value)
    :M (bit-get (if prev? (:prev-io ctx) (:io ctx)) :M value)
    :K (boolean value)
    false))

(defn- write-target [ctx {:keys [type value]} v]
  (case type
    :Q (update ctx :io #(update % :Q assoc-bit value v))
    :M (update ctx :io #(update % :M assoc-bit value v))
    ctx))

(defn- ensure-timer [timers idx]
  (let [need (inc (long idx))
        blank {:acc 0 :base :ms :done? false :running? false :last-ts nil}
        padded (if (< (count timers) need)
                 (into timers (repeat (- need (count timers)) blank))
                 timers)]
    padded))

(defn- ton-step [ctx data in now-ms]
  (let [timer-idx (some-> data first :value long)
        preset (or (some->> data (filter #(= (:type %) :K)) first :value) 0)
        base (or (some->> data (filter #(= (:type %) :BASE)) first :value) :ms)
        timers (ensure-timer (:timers ctx) timer-idx)
        timer (get timers timer-idx)
        now (long now-ms)
        base-ms (get base->ms base 1)
        preset-ms (* base-ms (long preset))]
    (if (and in (pos? preset-ms))
      (let [last-ts (or (:last-ts timer) now)
            acc (+ (:acc timer) (- now last-ts))
            done? (>= acc preset-ms)
            next {:acc (min acc preset-ms)
                  :base base
                  :running? (not done?)
                  :done? done?
                  :last-ts now}]
        {:ctx (assoc ctx :timers (assoc timers timer-idx next))
         :out done?
         :cell-state {:acc (:acc next) :done? done?}})
      (let [next {:acc 0 :base base :running? false :done? false :last-ts now}]
        {:ctx (assoc ctx :timers (assoc timers timer-idx next))
         :out false
         :cell-state {:acc 0 :done? false}}))))

(defn- exec-cell [ctx cell in now-ms]
  (let [{:keys [code data]} cell]
    (case code
      :NO (let [ok (bool-from-arg ctx (first data) false)
                out (and in ok)]
            {:ctx ctx :out out :cell (assoc cell :state out)})
      :NC (let [ok (not (bool-from-arg ctx (first data) false))
                out (and in ok)]
            {:ctx ctx :out out :cell (assoc cell :state out)})
      :RE (let [curr (bool-from-arg ctx (first data) false)
                prev (bool-from-arg ctx (first data) true)
                edge (and curr (not prev))
                out (and in edge)]
            {:ctx ctx :out out :cell (assoc cell :state out)})
      :FE (let [curr (bool-from-arg ctx (first data) false)
                prev (bool-from-arg ctx (first data) true)
                edge (and prev (not curr))
                out (and in edge)]
            {:ctx ctx :out out :cell (assoc cell :state out)})
      :TON (let [{:keys [ctx out cell-state]} (ton-step ctx data in now-ms)]
             {:ctx ctx :out out :cell (merge cell {:state out} cell-state)})
      :COIL (let [target (first data)
                  ctx' (write-target ctx target in)]
              {:ctx ctx' :out in :cell (assoc cell :state in)})
      ;; 未覆盖的指令作为直通处理, 便于扩展.
      {:ctx ctx :out in :cell (assoc cell :state in)})))

(defn- run-row [ctx row now-ms]
  (reduce (fn [[ctx in acc] cell]
            (let [{:keys [ctx out cell]} (exec-cell ctx cell in now-ms)]
              [ctx out (conj acc cell)]))
          [ctx true []]
          row))

(defn- run-network [ctx network now-ms]
  (let [[ctx' rows]
        (reduce (fn [[ctx acc] row]
                  (let [[ctx'' _ row'] (run-row ctx row now-ms)]
                    [ctx'' (conj acc row')]))
                [ctx []]
                (:cells network))]
    [ctx' (assoc network :cells rows)]))

(defn- run-program [ctx program now-ms]
  (let [[ctx' nets]
        (reduce (fn [[ctx acc] net]
                  (let [[ctx'' net'] (run-network ctx net now-ms)]
                    [ctx'' (conj acc net')]))
                [ctx []]
                (:networks program))]
    [ctx' (assoc program :networks nets)]))

(defn ->ctx
  "创建执行上下文, 默认扫描间隔 1s."
  ([program] (->ctx program {}))
  ([program {:keys [scan-ms]}]
   {:program program
    :io {:I [] :Q [] :M []}
    :prev-io {:I [] :Q [] :M []}
    :timers []
    :state {:mode :paused :err nil}
    :options {:scan-ms (or scan-ms 1000)}}))

(defn apply-inputs
  "将输入向量写入 :io/:I, 保留长度对齐."
  [ctx inputs]
  (if (nil? inputs)
    ctx
    (assoc-in ctx [:io :I]
              (reduce-kv (fn [v idx val] (assoc-bit v idx val))
                         (get-in ctx [:io :I] [])
                         (vec inputs)))))

(defn snapshot
  "提取简化状态用于展示."
  [ctx]
  {:I (get-in ctx [:io :I])
   :Q (get-in ctx [:io :Q])
   :M (get-in ctx [:io :M])
   :timers (map #(select-keys % [:acc :base :done?]) (:timers ctx))
   :mode (get-in ctx [:state :mode])
   :err  (get-in ctx [:state :err])})

(defn scan-step
  "执行一次扫描周期. inputs 为布尔向量, now-ms 可注入时间戳以便演示."
  ([ctx inputs] (scan-step ctx inputs (System/currentTimeMillis)))
  ([ctx inputs now-ms]
   (let [ctx0 (-> ctx
                  (assoc :prev-io (:io ctx))
                  (apply-inputs inputs)
                  (assoc-in [:state :mode] :running)
                  (assoc :last-tick now-ms))
         [ctx1 program'] (run-program ctx0 (:program ctx0) now-ms)]
     (assoc ctx1 :program program'))))
