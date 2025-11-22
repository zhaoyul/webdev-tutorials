^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.install
  (:require [nextjournal.clerk :as clerk]))

;; # 2. 安装
;; deps.edn 与 project.clj 示例, 执行 `clojure -P` 或 `lein deps` 拉取依赖。

;;  deps.edn 示例（Clojure CLI）

;;```clojure
;; {:paths ["src" "resources" "notebooks"]
;;  :deps {org.clojure/clojure {:mvn/version "1.12.3"}
;;         com.layerware/hugsql {:mvn/version "0.5.3"}
;;         com.layerware/hugsql-adapter-next-jdbc {:mvn/version "0.5.3"}
;;         com.github.seancorfield/next.jdbc {:mvn/version "1.3.939"}
;;         com.h2database/h2 {:mvn/version "2.2.224"}}
;;  :aliases
;;  {:repl {:main-opts ["-m" "clojure.main" "-r"]}
;;   :dev  {:extra-deps {io.github.nextjournal/clerk {:mvn/version "0.18.1150"}}}}}
;;```

;;  project.clj 示例（Leiningen）

;;```clojure
;;  (defproject hugsql-demo "0.1.0-SNAPSHOT"
;;    :description "HugSQL 示例项目"
;;    :license {:name "EPL-1.0"}
;;    :dependencies [[org.clojure/clojure "1.12.3"]
;;                   [com.layerware/hugsql "0.5.3"]
;;                   [com.layerware/hugsql-adapter-next-jdbc "0.5.3"]
;;                   [com.github.seancorfield/next.jdbc "1.3.939"]
;;```
