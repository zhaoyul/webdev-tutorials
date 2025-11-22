^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.crud
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

(c/load-sql!)
(c/reset-db!)

(def inserted (c/insert-guests c/ds {:guests [["A" "alpha"] ["B" "beta"]]}))
;; 插入条数:
(clerk/md (str inserted))

;; Select & 指定列
(clerk/table (c/guests-by-ids-cols c/ds {:ids [1 2] :cols ["name" "specialty"]}))

;; Tuple 参数示例: (id, name)
(clerk/table [(c/guest-by-id-name c/ds {:id-name [1 "A"]})])

;; Update/Delete
(def upd (c/update-guest c/ds {:id 1 :specialty "gamma"}))
(def del (c/delete-guest c/ds {:id 2}))
(clerk/md (str "更新: " upd ", 删除: " del))
(clerk/table (c/all-guests c/ds {}))
