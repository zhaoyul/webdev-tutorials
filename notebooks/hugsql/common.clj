^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.common
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc :as jdbc]))

;; 全局适配器与数据源
(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
(defonce ds (jdbc/get-datasource {:jdbcUrl "jdbc:h2:mem:hugsql_demo;DB_CLOSE_DELAY=-1"}))

;; 绑定 SQL 的可重载入口
(defn load-sql! []
  (hugsql/def-db-fns "hugsql/playground.sql")
  (hugsql/def-sqlvec-fns "hugsql/playground.sql"))

(load-sql!)

(defn reset-db!
  "重置并预置演示数据."
  []
  (load-sql!)
  (drop-table ds {})
  (create-table ds {})
  (insert-guests ds {:guests [["Westley" "love"]
                              ["Buttercup" "beauty"]
                              ["Inigo" "sword"]
                              ["Vizzini" "intelligence"]]}))

(reset-db!)
