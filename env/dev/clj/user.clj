;; 开发环境专用的 user 命名空间, 放在 :dev alias 的独立 src 中.

;; 提供常用的 REPL 辅助函数与演示入口.

(ns user
  "开发专用的 user 命名空间."
  (:require
   [clojure.repl :refer [doc source find-doc apropos dir]]
   [lambdaisland.classpath.watch-deps :as watch-deps]
   [clojure.tools.namespace.repl :refer [refresh]]
   [rc.web-tutorial :as app]))

(watch-deps/start! {:aliases [:dev :test]})

(defn help
  "打印常用的 REPL 帮助."
  []
  (println "可用函数: help, reload, greet.")
  (println "示例: (greet) 或 (greet \"Alice\")."))

(defn reload
  "刷新已变更的命名空间."
  []
  (refresh))

(defn greet
  "调用应用的 greet 函数."
  ([] (app/greet {}))
  ([name] (app/greet {:name name})))

