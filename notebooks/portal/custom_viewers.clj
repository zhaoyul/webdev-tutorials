^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.custom-viewers
  "Portal 自定义展示与快捷命令示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))


;; # 自定义展示与快捷命令

;; Portal 支持为数据附加 viewer 元信息, 也可以注册快捷命令, 便于在命令面板中复用常用操作.

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)


;; ## 自定义发送函数

;;下面的函数会把值包装成 log viewer, 并注册到命令面板.

^{::clerk/visibility {:code :show :result :show}}
(defn capture!
  "将值发送到 Portal, 并附带简单上下文."
  [value]
  (p/submit (v/log {:label "capture"
                    :value value})))

^{::clerk/visibility {:code :show :result :show}}
(p/register! #'capture!)

;; 调用 `(capture! {:status "ok" :at (java.time.Instant/now)})` 就能把数据发送到 Portal.

;; ## 指定 viewernnPortal viewer 可以直接包裹值, 用于强制渲染方式.

^{::clerk/visibility {:code :show :result :show}}
(def metrics
  [{:metric "cpu" :value 0.42 :unit "%"}
   {:metric "mem" :value 0.73 :unit "%"}
   {:metric "latency" :value 120 :unit "ms"}])

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/table metrics))

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/markdown "### 备注\nPortal 适合在 REPL 中作为实时观测面板使用."))


;; ## 小结

;; - `portal.api/register!` 可把函数注册到命令面板.
;; - `portal.viewer/*` 可指定渲染方式, 让同一份数据有更清晰的展示效果.
