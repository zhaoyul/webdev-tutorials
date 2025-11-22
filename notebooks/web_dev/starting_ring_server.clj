;; # 启动一个 Ring 服务器
;;
;; 本笔记本演示如何创建一个更复杂的 Ring 服务器
;; 具有路由和更好的响应处理。
(ns notes.starting-ring-server
  (:require [nextjournal.clerk :as clerk]
            [ring.adapter.jetty :as jetty]))

;; ## 创建具有路由的更复杂处理器
;;
;; 让我们构建一个可以根据 URI 不同而做出不同响应的处理器：
^{::clerk/visibility {:code :show :result :hide}}
(defn route-handler [request]
  (case (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "text/html"}
         :body "<h1>首页</h1><p>欢迎使用我们的 Web 服务！</p><ul><li><a href=\"/api/users\">用户 API</a></li><li><a href=\"/api/status\">状态 API</a></li></ul>"}
    "/api/users" {:status 200
                  :headers {"Content-Type" "application/json"}
                  :body "[{\"id\": 1, \"name\": \"爱丽丝\"}, {\"id\": 2, \"name\": \"鲍勃\"}]"}
    "/api/status" {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body "{\"status\": \"运行中\", \"version\": \"1.0.0\"}"}
    {:status 404
     :headers {"Content-Type" "text/html"}
     :body "<h1>未找到</h1><p>请求的资源未找到。</p>"}))

;; 测试一下我们的handler
^{::clerk/auto-expand-results? true}
(route-handler {:request-method :get :uri "/" :headers {}})
^{::clerk/auto-expand-results? true}
(route-handler {:request-method :get :uri "/api/users" :headers {}})
^{::clerk/auto-expand-results? true}
(route-handler {:request-method :get :uri "/nonexistent" :headers {}})

;; ## 使用 Ring 中间件
;;
;; 中间间允许我们修改 requests 和 responses.

;; 看一些例子

^{::clerk/visibility {:code :show :result :hide}}
(defn wrap-logger
  "记录 requests 的中间件"
  [handler]
  (fn [request]
    (let [start-time (System/nanoTime)
          response (handler request)
          duration (/ (- (System/nanoTime) start-time) 1000000.0)]
      (println (str "Request: " (:request-method request) " " (:uri request)
                    ", Status: " (:status response)
                    ", Duration: " (format "%.2f" duration) "ms"))
      response)))

^{::clerk/visibility {:code :show :result :hide}}
(defn wrap-content-type
  "检查content type, 如果不存在, 则添加"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (nil? (get-in response [:headers "Content-Type"]))
        (assoc-in response [:headers "Content-Type"] "text/plain")
        response))))

;; 让我们将中间件与处理器组合：
^{::clerk/visibility {:code :show :result :hide} ::clerk/auto-expand-results? true}
(def app
  (-> route-handler
      wrap-logger
      wrap-content-type))

;; 测试组合的应用程序：
^{::clerk/auto-expand-results? true}
(app {:request-method :get :uri "/" :headers {}})

;; ## 正确提供 JSON 响应
;;
;; 让我们使用 Ring 中间件增强我们的处理器以正确提供 JSON：

^{::clerk/visibility {:code :show :result :hide}}
(defn json-handler [request]
  (case (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:message "欢迎使用 API", :version "1.0.0"}}
    "/api/users" {:status 200
                  :headers {"Content-Type" "application/json"}
                  :body {:users [{:id 1 :name "爱丽丝"} {:id 2 :name "鲍勃"}]}}
    "/api/status" {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body {:status "运行中" :version "1.0.0"}}
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body {:error "未找到"}}))

;; ## 使用序列化创建适当的 JSON 响应
;;
;; 让我们创建一个辅助函数来正确序列化 JSON 响应：
^{::clerk/visibility {:code :show :result :hide}}
(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (pr-str data)})

;; 现在让我们创建一个使用此辅助函数的更好处理器：
^{::clerk/visibility {:code :show :result :hide}}
(defn better-json-handler [request]
  (case (:uri request)
    "/" (json-response {:message "欢迎使用 API", :version "1.0.0"})
    "/api/users" (json-response {:users [{:id 1 :name "爱丽丝"} {:id 2 :name "鲍勃"}]})
    "/api/status" (json-response {:status "运行中" :version "1.0.0"})
    (json-response {:error "未找到"} 404)))

;; 测试更好的 JSON 处理器：
^{::clerk/auto-expand-results? true}
(better-json-handler {:request-method :get :uri "/" :headers {}})

;; ## 启动具有中间件栈的服务器
;;
;; 现在让我们启动一个具有中间件栈的完整服务器：
^{::clerk/visibility {:code :show :result :hide}}
(defn start-ring-server []
  (let [full-app
        (-> better-json-handler
            wrap-logger)]
    (jetty/run-jetty full-app {:port 3001 :join? false})))

;; 要启动服务器，您将调用：
(comment
  (def server (start-ring-server))
  ;; 稍后停止服务器：
  ;; (.stop server)
  )

;; ## 总结
;;
;; 在本笔记本中，我们学习了：
;; 1. 如何创建基于路由的处理器
;; 2. 如何编写自定义中间件函数
;; 3. 如何使用线程优先宏将中间件与处理器组合
;; 4. 如何正确处理 JSON 响应
;; 5. 如何使用完整的中间件栈启动服务器
;;
;; 接下来，我们将探讨如何添加和使用各种中间件来增强我们的 Web 服务。
