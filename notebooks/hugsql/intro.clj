^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.intro
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 1. 介绍
;; HugSQL 强调 SQL 优先, 通过 SQL 注释生成 Clojure 函数, 支持多种参数替换与适配器。
;; 下方各笔记继续用可执行示例演示。
