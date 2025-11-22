^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.advanced-usage
  (:require [nextjournal.clerk :as clerk]
            [hugsql.common :as c]))

;; # 4.11 Advanced Usage
;; 展示命令/结果选项与原生 SQL 参数安全用法。

(c/load-sql!)
(c/reset-db!)

;; 插入并返回主键 (使用 :insert/getGeneratedKeys, 适配器决定返回结构)
(def rid (c/insert-guest-returning c/ds {:name "Returner" :specialty "id"}))
(clerk/md (str "generated keys: " rid))

;; 原生 SQL 参数，调用端白名单
(defn order-safe [field dir]
  (let [field* ({:id "id" :name "name" :created_at "created_at"} field)
        dir* ({:asc "asc" :desc "desc"} dir)]
    (c/order-by-raw c/ds {:field (or field* "id") :dir (or dir* "asc")})))
(clerk/table (order-safe :name :desc))

;; sqlvec 调试
(clerk/code (c/update-guest-sqlvec {:id 1 :specialty "deep"}))
