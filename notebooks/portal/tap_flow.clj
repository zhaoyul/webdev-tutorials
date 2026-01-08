^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.tap-flow
  "Portal 的 tap> 工作流示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # tap> 工作流

;; 本页展示 tap> 在数据处理链路中的用法.n建议先在 `portal.overview` 中打开 Portal, 或使用 `ensure-portal!` 自动打开.

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)


;; ## 示例数据

;; 我们用一组订单数据, 逐步加工并通过 tap> 输出过程状态.

^{::clerk/visibility {:code :show :result :show}}
(def orders
  [{:order/id 1001 :user "张三" :unit-price 18 :qty 2}
   {:order/id 1002 :user "李四" :unit-price 120 :qty 1}
   {:order/id 1003 :user "王五" :unit-price 35 :qty 4}])

^{::clerk/visibility {:code :show :result :show}}
(defn enrich-order
  "补充订单金额并通过 tap> 输出中间结果."
  [order]
  (let [total (* (:unit-price order) (:qty order))
        enriched (assoc order :total total)]
    (tap> (v/log {:stage :enrich
                 :order enriched}))
    enriched))

^{::clerk/visibility {:code :show :result :show}}
(defn expensive-orders
  "筛选总额超过阈值的订单, 并发送 tap> 追踪结果."
  [threshold input-orders]
  (let [result (->> input-orders
                    (map enrich-order)
                    (filter #(> (:total %) threshold))
                    (vec))]
    (tap> (v/log {:stage :filter
                 :threshold threshold
                 :count (count result)}))
    result))

^{::clerk/visibility {:code :show :result :show}}
(expensive-orders 50 orders)


;; ## 小结

;; - `tap>` 适合埋点式输出, 不影响主流程返回值.
;; - `portal.viewer/log` 可以让每次输出带上上下文.
;; - 当输出过多时, 可使用 `portal.api/clear` 清空面板.
