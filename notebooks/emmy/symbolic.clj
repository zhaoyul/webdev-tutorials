^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.symbolic
  "Emmy 符号计算: 表达式操作与简化."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]))

;; # 符号计算
;;
;; Emmy 的核心能力之一是符号计算 - 将数学符号作为一等公民来处理,
;; 对其进行代数运算, 自动生成和简化表达式.

;; ## 符号表达式基础

;; ### 创建符号

;; 使用 quote 创建符号:
^{::clerk/visibility {:code :show :result :show}}
(def x 'x)
(def y 'y)
(def z 'z)

;; 符号被视为抽象复数, 可以参与所有数学运算:
^{::clerk/visibility {:code :show :result :show}}
(+ x y z)

^{::clerk/visibility {:code :show :result :show}}
(* x (+ y 1))

;; ### 代数表达式

;; 多项式:
^{::clerk/visibility {:code :show :result :show}}
(def polynomial
  (+ (expt 'x 3) (* 2 (expt 'x 2)) (* -1 'x) 1))
polynomial

;; 使用 `->infix` 显示为人类可读格式:
^{::clerk/visibility {:code :show :result :show}}
(->infix polynomial)

;; 使用 `->TeX` 生成 TeX 格式:
^{::clerk/visibility {:code :show :result :show}}
(->TeX polynomial)

;; ## 表达式简化

;; ### simplify 函数

;; `simplify` 是 Emmy 中最常用的简化函数:

;; 三角恒等式自动简化:
^{::clerk/visibility {:code :show :result :show}}
(simplify (+ (expt (sin 'x) 2) (expt (cos 'x) 2)))

;; 代数简化:
^{::clerk/visibility {:code :show :result :show}}
(simplify (* (+ 'x 1) (- 'x 1)))

;; 展开后简化:
^{::clerk/visibility {:code :show :result :show}}
(simplify (expt (+ 'a 'b) 2))

;; ### 渲染辅助函数

;; 组合简化和渲染:
^{::clerk/visibility {:code :show :result :show}}
(defn render [expr]
  (-> expr simplify ->infix))

(defn render-tex [expr]
  (-> expr simplify ->TeX))

;; 示例:
^{::clerk/visibility {:code :show :result :show}}
(render (expt (+ 'a 'b) 2))

^{::clerk/visibility {:code :show :result :show}}
(render-tex (expt (+ 'a 'b) 2))

;; ## 三角函数

;; ### 基本三角函数
^{::clerk/visibility {:code :show :result :show}}
(sin 'theta)

^{::clerk/visibility {:code :show :result :show}}
(cos 'theta)

^{::clerk/visibility {:code :show :result :show}}
(tan 'theta)

;; ### 三角恒等式
^{::clerk/visibility {:code :show :result :show}}
(render (+ (square (sin 'x)) (square (cos 'x))))

;; 二倍角公式:
^{::clerk/visibility {:code :show :result :show}}
(render (sin (* 2 'x)))

^{::clerk/visibility {:code :show :result :show}}
(render (cos (* 2 'x)))

;; ### 反三角函数
^{::clerk/visibility {:code :show :result :show}}
(asin 'x)

^{::clerk/visibility {:code :show :result :show}}
(atan 'y 'x)

;; ## 指数与对数

;; ### 指数函数
^{::clerk/visibility {:code :show :result :show}}
(exp 'x)

^{::clerk/visibility {:code :show :result :show}}
(expt 'a 'b)

;; ### 对数函数
^{::clerk/visibility {:code :show :result :show}}
(log 'x)

;; 对数与指数的关系:
^{::clerk/visibility {:code :show :result :show}}
(simplify (log (exp 'x)))

;; ## 复杂表达式示例

;; ### 物理公式示例

;; 动能公式:
^{::clerk/visibility {:code :show :result :show}}
(def kinetic-energy
  (* (/ 1 2) 'm (square 'v)))
(render kinetic-energy)

;; 重力势能:
^{::clerk/visibility {:code :show :result :show}}
(def potential-energy
  (* 'm 'g 'h))
(render potential-energy)

;; 机械能:
^{::clerk/visibility {:code :show :result :show}}
(def mechanical-energy
  (+ kinetic-energy potential-energy))
(render mechanical-energy)

;; ### 欧拉公式
^{::clerk/visibility {:code :show :result :show}}
(def euler-formula
  (exp (* 'i 'theta)))
euler-formula

;; ## 符号函数

;; ### literal-function

;; 创建抽象函数 (未知函数):
^{::clerk/visibility {:code :show :result :show}}
(def f (literal-function 'f))

;; 应用抽象函数:
^{::clerk/visibility {:code :show :result :show}}
(f 'x)

;; 对抽象函数求导:
^{::clerk/visibility {:code :show :result :show}}
((D f) 'x)

;; 高阶导数:
^{::clerk/visibility {:code :show :result :show}}
(((expt D 2) f) 'x)

;; ### 多元函数
^{::clerk/visibility {:code :show :result :show}}
(def g (literal-function 'g '(-> (X Real Real) Real)))
(g 'x 'y)

;; ## 符号计算技巧

;; ### 表达式比较

;; 符号表达式可以进行相等性比较:
^{::clerk/visibility {:code :show :result :show}}
(= (simplify (+ (square (sin 'x)) (square (cos 'x))))
   1)

;; ### 表达式代入

;; 注意: Emmy 目前不直接支持表达式代入, 但可以通过函数组合实现:
^{::clerk/visibility {:code :show :result :show}}
(let [expr (fn [x] (+ (square x) (* 2 x) 1))]
  (simplify (expr 'a)))

;; ## 下一步

;; 继续阅读:
;; - [calculus.clj](calculus.clj) - 微积分与自动微分
;; - [mechanics.clj](mechanics.clj) - 拉格朗日与哈密顿力学
