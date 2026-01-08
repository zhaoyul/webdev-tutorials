^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.multi-service-realtime
  "Portal 多服务实时监控面板示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 多服务实时监控面板
;; 本页将多服务指标与定时刷新结合, 用于实时观察多个服务的健康状态.

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)

^{::clerk/visibility {:code :show :result :show}}
(def services
  [{:service "order-service" :baseline 150}
   {:service "inventory-service" :baseline 120}
   {:service "payment-service" :baseline 180}
   {:service "shipping-service" :baseline 140}])

(defonce ^:private multi-running? (atom false))
(defonce ^:private multi-worker (atom nil))

(defn- next-metric
  "生成单个服务的随机指标快照."
  [{:keys [service baseline]}]
  (let [p95 (+ baseline (- (rand-int 30) 15))]
    {:service service
     :status (if (> p95 (+ baseline 20)) "degraded" "healthy")
     :p50 (- p95 40)
     :p95 p95
     :p99 (+ p95 35)
     :errors (+ 0 (rand-int 4))
     :throughput (+ 80 (rand-int 40))}))

(defn- build-snapshot
  "生成当前时刻的多服务快照."
  []
  (mapv next-metric services))

(defn- snapshot-view
  "将快照包装为组合视图."
  [snapshot]
  {:summary (v/table snapshot)
   :p95-chart (v/vega-lite
               {:data {:values snapshot}
                :mark {:type "bar"}
                :encoding {:x {:field "service" :type "nominal" :title "服务"}
                           :y {:field "p95" :type "quantitative" :title "P95 延迟"}
                           :color {:field "status" :type "nominal"}}
                :width 480
                :height 240})})

^{::clerk/visibility {:code :show :result :show}}
(defn start-multi-dashboard!
  "启动多服务实时刷新, 每 800ms 推送一次快照."
  []
  (when-not @multi-running?
    (reset! multi-running? true)
    (reset! multi-worker
            (future
              (loop []
                (when @multi-running?
                  (tap> (snapshot-view (build-snapshot)))
                  (Thread/sleep 800)
                  (recur))))))
  :running)

^{::clerk/visibility {:code :show :result :show}}
(defn stop-multi-dashboard!
  "停止多服务实时刷新."
  []
  (reset! multi-running? false)
  (when-let [worker @multi-worker]
    (future-cancel worker)
    (reset! multi-worker nil))
  :stopped)

;; 执行 `(start-multi-dashboard!)` 开始推送, 需要停止时执行 `(stop-multi-dashboard!)`.

(comment
  (start-multi-dashboard!)
  (stop-multi-dashboard!)
  )
