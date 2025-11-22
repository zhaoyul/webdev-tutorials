^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.getting-clj
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 3.2 Clojure 接入
;; `hugsql.common` 已调用 def-db-fns/def-sqlvec-fns 绑定 SQL, 提供 ds 与 reset-db!。
;; 下方示例查看 sqlvec 输出。

(c/load-sql!)

(def sqlvec-example (c/insert-guest-sqlvec {:name "Sample" :specialty "demo"}))
(clerk/code sqlvec-example)
