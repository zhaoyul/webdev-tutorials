^{:nextjournal.clerk/visibility {:code :hide}}
(ns portal.inspect
  "Portal 常用视图与过滤示例."
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as v]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# 常用视图\n\n本页展示 Portal 内置 viewer 的常见用法.\n如果 Portal 尚未打开, 请先执行 `ensure-portal!`.")

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-portal!
  "保证 Portal 已打开, 如果未打开则创建一个新窗口."
  []
  (when (empty? (p/sessions))
    (p/open))
  (add-tap #'p/submit))

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 表格视图\n\n`portal.viewer/table` 可以把向量集合渲染成表格, 适合列表数据.")

^{::clerk/visibility {:code :show :result :show}}
(def users
  [{:user/id 1 :user/name "张三" :user/age 28}
   {:user/id 2 :user/name "李四" :user/age 32}
   {:user/id 3 :user/name "王五" :user/age 25}])

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/table users))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 树形视图\n\n`portal.viewer/tree` 适合层级数据, 例如菜单或组织结构.")

^{::clerk/visibility {:code :show :result :show}}
(def menu
  {:label "首页"
   :children [{:label "文档" :children [{:label "快速开始"} {:label "API"}]}
              {:label "案例" :children [{:label "入门"} {:label "进阶"}]}]})

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/tree menu))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## Diff 视图\n\n`portal.viewer/diff` 可以对比多个值, 适合查看变更前后差异.")

^{::clerk/visibility {:code :show :result :show}}
(def before-state {:status "draft" :amount 120 :tags #{"a" "b"}})

^{::clerk/visibility {:code :show :result :show}}
(def after-state {:status "paid" :amount 120 :tags #{"a" "c"} :paid-at "2025-01-08"})

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/diff [before-state after-state]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## Markdown 视图\n\n`portal.viewer/markdown` 可以把字符串渲染成富文本.")

^{::clerk/visibility {:code :show :result :show}}
(p/submit (v/markdown "### Portal 提示\n- 通过命令面板可以快速切换视图\n- 使用过滤器可以聚焦关键字段"))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 小结\n\n- `portal.viewer/table` 适合列表数据.\n- `portal.viewer/tree` 适合层级数据.\n- `portal.viewer/diff` 适合对比快照.\n- `portal.viewer/markdown` 适合输出说明文本.")
