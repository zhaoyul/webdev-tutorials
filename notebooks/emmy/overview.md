# Emmy 教程概览

Emmy 是一个 Clojure/ClojureScript 实现的计算机代数系统, 基于 MIT 的 scmutils 系统.
它专为数学和物理研究设计, 是学习经典力学和微分几何的理想工具.

## 教程结构

### 入门篇
- **[intro.clj](intro.clj)** - Emmy 核心概念与基本用法
  - 基本算术运算
  - 符号计算基础
  - 表达式渲染
  - 向量与矩阵

### 符号计算
- **[symbolic.clj](symbolic.clj)** - 符号表达式与简化
  - 创建符号
  - 表达式简化
  - 三角函数与指数对数
  - 抽象函数 (literal-function)

### 微积分
- **[calculus.clj](calculus.clj)** - 自动微分与数值方法
  - 微分算子 D
  - 高阶导数
  - 偏导数
  - 数值积分
  - 泰勒级数

### 经典力学
- **[mechanics.clj](mechanics.clj)** - 拉格朗日与哈密顿力学
  - 状态空间
  - 拉格朗日量
  - 欧拉-拉格朗日方程
  - 哈密顿量与哈密顿方程
  - 守恒量

### 交互与可视化
- **[interactive_demos.clj](interactive_demos.clj)** - MathBox 与第三方组件集成
  - MathBox 3D 曲线
  - ECharts 交互式曲线
- **[talk_recap.clj](talk_recap.clj)** - 演讲示例复盘与 MathBox 交互
  - 泰勒级数与双摆混沌
  - 势阱能量交换与环面轨迹

## 快速开始

```clojure
(ns my-emmy-exploration
  (:require [emmy.env :as e :refer :all]))

;; 符号计算
(simplify (+ (square (sin 'x)) (square (cos 'x))))
;; => 1

;; 自动微分
(->infix (simplify ((D cube) 'x)))
;; => "3 x²"

;; 拉格朗日力学
(defn L-harmonic [m k]
  (fn [[_ x v]]
    (- (* (/ 1 2) m (square v))
       (* (/ 1 2) k (square x)))))
```

## 相关资源

- [Emmy 官方文档](https://emmy.mentat.org/)
- [Emmy GitHub](https://github.com/mentat-collective/emmy)
- [SICM 在线教材](https://mitpress.mit.edu/sites/default/files/titles/content/sicm_edition_2/book.html)
- [scmutils 参考手册](https://groups.csail.mit.edu/mac/users/gjs/6946/refman.txt)
