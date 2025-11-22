^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.transactions
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]
            [next.jdbc :as jdbc]))

;; # 4.9 Transactions
;; 使用 next.jdbc 事务对象调用 HugSQL 函数。

(c/load-sql!)
(c/reset-db!)

(jdbc/with-transaction [tx c/ds]
  (c/insert-guest tx {:name "T1" :specialty "tx"})
  (c/insert-guest tx {:name "T2" :specialty "tx"}))

(clerk/table (c/all-guests c/ds {}))
