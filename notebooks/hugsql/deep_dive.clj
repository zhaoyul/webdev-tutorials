^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.deep-dive
  (:require [nextjournal.clerk :as clerk]))

;; # 5. 深入介绍
;; - 5.1 SQL File Conventions: 见 `resources/hugsql/playground.sql` 注释语法。
;; - 5.2 Command: `:!`/`:?`/`:< !`/`:insert` 在 CRUD/advanced 笔记中演示。
;; - 5.3 Result: `:raw`/`:n`/`:1`/`:*` 已在 CRUD/advanced 中体现。
;; - 5.4 Parameter Types: `:v`/`:v*`/`:t*`/`:i*`/`:sql`/`:snip`、深度取值均在 playground SQL 和对应笔记中运行。
