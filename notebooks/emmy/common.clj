^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.common
  "Emmy 笔记公共模块: 环境配置与辅助函数."
  (:require [emmy.env :as e]))

;; ## Emmy 环境配置
;;
;; Emmy 提供了 `emmy.env` 命名空间, 包含了所有常用的数学函数和操作符.
;; 我们通过这个公共模块来统一配置环境.

;; ### 渲染辅助函数

(defn render
  "将表达式简化并转换为 infix 格式的字符串."
  [expr]
  (-> expr e/simplify e/->infix))

(defn render-tex
  "将表达式简化并转换为 TeX 格式的字符串."
  [expr]
  (-> expr e/simplify e/->TeX))

;; ### 常用数学常量

(def π e/pi)
(def τ (* 2 e/pi))
(def e-const (e/exp 1))

;; ### 常用符号定义

(defn make-symbols
  "创建一组符号变量."
  [& names]
  (mapv e/literal-number names))

;; ## 示例数据

(def sample-polynomial
  "示例多项式: x^3 + 2x^2 - x + 1"
  (let [x (e/literal-number 'x)]
    (e/+ (e/expt x 3)
         (e/* 2 (e/expt x 2))
         (e/- x)
         1)))

(def sample-trig-expr
  "示例三角表达式: sin²(x) + cos²(x)"
  (let [x (e/literal-number 'x)]
    (e/+ (e/expt (e/sin x) 2)
         (e/expt (e/cos x) 2))))
