^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.adapters
  (:require [nextjournal.clerk :as clerk]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

;; # 6. 适配器
;; 当前示例使用 next.jdbc 适配器:
(clerk/code '(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

;; 其他适配器:
;; - 默认 clojure.java.jdbc: (require '[hugsql.adapter.clojure-java-jdbc :as a]) (hugsql/set-adapter! (a/hugsql-adapter-clojure-java-jdbc))
;; - clojure.jdbc: (require '[hugsql.adapter.clojure-jdbc :as a]) ...
;; - 自定义: 实现 hugsql.adapter/HugsqlAdapter。
;; - 适配器可在全局 set-adapter!、def-db-fns 选项或调用时传入。
