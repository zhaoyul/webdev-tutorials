^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.usage-fns
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 4. 使用 hugsql - 函数生成
;; 4.1 def-db-fns / 4.2 def-sqlvec-fns 已在 common 中绑定。
;; 下面展示 CRUD 示例与 sqlvec 输出。

(c/load-sql!)
(c/reset-db!)

(def all (c/all-guests c/ds {}))
(clerk/table all)

;; sqlvec 调试
(clerk/code (c/guest-by-id-sqlvec {:id 1}))

;; 4.3 其他函数: 可使用 map-of-db-fns 系列, 此处聚焦 def-db-fns 与 sqlvec。
