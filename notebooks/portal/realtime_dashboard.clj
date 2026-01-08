^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.realtime-dashboard
  "Portal 实时监控面板示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 实时监控面板nn本页展示如何用定时刷新 + tap> 构造实时监控面板.

;; 示例会模拟指标波动, 并在 Portal 中持续更新视图.

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)

(defonce ^:private dashboard-running? (atom false))
(defonce ^:private dashboard-worker (atom nil))
(defonce ^:private latency-series (atom []))

(defn- next-latency
  "生成下一条延迟数据."
  [{:keys [p95]}]
  (let [base (or p95 150)
        delta (- (rand-int 25) 12)]
    (max 60 (+ base delta))))

(defn- append-series
  "追加新数据并裁剪为固定长度."
  [series point max-size]
  (let [next-series (conj series point)]
    (vec (take-last max-size next-series))))

(defn- build-snapshot
  "构造当前的监控快照."
  [tick]
  (let [last-point (last @latency-series)
        p95 (next-latency last-point)
        point {:tick tick
               :p95 p95}]
    (swap! latency-series append-series point 12)
    {:service "order-service"
     :tick tick
     :status (if (> p95 180) "degraded" "healthy")
     :requests {:p50 (- p95 40)
                :p95 p95
                :p99 (+ p95 30)}
     :errors [{:type :timeout :count (rand-int 4)}
              {:type :validation :count (rand-int 3)}]
     :latency-series @latency-series}))

(defn- dashboard-view
  "将快照转换为组合视图."
  [snapshot]
  {:overview (v/log {:service (:service snapshot)
                     :status (:status snapshot)
                     :tick (:tick snapshot)})
   :requests (v/table [(merge {:metric "p50"} (:requests snapshot))
                       (merge {:metric "p95"} (:requests snapshot))
                       (merge {:metric "p99"} (:requests snapshot))])
   :errors (v/table (:errors snapshot))
   :latency (v/vega-lite
             {:data {:values (:latency-series snapshot)}
              :mark {:type "line" :point true}
              :encoding {:x {:field "tick" :type "ordinal"}
                         :y {:field "p95" :type "quantitative"}}
              :width 420
              :height 200})})

^{::clerk/visibility {:code :show :result :show}}
(defn start-dashboard!
  "启动定时刷新, 每 500ms 推送一次数据."
  []
  (when-not @dashboard-running?
    (reset! dashboard-running? true)
    (reset! dashboard-worker
            (future
              (loop [tick 1]
                (when @dashboard-running?
                  (p/clear)
                  (tap> (dashboard-view (build-snapshot tick)))
                  (Thread/sleep 500)
                  (recur (inc tick)))))))
  :running)

^{::clerk/visibility {:code :show :result :show}}
(defn stop-dashboard!
  "停止定时刷新."
  []
  (reset! dashboard-running? false)
  (when-let [worker @dashboard-worker]
    (future-cancel worker)
    (reset! dashboard-worker nil))
  :stopped)

;; 调用 `(start-dashboard!)` 开始推送, 用 `(stop-dashboard!)` 停止.n如果需要清屏, 可以执行 `(portal.api/clear)` 或 `(p/clear :all)`.

(comment
  (start-dashboard!)
  (stop-dashboard!)
  )
