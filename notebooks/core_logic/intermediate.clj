^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core_logic.intermediate
  "core.logic 中级补充: 对应 wiki 中 relation 组合与 pattern matching 的内容."
  (:require [clojure.core.logic :refer [== defne fresh matche run run*]]
            [nextjournal.clerk :as clerk]))

;; # Intermediate: relation 组合与 pattern matching
;;
;; 到了中级阶段, 重点会从"会不会用 `run*`"转向:
;; - 如何把 relation 写成可复用的小部件.
;; - 如何用 `defne` 写递归 relation.
;; - 如何用 `matche` 直接在 relation 中做结构匹配.

;; ## 1. `defne`: 像多分支谓词那样定义 relation
;;
;; 下面是一个教学版 `appendo`.

^{::clerk/visibility {:code :show :result :hide}}
(defne appendo-demo
  "关系版 append."
  [xs ys out]
  ([() ys ys])
  ([[head . tail] ys [head . rest]]
   (appendo-demo tail ys rest)))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo-demo [:core] [:logic] q))

;; 同一个 relation 可以反向运行:
;; 已知总列表和尾巴, 反推出前缀.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo-demo q [:logic] [:core :logic]))

;; ## 2. `matche`: 直接对结构做关系匹配
;;
;; 这里把列表拆成 head / second / tail.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [head second tail]
    (matche [[:clojure :logic :clerk]]
      ([[head second . tail]]
       (== q {:head head
              :second second
              :tail tail})))))

;; ## 3. 递归 relation 的可读性来自模式而不是流程
;;
;; 下面这个 relation 用 pattern matching 判断一个列表是否以某个元素开头.

^{::clerk/visibility {:code :show :result :hide}}
(defne starts-witho
  "判断列表是否以某个元素开头."
  [x xs]
  ([x [x . _]]))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (starts-witho :clojure [:clojure :logic :clerk])
  (== q :matched))

;; 也可以把未知量留给求解器.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (starts-witho q [:clojure :logic :clerk]))

;; ## 4. Intermediate 阶段最重要的迁移

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["从 Beginner 到 Intermediate" "含义"]
  :rows [["从 API 记忆转向 relation 设计" "开始考虑可复用性和递归形状"]
         ["从普通 destructuring 转向 `matche` / `defne`" "把结构也交给关系系统处理"]
         ["从示例查询转向 relation 库" "后续业务规则通常是多个 relation 组合出来的"]]})

;; ## 接下来
;;
;; - [advanced.clj](advanced.clj): 进入有限域约束与更大的建模问题
;; - [techniques.clj](techniques.clj): 回看 relation 拆分与搜索空间控制
