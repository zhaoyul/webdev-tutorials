^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.intro
  "Emmy 入门: 核心概念与基本用法."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]))

;; # Emmy 介绍
;;
;; Emmy 是一个 Clojure/ClojureScript 实现的计算机代数系统, 灵感来自于
;; MIT 的 scmutils 系统. 它专为数学和物理研究设计, 支持符号计算、
;; 自动微分、数值积分等功能.

;; ## 核心概念

;; ### 1. 泛型数学操作 (Generic Mathematics)
;;
;; Emmy 实现了一套泛型数学操作系统, 可以统一处理:
;; - 数值 (整数, 有理数, 浮点数, 复数)
;; - 符号表达式
;; - 函数
;; - 矩阵和向量
;; - 微分形式

;; ### 2. 符号计算 (Symbolic Computation)
;;
;; Emmy 可以将符号作为抽象复数来处理, 对其进行代数运算会生成符号表达式.

;; ### 3. 自动微分 (Automatic Differentiation)
;;
;; Emmy 提供前向模式自动微分, 可以对数值和符号表达式求导.

;; ### 4. TeX 渲染
;;
;; Emmy 可以将符号表达式渲染为 TeX 和中缀表示法, 便于文档和可视化.

;; ## 快速开始

;; ### 基本算术运算

;; Emmy 的数值运算与 Clojure 类似, 但支持完整的数值塔:

^{::clerk/visibility {:code :show :result :show}}
(- (* 7 (/ 1 2)) 2)
;; => 3/2 (有理数)

;; 复数运算:
^{::clerk/visibility {:code :show :result :show}}
(asin -10)
;; => 复数结果

;; ### 符号计算基础

;; 使用 quote (') 创建符号:
^{::clerk/visibility {:code :show :result :show}}
(def x 'x)
(def y 'y)

;; 对符号进行算术运算会生成符号表达式:
^{::clerk/visibility {:code :show :result :show}}
(+ x y)

^{::clerk/visibility {:code :show :result :show}}
(* x x)

^{::clerk/visibility {:code :show :result :show}}
(square x)

;; ### 表达式渲染

;; 使用 `->infix` 将表达式转换为中缀表示法:
^{::clerk/visibility {:code :show :result :show}}
(->infix (square (sin (+ 'a 3))))

;; 使用 `simplify` 简化表达式:
^{::clerk/visibility {:code :show :result :show}}
(simplify (+ (square (sin 'x)) (square (cos 'x))))

;; 组合使用:
^{::clerk/visibility {:code :show :result :show}}
(defn render [expr]
  (-> expr simplify ->infix))

(render (+ (square (sin 'x)) (square (cos 'x))))

;; ### 基本微分

;; `D` 是微分算子, 用于求导:
^{::clerk/visibility {:code :show :result :show}}
((D cube) 'x)

;; 简化后的结果:
^{::clerk/visibility {:code :show :result :show}}
(render ((D cube) 'x))

;; ## 数据类型概览

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["类型" "描述" "示例"]
  :rows [["符号 (Symbol)" "抽象数学变量" "'x, 'theta"]
         ["有理数 (Rational)" "精确分数" "1/2, 3/4"]
         ["复数 (Complex)" "实部+虚部" "(complex 1 2)"]
         ["向量 (Up/Down)" "协变/逆变向量" "(up 1 2 3)"]
         ["矩阵 (Matrix)" "二维数组" "(matrix [[1 2] [3 4]])"]
         ["函数 (Function)" "可微分函数" "(fn [x] (* x x))"]
         ["算子 (Operator)" "作用于函数" "D, (expt D 2)"]]})

;; ## 向量与矩阵

;; Emmy 使用 `up` 和 `down` 来表示向量:

^{::clerk/visibility {:code :show :result :show}}
(def v (up 1 2 3))
v

^{::clerk/visibility {:code :show :result :show}}
(def w (down 4 5 6))
w

;; 向量运算:
^{::clerk/visibility {:code :show :result :show}}
(* v w)  ; 内积

;; 矩阵创建:
^{::clerk/visibility {:code :show :result :show}}
(def M (matrix [[1 2] [3 4]]))
M

;; 矩阵与向量相乘:
^{::clerk/visibility {:code :show :result :show}}
(* M (up 1 0))

;; ## 下一步

;; 继续阅读:
;; - [symbolic.clj](symbolic.clj) - 深入符号计算
;; - [calculus.clj](calculus.clj) - 微积分与自动微分
;; - [mechanics.clj](mechanics.clj) - 拉格朗日与哈密顿力学
