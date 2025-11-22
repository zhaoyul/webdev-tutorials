;; 开发环境专用的 user 命名空间, 放在 :dev alias 的独立 src 中.
;; 提供常用的 REPL 辅助函数与 Clerk 操作入口.

(ns user
  "开发专用的 user 命名空间."
  (:require
   [clojure.repl :refer [doc source find-doc apropos dir]]
   [lambdaisland.classpath.watch-deps :as watch-deps]
   [clojure.tools.namespace.repl :refer [refresh]]
   [nextjournal.clerk :as clerk]))

(defonce ^:private clerk-server (atom nil))
(defonce ^:private watcher (atom nil))

(defn help
  "打印常用的 REPL 帮助."
  []
  (println "可用函数: help, reload, start-watch!, stop-watch!, start-clerk!, stop-clerk!, show-notebook, publish-notebooks.")
  (println "示例: (start-watch!) (start-clerk!) (show-notebook \"notebooks/clojure112_features.clj\")."))

(defn reload
  "刷新已变更的命名空间."
  []
  (refresh))

(declare stop-clerk!)

(defn start-clerk!
  "启动 Clerk, 默认监听 notebooks 目录, 返回当前服务信息.
  opts 可自定义 :watch-paths :port :browse? 等 Clerk 选项."
  ([] (start-clerk! {:watch-paths ["notebooks"]
                     :browse? false
                     :port 7777}))
  ([opts]
   (when @clerk-server
     (stop-clerk!))
   (let [server (clerk/serve! opts)]
     (reset! clerk-server server)
     (println "Clerk 已启动, 监听端口" (:port opts))
     server)))

(defn stop-clerk!
  "停止 Clerk 服务与 watch."
  []
  (when @clerk-server
    (if-let [halt (resolve 'nextjournal.clerk/halt!)]
      (halt)
      (when-let [stop-fn (:stop-fn @clerk-server)]
        (stop-fn)))
    (reset! clerk-server nil)
    (println "Clerk 已停止.")))

(defn show-notebook
  "在 Clerk 中打开指定 notebook 文件路径."
  [path]
  (clerk/show! path))

(defn publish-notebooks
  "构建静态 notebook 页面, 默认输出到 public 目录.
  参数可传 {:paths [..] :out-path \"public\"} 自定义."
  ([] (publish-notebooks {:paths ["notebooks"]
                          :out-path "public"}))
  ([opts]
   (clerk/build! opts)
   (println "Clerk build 完成:" opts)))

(defn start-watch!
  "启动依赖与源码的变更监控, 使用 :dev/:test alias."
  []
  (when-not @watcher
    (reset! watcher (watch-deps/start! {:aliases [:dev :test]}))
    (println "watch-deps 已启动.")))

(defn stop-watch!
  "停止变更监控."
  []
  (when-let [w @watcher]
    (watch-deps/stop! w)
    (reset! watcher nil)
    (println "watch-deps 已停止.")))

(comment
  (help)
  (reload)
  (start-clerk!)
  (show-notebook "notebooks/clojure112_features.clj")
  (publish-notebooks))
