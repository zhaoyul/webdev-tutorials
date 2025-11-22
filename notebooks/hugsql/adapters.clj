^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.adapters
  (:require [nextjournal.clerk :as clerk]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

;; # 6. 适配器
;; 当前示例使用 next.jdbc 适配器:
(clerk/code '(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

;; clojure.java.jdbc 适配器 (默认为 hugsql 元包内置)
(clerk/code '(do (require '[hugsql.adapter.clojure-java-jdbc :as a])
                 (hugsql/set-adapter! (a/hugsql-adapter-clojure-java-jdbc))))

;; clojure.jdbc 适配器
(clerk/code '(do (require '[hugsql.adapter.clojure-jdbc :as a])
                 (hugsql/set-adapter! (a/hugsql-adapter-clojure-jdbc))))

;; 自定义适配器示例(实现 hugsql.adapter/HugsqlAdapter 协议)
(clerk/code '(defrecord MyAdapter [] hugsql.adapter/HugsqlAdapter
                        (execute [this db sql params options] (println :execute sql params) {:result :raw})
                        (query [this db sql params options] (println :query sql params) [])))
(clerk/md "自定义适配器需要在 set-adapter! 前 require 并满足协议方法 execute/query, 生产环境请替换为真实实现.")

;; 其他适配器:
;; - 默认 clojure.java.jdbc: (require '[hugsql.adapter.clojure-java-jdbc :as a]) (hugsql/set-adapter! (a/hugsql-adapter-clojure-java-jdbc))
;; - clojure.jdbc: (require '[hugsql.adapter.clojure-jdbc :as a]) ...
;; - 自定义: 实现 hugsql.adapter/HugsqlAdapter。
;; - 适配器可在全局 set-adapter!、def-db-fns 选项或调用时传入。
