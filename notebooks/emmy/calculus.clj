^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.calculus
  "Emmy 微积分: 自动微分与数值方法."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]))

;; # 微积分
;;
;; Emmy 提供强大的微积分功能, 包括:
;; - 前向模式自动微分
;; - 符号微分
;; - 数值积分
;; - 优化算法

;; ## 微分算子 D

;; ### 基本用法

;; `D` 是 Emmy 中的微分算子, 返回函数的导数:
^{::clerk/visibility {:code :show :result :show}}
((D square) 'x)

;; 简化结果:
^{::clerk/visibility {:code :show :result :show}}
(defn render [expr]
  (-> expr simplify ->infix))

(render ((D square) 'x))

;; ### 常见函数的导数

;; 幂函数:
^{::clerk/visibility {:code :show :result :show}}
((D cube) 'x)

^{::clerk/visibility {:code :show :result :show}}
(render ((D cube) 'x))

;; 三角函数:
^{::clerk/visibility {:code :show :result :show}}
(render ((D sin) 'x))

^{::clerk/visibility {:code :show :result :show}}
(render ((D cos) 'x))

;; 指数函数:
^{::clerk/visibility {:code :show :result :show}}
(render ((D exp) 'x))

;; 对数函数:
^{::clerk/visibility {:code :show :result :show}}
(render ((D log) 'x))

;; ## 高阶导数

;; 使用 `(expt D n)` 计算 n 阶导数:

;; 二阶导数:
^{::clerk/visibility {:code :show :result :show}}
(((expt D 2) cube) 'x)

^{::clerk/visibility {:code :show :result :show}}
(render (((expt D 2) cube) 'x))

;; 三阶导数:
^{::clerk/visibility {:code :show :result :show}}
(render (((expt D 3) cube) 'x))

;; sin 的各阶导数:
^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["阶数" "导数" "简化结果"]
  :rows (for [n (range 5)]
          [n
           (((expt D n) sin) 'x)
           (render (((expt D n) sin) 'x))])})

;; ## 复合函数求导

;; ### 链式法则

;; Emmy 自动应用链式法则:
^{::clerk/visibility {:code :show :result :show}}
(def f (fn [x] (sin (square x))))

((D f) 'x)

^{::clerk/visibility {:code :show :result :show}}
(render ((D f) 'x))

;; 更复杂的例子:
^{::clerk/visibility {:code :show :result :show}}
(def g (fn [x] (exp (sin x))))

(render ((D g) 'x))

;; ### 乘法法则
^{::clerk/visibility {:code :show :result :show}}
(def h (fn [x] (* x (sin x))))

(render ((D h) 'x))

;; ### 除法法则
^{::clerk/visibility {:code :show :result :show}}
(def q (fn [x] (/ (sin x) x)))

(render ((D q) 'x))

;; ## 偏导数

;; ### 多元函数

;; 定义二元函数:
^{::clerk/visibility {:code :show :result :show}}
(defn f2 [[x y]]
  (+ (square x) (* x y) (square y)))

;; 对向量参数求导会得到梯度:
^{::clerk/visibility {:code :show :result :show}}
((D f2) (up 'x 'y))

;; ### partial 偏导数
^{::clerk/visibility {:code :show :result :show}}
(((partial 0) f2) (up 'x 'y))

^{::clerk/visibility {:code :show :result :show}}
(((partial 1) f2) (up 'x 'y))

;; ## 抽象函数的导数

;; ### literal-function

;; 创建抽象函数并求导:
^{::clerk/visibility {:code :show :result :show}}
(def F (literal-function 'F))

((D F) 'x)

;; 高阶导数:
^{::clerk/visibility {:code :show :result :show}}
(((expt D 2) F) 'x)

;; ### 多元抽象函数
^{::clerk/visibility {:code :show :result :show}}
(def G (literal-function 'G '(-> (X Real Real) Real)))

((D G) (up 'x 'y))

;; ## 数值微分

;; ### 数值求导

;; Emmy 也支持数值计算:
^{::clerk/visibility {:code :show :result :show}}
((D sin) 0.0)

^{::clerk/visibility {:code :show :result :show}}
((D sin) (/ pi 2))

^{::clerk/visibility {:code :show :result :show}}
((D exp) 1.0)

;; ### 自动微分 vs 数值微分

;; Emmy 使用前向模式自动微分, 比数值微分更精确:
^{::clerk/visibility {:code :show :result :show}}
(def exact-derivative ((D exp) 1.0))
exact-derivative

;; 数值微分会有舍入误差, 而自动微分给出精确结果.

;; ## 积分 (数值方法)

;; ### 定积分

;; 使用 `definite-integral` 计算定积分:
^{::clerk/visibility {:code :show :result :show}}
(definite-integral sin 0 pi)

^{::clerk/visibility {:code :show :result :show}}
(definite-integral (fn [x] (* x x)) 0 1)
;; => 1/3

;; ### 积分选项
^{::clerk/visibility {:code :show :result :show}}
(definite-integral exp 0 1 {:tolerance 1e-10})

;; ## 微分方程

;; ### 状态演化

;; Emmy 提供工具来模拟动力系统的演化:
^{::clerk/visibility {:code :show :result :show}}
(defn harmonic-oscillator-state-derivative [_]
  (fn [[_ x v]]
    (up 1 v (- x))))

;; 这定义了简谐振子的状态导数: dx/dt = v, dv/dt = -x

;; ## 泰勒级数

;; ### series 展开
^{::clerk/visibility {:code :show :result :show}}
(take 8 (seq (((exp D) identity) 0)))

;; sin 在 x=0 处的泰勒系数:
^{::clerk/visibility {:code :show :result :show}}
(take 8 (seq (((exp D) sin) 0)))

;; ## 实用示例

;; ### 物理: 自由落体

;; 位置函数:
^{::clerk/visibility {:code :show :result :show}}
(defn position [t]
  (- (* 'v0 t) (* (/ 1 2) 'g (square t))))

;; 速度 (位置对时间的导数):
^{::clerk/visibility {:code :show :result :show}}
(render ((D position) 't))

;; 加速度 (速度对时间的导数):
^{::clerk/visibility {:code :show :result :show}}
(render (((expt D 2) position) 't))

;; ### 优化: 极值点

;; 寻找极值点需要一阶导数为零:
^{::clerk/visibility {:code :show :result :show}}
(defn cubic [x]
  (+ (cube x) (* -3 (square x)) (* -9 x) 5))

;; 一阶导数:
^{::clerk/visibility {:code :show :result :show}}
(render ((D cubic) 'x))

;; 二阶导数 (判断极值类型):
^{::clerk/visibility {:code :show :result :show}}
(render (((expt D 2) cubic) 'x))

;; ## 下一步

;; 继续阅读:
;; - [mechanics.clj](mechanics.clj) - 拉格朗日与哈密顿力学
