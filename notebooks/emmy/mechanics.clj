^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.mechanics
  "Emmy 力学: 拉格朗日与哈密顿力学."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e
             :refer :all
             :rename {Lagrangian->energy env-lagrangian->energy
                     momentum env-momentum
                     Lagrangian->Hamiltonian env-lagrangian->hamiltonian}]))

;; # 经典力学
;;
;; Emmy 继承自 scmutils, 专为《经典力学的结构与解释》(SICM) 设计.
;; 本节展示如何使用 Emmy 研究拉格朗日力学和哈密顿力学.

;; ## 拉格朗日力学基础

;; ### 状态空间

;; 在拉格朗日力学中, 系统的状态由 **广义坐标** 和 **广义速度** 描述.
;; Emmy 使用 `up` 向量来表示状态: (t, q, qdot)

^{::clerk/visibility {:code :show :result :show}}
(def state (up 't (up 'x 'y) (up 'vx 'vy)))
state

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
### up 与 down 对照

up 表示上标向量, down 表示下标协向量. 在 Emmy 中显式区分它们,
可以避免把梯度和速度混用。

| 结构 | 含义 | 典型对象 | 例子 |
| --- | --- | --- | --- |
| up | 向量, 上标分量 | 位置, 速度, 状态 | `(up t q qdot)` |
| down | 协向量, 下标分量 | 梯度, 动量, 一形式 | `(down p0 p1)` |
| up ↔ down | 度量升降指标 | 从速度到动量 | `(down (* m v))` |
")

;; ### 拉格朗日量

;; 拉格朗日量 L = T - V (动能减势能)

;; 自由粒子的拉格朗日量:
^{::clerk/visibility {:code :show :result :show}}
(defn L-free-particle [mass]
  (fn [[_ _ v]]
    (* (/ 1 2) mass (square v))))

;; 应用到符号状态:
^{::clerk/visibility {:code :show :result :show}}
(def L-free ((L-free-particle 'm) (up 't 'x 'v)))
L-free

;; ### 带势能的粒子
^{::clerk/visibility {:code :show :result :show}}
(defn L-particle-in-potential [mass potential]
  (fn [[_ q v]]
    (- (* (/ 1 2) mass (square v))
       (potential q))))

;; ## 欧拉-拉格朗日方程

;; ### 渲染辅助函数
^{::clerk/visibility {:code :show :result :show}}
(defn render [expr]
  (-> expr simplify ->infix))

;; ### Lagrange-equations

;; Emmy 提供 `Lagrange-equations` 来生成运动方程:
^{::clerk/visibility {:code :show :result :show}}
(def eom
  (Lagrange-equations (L-free-particle 'm)))

;; 应用到坐标路径:
^{::clerk/visibility {:code :show :result :show}}
(def q (literal-function 'q))

(render ((eom q) 't))

;; 这给出自由粒子的运动方程: m * d²q/dt² = 0

;; ## 具体力学系统

;; ### 1. 一维简谐振子
;;
;; V(x) = (1/2) k x²

^{::clerk/visibility {:code :show :result :show}}
(defn L-harmonic [m k]
  (fn [[_ x v]]
    (- (* (/ 1 2) m (square v))
       (* (/ 1 2) k (square x)))))

^{::clerk/visibility {:code :show :result :show}}
(def harmonic-eom
  (Lagrange-equations (L-harmonic 'm 'k)))

^{::clerk/visibility {:code :show :result :show}}
(def x (literal-function 'x))

(render ((harmonic-eom x) 't))

;; 这给出简谐振子方程: m * d²x/dt² + k*x = 0

;; ### 2. 中心力场
;;
;; 极坐标下的中心力场问题

^{::clerk/visibility {:code :show :result :show}}
(defn L-central-polar [m U]
  (fn [[_ [r] [rdot thetadot]]]
    (- (* (/ 1 2) m
          (+ (square rdot)
             (square (* r thetadot))))
       (U r))))

;; 定义抽象势能函数:
^{::clerk/visibility {:code :show :result :show}}
(def U (literal-function 'U))

;; 生成运动方程:
^{::clerk/visibility {:code :show :result :show}}
(def central-eom
  (Lagrange-equations (L-central-polar 'm U)))

;; 应用到坐标路径:
^{::clerk/visibility {:code :show :result :show}}
(def r (literal-function 'r))
(def theta (literal-function 'theta))

(render ((central-eom (up r theta)) 't))

;; ### 3. 单摆
;;
;; 单摆在重力场中的运动

^{::clerk/visibility {:code :show :result :show}}
(defn L-pendulum [m l g]
  (fn [[_ theta thetadot]]
    (- (* (/ 1 2) m (square (* l thetadot)))
       (* m g l (- 1 (cos theta))))))

^{::clerk/visibility {:code :show :result :show}}
(def pendulum-eom
  (Lagrange-equations (L-pendulum 'm 'l 'g)))

^{::clerk/visibility {:code :show :result :show}}
(def theta-path (literal-function 'theta))

(render ((pendulum-eom theta-path) 't))

;; ## 守恒量

;; ### 动量
^{::clerk/visibility {:code :show :result :show}}
(defn lagrangian-momentum
  "教学用动量函数, p = ∂L/∂q̇."
  [L]
  (fn [state]
    (((partial 2) L) state)))

((lagrangian-momentum (L-free-particle 'm)) (up 't 'x 'v))

;; ### 能量守恒
;;
;; 能量函数 E = p·qdot - L

^{::clerk/visibility {:code :show :result :show}}
(defn lagrangian->energy
  "教学用能量函数, 以拉格朗日量为输入."
  [L]
  (fn [state]
    (let [[_ _ qdot] state]
      (- (* ((lagrangian-momentum L) state) qdot)
         (L state)))))

;; 自由粒子的能量:
^{::clerk/visibility {:code :show :result :show}}
(def energy-free
  (lagrangian->energy (L-free-particle 'm)))

(energy-free (up 't 'x 'v))

;; ## 哈密顿力学

;; ### 勒让德变换
;;
;; 从拉格朗日量到哈密顿量

^{::clerk/visibility {:code :show :result :show}}
(defn Lagrangian->Hamiltonian [L]
  (fn [[t q p]]
    (let [state-path (fn [qdot] (up t q qdot))]
      ;; 简化版本: 假设 p = m*v, 即 v = p/m
      ;; 完整实现需要求解 p = dL/dqdot 得到 qdot
      (lagrangian->energy L))))

;; ### 哈密顿量示例

;; 自由粒子哈密顿量: H = p²/(2m)
^{::clerk/visibility {:code :show :result :show}}
(defn H-free-particle [m]
  (fn [[_ _ p]]
    (* (/ 1 2) (/ (square p) m))))

((H-free-particle 'm) (up 't 'x 'p))

;; 简谐振子哈密顿量: H = p²/(2m) + kx²/2
^{::clerk/visibility {:code :show :result :show}}
(defn H-harmonic [m k]
  (fn [[_ x p]]
    (+ (/ (square p) (* 2 m))
       (* (/ 1 2) k (square x)))))

((H-harmonic 'm 'k) (up 't 'x 'p))

;; ### 哈密顿方程
^{::clerk/visibility {:code :show :result :show}}
(def harmonic-hamilton-eom
  (Hamilton-equations (H-harmonic 'm 'k)))

^{::clerk/visibility {:code :show :result :show}}
(def x-path (literal-function 'x))
(def p-path (literal-function 'p))

((harmonic-hamilton-eom x-path p-path) 't)

;; ## 相空间

;; ### 相空间轨迹

;; 哈密顿力学中, 状态由 (q, p) 描述
;; 相空间轨迹展示系统的演化

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["力学系统" "拉格朗日量 L" "哈密顿量 H"]
  :rows [["自由粒子" "½mv²" "p²/(2m)"]
         ["简谐振子" "½mv² - ½kx²" "p²/(2m) + ½kx²"]
         ["单摆" "½ml²θ̇² - mgl(1-cosθ)" "p²/(2ml²) + mgl(1-cosθ)"]
         ["中心力" "½m(ṙ² + r²θ̇²) - U(r)" "复杂形式"]]})

;; ## 数值仿真

;; ### 状态导数

;; 定义系统演化的状态导数:
^{::clerk/visibility {:code :show :result :show}}
(defn harmonic-state-derivative [m k]
  (fn [[_ x v]]
    (up 1 v (- (/ (* k x) m)))))

;; 这定义了: dt/dt = 1, dx/dt = v, dv/dt = -kx/m

;; ### 状态前进器
;;
;; 使用 `state-advancer` 可以数值积分状态:

;; (def advance (state-advancer harmonic-state-derivative 1.0 1.0))
;; (advance (up 0 1 0) 10)  ; 从 t=0, x=1, v=0 演化 10 秒

;; ## 总结

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["概念" "数学表示" "Emmy 函数"]
  :rows [["拉格朗日量" "L(t,q,q̇) = T - V" "自定义函数"]
         ["欧拉-拉格朗日方程" "d/dt(∂L/∂q̇) - ∂L/∂q = 0" "Lagrange-equations"]
         ["哈密顿量" "H(t,q,p) = p·q̇ - L" "Lagrangian->Hamiltonian"]
         ["哈密顿方程" "q̇ = ∂H/∂p, ṗ = -∂H/∂q" "Hamilton-equations"]
         ["动量" "p = ∂L/∂q̇" "lagrangian-momentum"]]})

;; ## 参考资料

;; - [Structure and Interpretation of Classical Mechanics](https://mitpress.mit.edu/sites/default/files/titles/content/sicm_edition_2/book.html)
;; - [Emmy 官方文档](https://emmy.mentat.org/)
;; - [scmutils 参考手册](https://groups.csail.mit.edu/mac/users/gjs/6946/refman.txt)
