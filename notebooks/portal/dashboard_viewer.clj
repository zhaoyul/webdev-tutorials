^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.dashboard-viewer
  "Portal 组合仪表盘示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 组合仪表盘


;; 本页展示如何将多个 Portal viewer 组合成一个仪表盘, 用于实时观察服务状态.n数据本身保持纯 map, 通过 viewer 包装实现不同视图的组合输出.

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
(def dashboard-snapshot
  {:service "order-service"
   :status "healthy"
   :release "2025.01.08"
   :requests {:p50 86 :p95 142 :p99 210}
   :db {:pool "primary" :active 12 :idle 4 :queue 1}
   :errors [{:type :timeout :count 3}
            {:type :validation :count 1}]
   :latency-series
   [{:minute "10:00" :p95 140}
    {:minute "10:05" :p95 165}
    {:minute "10:10" :p95 150}
    {:minute "10:15" :p95 175}
    {:minute "10:20" :p95 160}]})

;; ## 组合展示nn用 `portal.viewer/log`, `table`, `vega-lite` 搭配, 构造一个聚合面板.

^{::clerk/visibility {:code :show :result :show}}
(defn dashboard-view
  "将快照转换为 Portal 友好的组合展示."
  [snapshot]
  {:overview (v/log {:service (:service snapshot)
                     :status (:status snapshot)
                     :release (:release snapshot)})
   :requests (v/table [(merge {:metric "p50"} (:requests snapshot))
                       (merge {:metric "p95"} (:requests snapshot))
                       (merge {:metric "p99"} (:requests snapshot))])
   :errors (v/table (:errors snapshot))
   :latency (v/vega-lite
             {:data {:values (:latency-series snapshot)}
              :mark {:type "line" :point true}
              :encoding {:x {:field "minute" :type "ordinal"}
                         :y {:field "p95" :type "quantitative"}}
              :width 420
              :height 200})
   :db (v/table [(:db snapshot)])})

^{::clerk/visibility {:code :show :result :show}}
(p/submit (dashboard-view dashboard-snapshot))


;; ## 小结

;; - `dashboard-view` 只做视图包装, 业务数据保持纯 map.
;; - 可以周期性刷新快照并 `tap>` 到 Portal, 实现轻量监控面板.
