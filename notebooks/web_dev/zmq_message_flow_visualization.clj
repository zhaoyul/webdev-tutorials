;; # ZeroMQ 消息流转可视化页面

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns web-dev.zmq-message-flow-visualization
  (:require [clojure.string :as str]
            [ezzmq.core :as zmq]
            [nextjournal.clerk :as clerk])
  (:import [java.util UUID]
           [org.zeromq ZMQ$Error ZMQException]))

;; 本页面面向需要“直观看到” ZeroMQ 消息如何在各个角色之间流转的同事.
;;
;; 设计目标:
;; - 用一个可重复运行的最小场景, 记录 send/recv 的事件流
;; - 把事件流渲染成两种视图:
;;   1) sequenceDiagram: 按时间顺序展示消息方向
;;   2) flowchart: 聚合展示消息通道与次数
;;
;; 参考风格: core.async.flow-monitor 的“看得到流动”.

;; ## 0. Mermaid 查看器(复用 Clerk 之书里的实现)

(def mermaid-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require {:package ["mermaid@8.14/dist/mermaid.js"]}
                    (fn [mermaid]
                      [:div {:ref (fn [el]
                                    (when el
                                      ;; startOnLoad=false, 每次显式 render.
                                      (.initialize mermaid (clj->js {:startOnLoad false}))
                                      (.render mermaid (str (gensym "mermaid")) value
                                               #(set! (.-innerHTML el) %))))}])]))})

(defn mermaid
  "渲染 Mermaid 文本为图."
  [s]
  (clerk/with-viewer mermaid-viewer s))

;; 说明: 与 ezzmq 的完整测试场景一致, 这里切换到 :zmq.context, 确保 socket 关闭不会拖慢页面求值.
(defmacro with-fast-context
  [& body]
  `(binding [ezzmq.context/*context-type* :zmq.context]
     (ezzmq.context/with-new-context ~@body)))

;; `zmq/worker-thread` 基于 `future`, 在脚本模式下可能导致 JVM 退出延迟.
;; 本页面用 daemon 线程承载 worker, 并用 promise 返回结果.
(defmacro worker-daemon
  [{:keys [on-interrupt] :as _opts} & body]
  `(let [p# (promise)
         run# (bound-fn []
                (try
                  (deliver p# (do ~@body))
                  (catch ZMQException e#
                    (let [eterm# (.getCode ZMQ$Error/ETERM)]
                      (if (= eterm# (.getErrorCode e#))
                        (deliver p# ((or ~on-interrupt (fn [] nil))))
                        (deliver p# e#))))
                  (catch Throwable t#
                    (deliver p# t#))))
         t# (doto (Thread. run#)
              (.setDaemon true)
              (.setName (str "zmq-flow-daemon-" (UUID/randomUUID))))]
     (.start t#)
     p#))

;; ## 1. 事件模型: 把 send/recv 统一记为“消息事件”

(defn now-ms [] (System/currentTimeMillis))

(defn safe-id
  "把角色名转为 Mermaid 可用的 participant id."
  [s]
  (let [s (-> s
              (str/replace #"[^0-9A-Za-z_]" "_")
              (str/replace #"__+" "_"))]
    (if (re-matches #"^[0-9].*" s) (str "p_" s) s)))

(defn truncate
  [s n]
  (let [s (str s)]
    (if (<= (count s) n) s (str (subs s 0 n) "..."))))

(defn summarize-frames
  "把多帧消息压缩成一行 label, 用于图里显示."
  [frames]
  (let [frames (cond
                 (nil? frames) []
                 (sequential? frames) frames
                 :else [frames])]
    (let [s (->> frames
                 (map #(truncate % 60))
                 (str/join " | "))]
      (-> s
          (truncate 120)
          ;; Mermaid 对换行敏感, 这里把换行打平.
          (str/replace #"\n" " ")))))

(defn log!
  [log-atom ev]
  (swap! log-atom
         (fn [xs]
           (let [ev (assoc ev :t (now-ms) :n (count xs))]
             (conj xs ev)))))

(defn traced-send!
  [log-atom {:keys [from to kind frames] :as ev} socket frames*]
  (let [frames* (cond
                  (some? frames) frames
                  (sequential? frames*) frames*
                  :else [frames*])]
    (log! log-atom (merge {:op :send :from from :to to :kind kind :frames frames*} (dissoc ev :frames)))
    (zmq/send-msg socket (if (= 1 (count frames*)) (first frames*) frames*))))

(defn traced-recv!
  [log-atom {:keys [from to kind] :as ev} socket opts]
  (let [msg (zmq/receive-msg socket opts)]
    (when msg
      (log! log-atom (merge {:op :recv :from from :to to :kind kind :frames msg} ev)))
    msg))

;; ## 2. 从事件流生成图

(defn events->sequence-diagram
  [{:keys [participants events]}]
  (let [parts (or participants
                  (->> events
                       (mapcat (juxt :from :to))
                       (remove nil?)
                       distinct
                       vec))
        part-lines (map (fn [p] (format "  participant %s as %s" (safe-id p) p)) parts)
        msg-lines
        (for [{:keys [op from to kind frames]} (sort-by :n events)
              :when (and from to (= op :send))]
          (format "  %s->>%s: %s (%s)"
                  (safe-id from)
                  (safe-id to)
                  (summarize-frames frames)
                  (name kind)))]
    (str/join
     "\n"
     (concat
      ["sequenceDiagram"]
      part-lines
      msg-lines))))

(defn events->flowchart
  [{:keys [events]}]
  (let [edges (->> events
                   (filter #(= :send (:op %)))
                   (group-by (juxt :from :to :kind))
                   (map (fn [[[from to kind] xs]]
                          {:from from :to to :kind kind :n (count xs)}))
                   (sort-by (juxt :from :to :kind)))]
    (str/join
     "\n"
     (concat
      ["flowchart LR"]
      (for [{:keys [from to kind n]} edges]
        (format "  %s[%s] -->|%s x%d| %s[%s]"
                (safe-id from) from
                (name kind) n
                (safe-id to) to))))))

;; ## 3. Demo: 一个“网关-服务-消费者”的完整消息链路

(defn inproc
  [prefix]
  (str "inproc://" prefix "-" (UUID/randomUUID)))

(defn await!
  [f ms label]
  (let [v (deref f ms ::timeout)]
    (when (= v ::timeout)
      (throw (ex-info (str "等待超时: " label) {:timeout-ms ms})))
    (when (instance? Throwable v)
      (throw v))
    v))

(defn run-demo!
  "运行一次 demo, 返回 {:events .. :result .. :diagrams ..}."
  []
  (with-fast-context
    (let [log (atom [])
          req-rep-addr (inproc "req-rep")
          pub-addr (inproc "pub")
          pub-ready-addr (inproc "pub-ready")
          metrics-addr (inproc "metrics")
          svc-bound (promise)
          svc-f
          (worker-daemon {}
                             (let [rep (zmq/socket :rep {:bind req-rep-addr})
                                   pub (zmq/socket :pub {:bind pub-addr})
                                   ready-pull (zmq/socket :pull {:bind pub-ready-addr})]
                               (deliver svc-bound :bound)
                               (or (traced-recv! log {:from "audit-consumer" :to "order-service" :kind :ready}
                                                 ready-pull {:stringify true :timeout 1500})
                                   (throw (ex-info "order-service 等待 READY 超时" {})))
                               (let [req (or (traced-recv! log {:from "gateway" :to "order-service" :kind :req}
                                                           rep {:stringify true :timeout 1500})
                                             (throw (ex-info "order-service 等待请求超时" {})))
                                     reply ["OK" (pr-str {:service "order-service" :request req})]
                                     audit ["audit" (pr-str {:event :order/created :request req})]]
                                 (traced-send! log {:from "order-service" :to "gateway" :kind :rep}
                                               rep reply)
                                 (traced-send! log {:from "order-service" :to "audit-consumer" :kind :pub}
                                               pub audit)
                                 {:req req :reply reply :audit audit})))
          _ (or (deref svc-bound 1500 nil) (throw (ex-info "等待 order-service bind 超时" {})))
          consumer-f
          (worker-daemon {}
                             (let [sub (zmq/socket :sub {:connect pub-addr :subscribe "audit"})
                                   pull (zmq/socket :pull {:bind metrics-addr})
                                   ready-push (zmq/socket :push {:connect pub-ready-addr})
                                   _ (traced-send! log {:from "audit-consumer" :to "order-service" :kind :ready}
                                                   ready-push "READY")
                                   audit (atom nil)
                                   metric (atom nil)]
                               (zmq/polling {:receive-opts {:stringify true}}
                                            [sub  :pollin [msg] (reset! audit msg)
                                             pull :pollin [msg] (reset! metric msg)]
                                            (loop [deadline (+ (System/currentTimeMillis) 2000)]
                                              (when (>= (System/currentTimeMillis) deadline)
                                                (throw (ex-info "audit-consumer 超时: 未收到预期消息"
                                                                {:audit @audit :metric @metric})))
                                              (zmq/poll {:timeout 50})
                                              (if (and @audit @metric)
                                                {:audit @audit :metric @metric}
                                                (recur deadline))))))]
      (let [gateway
            (let [req (zmq/socket :req {:connect req-rep-addr})
                  push (zmq/socket :push {:connect metrics-addr})
                  request ["CREATE_ORDER" (pr-str {:order/id 42 :amount 100})]
                  _ (traced-send! log {:from "gateway" :to "order-service" :kind :req} req request)
                  reply (or (traced-recv! log {:from "order-service" :to "gateway" :kind :rep}
                                          req {:stringify true :timeout 1500})
                            (throw (ex-info "gateway 等待 reply 超时" {})))
                  _ (traced-send! log {:from "gateway" :to "audit-consumer" :kind :push}
                                  push ["metric" (pr-str {:name :order/create :ok true})])]
              {:request request :reply reply})
            svc (await! svc-f 2000 "order-service")
            consumer (await! consumer-f 2000 "audit-consumer")
            events @log
            seq (events->sequence-diagram {:events events})
            flow (events->flowchart {:events events})]
        {:events (map (fn [ev] (assoc ev :idx (:n ev))) events)
         :result {:gateway gateway :service svc :consumer consumer}
         :diagrams {:sequence seq :flowchart flow}}))))

;; ## 4. 页面展示

;; 注意: 本页面在加载时会运行一次 demo, 生成事件与图. 之后的展示都复用 `demo`.
(def demo (run-demo!))

^{:nextjournal.clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true
  ::clerk/budget nil}
{:result (:result demo)
 :events-preview (take 12 (:events demo))
 :hint "事件表默认只展示前 12 条, 完整事件流见下方表格."
 :diagrams-preview (update-vals (:diagrams demo) #(subs % 0 (min 500 (count %))))}

;; ### 4.1 Sequence: 按时间顺序的消息方向图
(mermaid (get-in demo [:diagrams :sequence]))

;; ### 4.2 Flow: 聚合后的消息通道与次数
(mermaid (get-in demo [:diagrams :flowchart]))

;; ### 4.3 事件表: 用于精确对照 send/recv
^{:nextjournal.clerk/visibility {:code :show :result :show}}
(clerk/table
 (map (fn [{:keys [idx t op from to kind frames]}]
        {:idx idx
         :t t
         :op (name op)
         :from from
         :to to
         :kind (name kind)
         :frames (summarize-frames frames)})
      (get demo :events)))

;; 你可以把 `run-demo!` 替换成 `ROUTER/DEALER` 或 `PUB/SUB + proxy` 的实际拓扑,
;; 只要持续产出同样的事件结构, 这套渲染就能复用.
