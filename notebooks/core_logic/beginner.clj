^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core_logic.beginner
  "core.logic 入门补充: 对应 wiki Primer 的基础概念与操作."
  (:require [clojure.core.logic :refer [== conde conso fresh membero resto run run*]]
            [nextjournal.clerk :as clerk]))

;; # Beginner: 从 Primer 补足最基础直觉
;;
;; 这一页对应 core.logic wiki 里偏 Beginner 的内容:
;; - 什么是 logic variable.
;; - 什么是 goal / constraint.
;; - 为什么 `run*` 返回的是"所有满足约束的答案".
;; - 如何理解 `conso` / `resto` 这类可逆 relation.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["概念" "可以这样理解"]
  :rows [["logic variable" "还没确定值, 但会被约束逐步缩小范围的变量"]
         ["goal" "一个会成功或失败的约束"]
         ["run*" "枚举所有满足约束的答案"]
         ["relation" "不仅能正向求值, 也能反向追问未知量"]]})

;; ## 1. `run*` + `==`: 从唯一答案开始

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (== q :core-logic))

;; 多个 goal 默认是 AND.
;; 只有同时满足所有约束的答案才会保留下来.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q [:clojure :core-logic :datomic])
  (membero q [:core-logic :malli :clerk]))

;; ## 2. `conde`: 把多个可能分支写成声明式约束

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (conde
    [(== q :beginner)]
    [(== q :intermediate)]
    [(== q :advanced)]))

;; `run` 可以限制结果数量, 适合先验证 relation 是否写对.
^{::clerk/visibility {:code :show :result :show}}
(run 2 [q]
  (conde
    [(== q :beginner)]
    [(== q :intermediate)]
    [(== q :advanced)]))

;; ## 3. `fresh`: 显式引入局部逻辑变量
;;
;; 这和 wiki Primer 中强调的思路一致:
;; 写 relation 时, 先把中间未知量命名出来, 再叠加约束.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [lang topic]
    (== lang :clojure)
    (== topic :logic-programming)
    (== q [lang topic])))

;; ## 4. `conso` / `resto`: 用 relation 理解列表
;;
;; `(conso a d l)` 的意思是:
;; `l` 的头部是 `a`, 剩余部分是 `d`.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [head tail]
    (conso head tail [:clojure :logic :clerk])
    (== q {:head head
           :tail tail})))

;; 反过来也能问:
;; 已知头元素和完整列表, 剩余部分是什么.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (conso :clojure q [:clojure :logic :clerk]))

;; `resto` 是更直接的"取尾部" relation.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (resto [:clojure :logic :clerk] q))

;; ## 5. Beginner 阶段的阅读重点

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["重点" "为什么重要"]
  :rows [["先看答案如何被筛出来" "建立约束求解而不是逐步执行的直觉"]
         ["接受 relation 的双向性" "同一段代码可以正问, 也可以反问"]
         ["把列表也看成约束对象" "后面写递归 relation 会更自然"]
         ["先用小样本验证" "逻辑代码正确但搜索过大时也会难读"]]})

;; ## 接下来
;;
;; - [intermediate.clj](intermediate.clj): 进入 pattern matching 与 `defne`
;; - [family_relations.clj](family_relations.clj): 用亲属关系练习递归 relation
