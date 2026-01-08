^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.advanced-scenarios
  "Portal 高级展示场景."
  (:require [clojure.datafy :as datafy]
            [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 高级展示场景

;; 本页聚焦一些更贴近日常开发的 Portal 展示方式, 包括时间序列可视化, 性能采样, HTTP 跟踪与异常定位.

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)

;; ## 1. 时间序列可视化

;; 使用 `portal.viewer/vega-lite` 可以快速生成趋势图, 适合性能指标或业务指标走势.

^{::clerk/visibility {:code :show :result :show}}
(def latency-series
  [{:minute "10:00" :p50 85 :p95 140}
   {:minute "10:05" :p50 92 :p95 160}
   {:minute "10:10" :p50 88 :p95 150}
   {:minute "10:15" :p50 95 :p95 180}
   {:minute "10:20" :p50 90 :p95 155}])

^{::clerk/visibility {:code :show :result :show}}
(p/submit
 (v/vega-lite
  {:data {:values latency-series}
   :mark {:type "line" :point true}
   :encoding {:x {:field "minute" :type "ordinal" :title "时间"}
              :y {:field "p95" :type "quantitative" :title "P95 延迟"}
              :color {:value "#f97316"}}
   :width 480
   :height 220}))


;; ## 2. 性能采样

;; 将耗时包装为 `duration-ms` 后再放入表格, 便于对比不同阶段的性能差异.

^{::clerk/visibility {:code :show :result :show}}
(def latency-samples
  [{:step "加载配置" :duration-ms 32}
   {:step "请求数据库" :duration-ms 128}
   {:step "渲染响应" :duration-ms 56}])

^{::clerk/visibility {:code :show :result :show}}
(p/submit
 (v/table
  (mapv (fn [{:keys [step duration-ms]}]
          {:step step
           :duration duration-ms
           :view (v/duration-ms duration-ms)})
        latency-samples)))

;; ## 3. HTTP 跟踪

;; `portal.viewer/http` 会突出展示 HTTP 相关字段, 便于快速定位请求问题.

^{::clerk/visibility {:code :show :result :show}}
(p/submit
 (v/http {:method :get
          :url "/api/orders/1002"
          :status 200
          :duration-ms 148
          :headers {"x-trace-id" "trace-7f3a"}}))


;; ## 4. 异常定位

;;`portal.viewer/ex` 会以更友好的方式展示异常数据.

^{::clerk/visibility {:code :show :result :show}}
(defn sample-error
  []
  (try
    (/ 1 0)
    (catch Exception ex
      (p/submit (v/ex (datafy/datafy ex))))))

^{::clerk/visibility {:code :show :result :show}}
(sample-error)


;; ## 5. 实时流式 tap>

;; 使用 `tap>` 结合后台任务, 可以在 Portal 中观察实时数据流.

^{::clerk/visibility {:code :show :result :show}}
(defn simulate-stream!
  "模拟持续输出数据流."
  []
  (future
    (doseq [tick (range 1 6)]
      (tap> (v/log {:stream "orders"
                   :tick tick
                   :payload {:order/id (+ 1000 tick)
                             :amount (* tick 42)}}))
      (Thread/sleep 200))))

^{::clerk/visibility {:code :show :result :show}}
(simulate-stream!)

;; ## 小结

;; - `vega-lite` 适合可视化趋势与对比数据.
;; - `duration-ms` + `table` 便于做性能对照.
;; - `http` 与 `ex` 可快速定位请求与异常信息.
;; - `tap>` 可以做轻量级流式观测.
