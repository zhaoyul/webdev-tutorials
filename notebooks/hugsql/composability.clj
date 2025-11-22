^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.composability
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 4.10 Composability
;; 演示表达式与 Snippet 组合。

(c/load-sql!)
(c/reset-db!)

;; 表达式：动态列
(def expr-default (c/expr-cols c/ds {}))
(def expr-names (c/expr-cols c/ds {:cols ["name"]}))
(clerk/md (str "表达式默认列数: " (count (first expr-default))))
(clerk/table expr-names)

;; Snippet 组合
(def select-sn (c/select-snip {:cols ["id" "name"]}))
(def where-sn (c/where-snip {:cond [(c/cond-snip {:conj "" :cond ["id" "=" 1]})
                                    (c/cond-snip {:conj "or" :cond ["id" "=" 3]})]}))
(def snip-res (c/snip-query c/ds {:select select-sn :from (c/from-snip {}) :where where-sn}))
(clerk/table snip-res)
