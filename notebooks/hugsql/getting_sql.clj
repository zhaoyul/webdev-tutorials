^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.getting-sql
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 3.1 Start with SQL
;; SQL 文件 `resources/hugsql/playground.sql` 展示 HugSQL 注释约定。示例片段:
(clerk/code (slurp "resources/hugsql/playground.sql"))
