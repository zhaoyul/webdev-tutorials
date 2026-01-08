^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.overview
  "Portal 入门与基本配置."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]))

;; # Portal 入门n

;; Portal 是一个轻量的数据探查工具, 适合在 REPL 中快速浏览和定位问题.
;; 本系列笔记会使用 Portal 的 tap> 工作流, 并展示常用视图与自定义展示方式.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 打开 Portal\n\n下面的函数会打开 Portal 并注册 tap> 目标.\n如果需要指定主题或窗口标题, 可以传入选项, 例如 `{:theme :portal.colors/nord :window-title \"Portal Demo\"}`.")

^{::clerk/visibility {:code :show :result :hide}}
(defonce portal-instance (atom nil))

^{::clerk/visibility {:code :show :result :show}}
(defn open-portal!
  "打开 Portal, 并注册 tap> 目标, 返回 Portal 实例."
  ([] (open-portal! {}))
  ([opts]
   (let [portal (p/open opts)]
     (reset! portal-instance portal)
     (add-tap #'p/submit)
     portal)))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "执行 `(open-portal!)` 后, 可以在 Portal 中看到后续的 tap> 数据.\n")

^{::clerk/visibility {:code :show :result :show}}
(open-portal!)

^{::clerk/visibility {:code :show :result :show}}
(tap> {:event "portal-ready"
       :message "Portal 已接入 tap>"
       :at (java.time.Instant/now)})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 常用清理\n\n下面是清理和关闭的辅助函数, 需要时再调用即可.")

^{::clerk/visibility {:code :show :result :hide}}
(defn clear-portal!
  "清空 Portal 面板中的历史值."
  []
  (when-let [portal @portal-instance]
    (p/clear portal)))

^{::clerk/visibility {:code :show :result :hide}}
(defn close-portal!
  "关闭 Portal 并移除 tap> 目标."
  []
  (when-let [portal @portal-instance]
    (remove-tap #'p/submit)
    (p/close portal)
    (reset! portal-instance nil)))


;; ## 下一步nn继续查看以下笔记:
;; - `tap_flow.clj`: tap> 工作流示例
;; - `inspect.clj`: 常用视图和过滤
;; - `custom_viewers.clj`: 自定义展示与快捷命令
