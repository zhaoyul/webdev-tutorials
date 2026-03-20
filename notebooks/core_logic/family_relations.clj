^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core-logic.family-relations
  "core.logic 关系建模: 用亲属关系展示递归推理."
  (:require [clojure.core.logic :refer [!= == conde fresh membero run run*]]
            [core-logic.common :as c]
            [nextjournal.clerk :as clerk]))

;; # 用关系表达亲属系统
;;
;; 亲属关系是学习 logic programming 的经典入口:
;; 它同时包含事实, 组合规则, 递归规则和反向查询.

^{::clerk/visibility {:code :hide :result :show}}
(c/fact-table
 ["父母" "孩子"]
 c/family-parent-pairs)

^{::clerk/visibility {:code :show :result :hide}}
(defn parento
  "父母关系."
  [parent child]
  (membero [parent child] c/family-parent-pairs))

^{::clerk/visibility {:code :show :result :hide}}
(defn maleo
  "男性成员."
  [person]
  (membero person c/male-members))

^{::clerk/visibility {:code :show :result :hide}}
(defn femaleo
  "女性成员."
  [person]
  (membero person c/female-members))

^{::clerk/visibility {:code :show :result :hide}}
(defn siblingo
  "兄弟姐妹关系."
  [x y]
  (fresh [parent]
    (parento parent x)
    (parento parent y)
    (!= x y)))

^{::clerk/visibility {:code :show :result :hide}}
(defn grandparento
  "祖辈关系."
  [grandparent child]
  (fresh [parent]
    (parento grandparent parent)
    (parento parent child)))

^{::clerk/visibility {:code :show :result :hide}}
(defn ancestoro
  "递归祖先关系."
  [ancestor descendant]
  (conde
    [(parento ancestor descendant)]
    [(fresh [middle]
       (parento ancestor middle)
       (ancestoro middle descendant))]))

;; ## 1. 基础查询

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (parento :母亲 q))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (grandparento q :小明))

;; ## 2. 组合约束
;;
;; 逻辑编程的重点不在"写 if", 而在"叠加约束".

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (femaleo q)
  (grandparento q :小明))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (maleo q)
  (parento q :小美))

;; ## 3. 同一个 relation 的正反两面
;;
;; 因为 `:小明` 和 `:小美` 同时共享父亲、母亲两条事实边,
;; 所以下面的查询会得到重复解. 这里保留原始 relation, 只在展示层去重,
;; 方便读者看到"事实建模"与"结果整理"是两个不同层次的问题.

;; 展示层去重:
^{::clerk/visibility {:code :show :result :show}}
(->> (run* [q]
       (siblingo :小明 q))
     distinct
     vec)

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (ancestoro q :豆豆))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (ancestoro :祖母 q))

;; ## 4. 递归关系的价值
;;
;; 一旦把事实和基本 relation 拆开, 更复杂的推理就只是在上面继续组合.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["relation" "依赖" "作用"]
  :rows [["parento" "事实表" "最基础的边"]
         ["siblingo" "parento" "共享父母但不是同一人"]
         ["grandparento" "parento + parento" "两跳推理"]
         ["ancestoro" "parento + 递归" "任意层级的祖先查询"]]})

;; ## 5. 对教程式代码的启发
;;
;; 这套写法和 Datomic Datalog 很像, 但更进一步:
;; 不只是查询现成数据, 还可以把未知量留给系统自己求.
