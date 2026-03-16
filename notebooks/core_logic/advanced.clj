^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core_logic.advanced
  "core.logic 高级补充: 对应 wiki 中有限域约束与更复杂建模的内容."
  (:require [clojure.core.logic :refer [== fresh run run*]]
            [clojure.core.logic.fd :as fd]
            [nextjournal.clerk :as clerk]))

;; # Advanced: 有限域约束与更复杂的求解问题
;;
;; wiki 的高级内容通常会逐步进入:
;; - finite domain constraints.
;; - 更大的搜索空间控制.
;; - 把问题建模成离散变量 + 约束系统.
;;
;; 这也是 core.logic 开始和普通集合过滤明显拉开差距的地方.

;; ## 1. 用有限域约束定义搜索空间

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [a b c]
    (fd/in a b c (fd/interval 1 4))
    (fd/distinct [a b c])
    (fd/< a b)
    (fd/< b c)
    (== q [a b c])))

;; 上面这段代码不是先生成所有组合再过滤,
;; 而是一开始就把变量限制在一个更小的可行空间里.

;; ## 2. 一个更像真实问题的排班示例
;;
;; 三个任务分别用 1, 2, 3 表示执行顺序.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [api db qa]
    (fd/in api db qa (fd/interval 1 3))
    (fd/distinct [api db qa])
    (fd/< api qa)
    (fd/!= db 1)
    (== q {:api api
           :db db
           :qa qa})))

;; ## 3. Advanced 阶段要关注什么
;;
;; 高级内容不只是"会更多 API", 而是会开始认真思考:
;; - 哪些约束应该尽早声明.
;; - 哪些问题适合用 finite domain.
;; - 什么时候需要把宿主语言计算和 logic relation 分层.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["高级关注点" "实践建议"]
  :rows [["先收缩 domain" "越早声明变量范围, 搜索越容易控制"]
         ["优先 all-different / ordering 约束" "这些约束通常最能快速剪枝"]
         ["把业务规则翻译成离散变量" "排班, 谜题, 分配问题都适合先做建模"]
         ["保持 notebook 可解释" "先把小例子讲清楚, 再逐步扩大问题规模"]]})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 继续深入的方向

如果后续继续吸收 wiki 的更深层内容, 可以在这个系列上继续扩展:

- 用 `fd` 做 N 皇后, 数独或 SEND+MORE=MONEY.
- 用 `project` 把少量宿主语言计算嵌入 relation.
- 用 feature / map 约束表达结构化数据规则.
- 比较普通 relation 与 finite domain 版本在搜索上的差异.

这几个主题更适合作为单独 notebook, 以免把当前入门路径拉得过长.
")
