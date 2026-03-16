^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core-logic.intro
  "core.logic 入门: 统一, 关系与双向查询."
  (:require [clojure.core.logic :refer [== appendo conde fresh membero run run*]]
            [core-logic.common :as c]
            [nextjournal.clerk :as clerk]))

;; # core.logic 入门
;;
;; core.logic 把"求值"换成了"求满足条件的答案".
;; 你不再一步步告诉程序怎么做, 而是声明变量之间必须满足什么关系.

^{::clerk/visibility {:code :hide :result :show}}
(c/fact-table
 ["概念" "说明"]
 [["逻辑变量" "在搜索过程中逐步绑定的未知量"]
  ["统一(==)" "让两个结构相等, 并传播约束"]
  ["fresh" "引入局部逻辑变量"]
  ["conde" "声明多个可选分支"]
  ["relation" "可以正向问, 也可以反向问的约束"]])

;; ## 1. 最小示例: 统一

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (== q :hello-core-logic))

;; 统一不仅能处理简单值, 也能处理结构.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (== q {:topic :core.logic
         :style :declarative
         :benefits [:search :constraints :relations]}))

;; ## 2. conde: 声明多个可能的答案

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (conde
    [(== q :web)]
    [(== q :data)]
    [(== q :ai)]))

;; ## 3. fresh: 引入中间变量

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [lang tool]
    (== lang :clojure)
    (== tool :clerk)
    (== q [lang tool])))

;; ## 4. membero: 用事实表回答问题

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q [:ring :reitit :malli :core-logic]))

;; 可以同时叠加多个条件.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q [:ring :reitit :malli :core-logic])
  (membero q [:malli :core-logic :datomic]))

;; ## 5. 关系天然支持反向查询
;;
;; `appendo` 表示"前缀 + 后缀 = 结果".
;; 同一个 relation 既能用来拼接, 也能反推出前缀或后缀.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo [:ring :reitit] [:muuntaja] q))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo q [:muuntaja] [:ring :reitit :muuntaja]))

;; `run` 常用于限制搜索规模, 避免一次返回过多结果.
^{::clerk/visibility {:code :show :result :show}}
(run 3 [q]
  (fresh [suffix]
    (appendo q suffix [:clojure :clerk :core.logic])))

;; ## 6. 什么时候适合用 core.logic

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["场景" "为什么适合"]
  :rows [["规则推导" "权限继承, 资格判断, 路由选择都能写成 relation"]
         ["搜索问题" "把目标写成约束, 由系统枚举候选答案"]
         ["可逆变换" "同一个 relation 可正向运行, 也可反向求解"]
         ["原型验证" "先描述规则, 再逐步补充剪枝与优化"]]})

;; ## 下一步
;;
;; - [family_relations.clj](family_relations.clj): 用亲属关系建立递归 relation 的直觉
;; - [applications.clj](applications.clj): 看权限推导, 人员匹配与有限域约束
;; - [techniques.clj](techniques.clj): 总结写 relation 时的常用技巧
