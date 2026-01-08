^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.multi-service-dashboard
  "Portal 多服务对比面板示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 多服务对比面板

;; 本页展示如何在 Portal 中对比多个服务的关键指标, 适合用于联调或全链路压测观察.

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
   {:service "payment-service" :baseline 180}])

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

;; ## 对比表格nn用表格展示各服务的关键指标, 便于横向对比.

^{::clerk/visibility {:code :show :result :show}}
(defn build-snapshot
  "生成当前时刻的多服务快照."
  []
  (mapv next-metric services))

^{::clerk/visibility {:code :show :result :show}}
(def snapshot (build-snapshot))

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/table snapshot))


;; ## 可视化对比

;; 用 vega-lite 画出各服务的 P95 延迟柱状图.

^{::clerk/visibility {:code :show :result :show}}
(p/submit
 (v/vega-lite
  {:data {:values snapshot}
   :mark {:type "bar"}
   :encoding {:x {:field "service" :type "nominal" :title "服务"}
              :y {:field "p95" :type "quantitative" :title "P95 延迟"}
              :color {:field "status" :type "nominal"}}
   :width 460
   :height 240}))

;; ## 小结

;; - 表格适合横向比对多指标.
;; - 柱状图适合快速发现哪一项偏高.
;; - 可以把该输出放入实时刷新逻辑, 形成多服务监控面板.
