(ns notes.starting-web-server
  (:require [nextjournal.clerk :as clerk]))

;; # 使用 Clojure 启动 Web 服务器
;;
;; 本 notebook 演示了如何在 Clojure 中启动一个基本的 Web 服务器。
;; 我们将探讨 HTTP 服务器的基础知识以及如何设置它们。

;; 首先，让我们加载必要的库
^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## 基础知识：创建一个简单的处理函数
;;
;; 在 Ring (Clojure 的标准 Web 应用接口) 中，处理函数 (handler) 只是一个
;; 接收请求 map 并返回响应 map 的函数。

(defn simple-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<h1>欢迎来到我们的 Web 教程！</h1><p>这是一个简单的 Web 服务器。</p>"})

;; 让我们用一个模拟请求来测试我们的处理函数：
(def mock-request {:request-method :get :uri "/" :headers {}})

(simple-handler mock-request)

;; ## 启动服务器
;;
;; 现在，让我们使用 Ring 的 Jetty 适配器启动一个真实的服务器。
;; 服务器将在 3000 端口上运行。

(defn start-server []
  (jetty/run-jetty simple-handler {:port 3000 :join? false}))

;; 要启动服务器，请调用该函数：
(comment
  (def server (start-server))
  ;; 稍后要停止服务器：
  (.stop server))

;; ## 理解请求 map
;;
;; 当一个 HTTP 请求进来时，Ring 提供一个包含所有请求详细信息的 map。
;; 让我们创建一个处理函数来显示请求的结构：

(defn request-inspector [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str (with-out-str (clojure.pprint/pprint request)))})

;; 这个处理函数将整个请求 map 作为响应打印出来。
;; 尝试访问不同的路由，看看请求 map 是如何变化的：

(defn request-info-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<h2>请求信息</h2>"
              "<p><strong>URI:</strong> " (:uri request) "</p>"
              "<p><strong>方法:</strong> " (name (:request-method request)) "</p>"
              "<p><strong>头信息:</strong> " (pr-str (:headers request)) "</p>"
              "<p><strong>查询字符串:</strong> " (:query-string request) "</p>")})

;; 让我们看看请求信息处理函数返回什么：
(request-info-handler {:request-method :get :uri "/hello" :headers {"host" "localhost:3000"} :query-string "name=clerk"})

;; ## 总结
;;
;; 在本 notebook 中，我们学到了：
;; 1. 如何创建基本的 Ring 处理函数
;; 2. 请求在 Ring 中如何表示为 map
;; 3. 如何使用 Jetty 启动 Web 服务器
;; 4. 如何检查和使用请求数据
;;
;; 接下来，我们将探讨 Ring 中间件，它允许我们为处理函数增强附加功能。

(comment
  (clerk/serve! {:port 4455})

  (clerk/serve! {:port 4456 :watch-paths ["notebooks/notes"]})

  (clerk/show! 'notes.starting-web-server))
