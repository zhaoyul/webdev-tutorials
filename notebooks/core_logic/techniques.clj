^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core-logic.techniques
  "core.logic 使用技巧: 控制搜索空间并保持 relation 可组合."
  (:require [clojure.core.logic :refer [== appendo conde fresh membero run run*]]
            [core-logic.common :as c]
            [nextjournal.clerk :as clerk]))

;; # core.logic 使用技巧
;;
;; 真正把 core.logic 用顺手, 关键不在记 API, 而在 relation 的拆分方式和搜索空间的控制.

^{::clerk/visibility {:code :show :result :hide}}
(defn has-skillo
  "成员拥有某项技能."
  [member skill]
  (membero [member skill] c/member-skills))

^{::clerk/visibility {:code :show :result :hide}}
(defn full-stacko
  "既会 Clojure 又会 UI 的成员."
  [member]
  (fresh [m]
    (== m member)
    (has-skillo m :clojure)
    (has-skillo m :ui)))

^{::clerk/visibility {:code :show :result :hide}}
(defn available-full-stacko
  "指定日期可排班的全栈成员."
  [member day]
  (fresh [m d]
    (== m member)
    (== d day)
    (full-stacko m)
    (membero [m d] c/member-shifts)))

;; ## 1. 先写小 relation, 再做组合

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (full-stacko q))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (available-full-stacko q :thu))

;; ## 2. 尽量保持 relation 可逆
;;
;; 能双向运行的 relation 复用价值最高.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo [:reitit] [:malli :muuntaja] q))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (appendo q [:malli :muuntaja] [:reitit :malli :muuntaja]))

;; ## 3. 用 run 限制搜索规模
;;
;; 面对开放式搜索时, 先拿少量结果验证 relation 是否正确, 再逐步放宽范围.

^{::clerk/visibility {:code :show :result :show}}
(run 3 [q]
  (membero q [:ring :reitit :malli :muuntaja :core-logic :datomic]))

;; ## 4. 优先写选择性高的约束
;;
;; 下面先把成员固定在周四可用的人群里, 再问技能, 通常比盲目枚举更容易理解和优化.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [member]
    (membero [member :thu] c/member-shifts)
    (has-skillo member :clojure)
    (== q member)))

;; ## 5. 用 conde 表达业务分支, 不要急着掉回命令式代码

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [member]
    (== member :小赵)
    (conde
      [(has-skillo member :clojure) (== q :backend)]
      [(has-skillo member :ui) (== q :frontend)]
      [(has-skillo member :ops) (== q :support)])))

;; ## 6. 常见实践建议

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["建议" "原因"]
  :rows [["先事实、后规则" "先把原始事实变成 relation, 更容易验证"]
         ["先小样本、后放量" "用 run 限制结果数, 更容易调试"]
         ["避免一次塞太多宿主语言逻辑" "保持 relation 纯净, 可逆性更强"]
         ["递归 relation 先加基础分支" "先保证终止条件清晰, 再扩展复杂规则"]
         ["把高选择性条件放前面" "能更快收缩搜索空间"]]})

;; ## 下一步
;;
;; 如果你后续要写更复杂的求解器, 可以继续尝试:
;; - 用 `fd` 扩展离散约束
;; - 把业务规则拆成可复用 relation 库
;; - 先在 notebook 里验证 relation, 再迁移到服务代码
