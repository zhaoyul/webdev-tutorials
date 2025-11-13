(ns notes.demonstrating-middleware-functionality
  (:require [nextjournal.clerk :as clerk]))

;; # 演示中间件功能
;;
;; 本笔记本演示中间件的实际功能，
;; 展示数据如何流经中间件链以及每个
;; 中间件如何转换请求和响应。

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## 理解中间件链
;;
;; 中间件函数按特定顺序应用，每个函数 
;; 都可以在传递给下一个中间件之前修改请求，
;; 并在从下一个中间件获取响应后修改响应。

;; 为了直观地展示这一点，让我们创建记录每个步骤发生情况的中间件：

(defn wrap-logger-step [handler step-name]
  (fn [request]
    (println (str "  -> " step-name ": Processing request"))
    (let [response (handler request)]
      (println (str "  <- " step-name ": Processing response"))
      response)))

;; ## 创建带有步骤日志的中间件链
;;
;; 让我们创建一个处理器和一个可以跟踪的中间件链：

(defn sample-handler [request]
  (println "    Handler: Processing request with URI" (:uri request))
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str "{\"message\": \"Response from handler\", \"uri\": \"" (:uri request) "\"}")})

(def traced-app
  (-> sample-handler
      (wrap-logger-step "Middleware-3")
      (wrap-logger-step "Middleware-2")
      (wrap-logger-step "Middleware-1")))

;; Let's trace a request through the middleware chain:
(comment
  (traced-app {:request-method :get :uri "/test" :headers {}}))

;; 从输出中可以看出，请求的流向是：
;; 1. 中间件-1 接收请求
;; 2. 中间件-2 接收请求 
;; 3. 中间件-3 接收请求
;; 4. 处理器处理请求
;; 5. 中间件-3 处理响应
;; 6. 中间件-2 处理响应
;; 7. 中间件-1 处理响应

;; ## 演示请求修改
;;
;; 让我们创建修改请求的中间件：

(defn wrap-request-modifier [handler]
  (fn [request]
    (println "  -> Request modifier: Adding custom header to request")
    (let [modified-request (assoc-in request [:headers "x-modified-by"] "request-modifier")]
      (handler modified-request))))

;; ## 演示响应修改
;;
;; 让我们创建修改响应的中间件：

(defn wrap-response-modifier [handler]
  (fn [request]
    (let [response (handler request)]
      (println "  <- Response modifier: Adding custom header to response")
      (assoc-in response [:headers "x-modified-by"] "response-modifier"))))

;; ## Complete trace example
;;
;; Let's create a complete example that traces everything:

(defn detailed-trace-handler [request]
  (println "    Handler: Processing with headers" (pr-str (:headers request)))
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"result\": \"success\"}"})

(def detailed-trace-app
  (-> detailed-trace-handler
      wrap-response-modifier
      wrap-request-modifier
      (wrap-logger-step "Final Logger")))

;; Let's trace this request:
(comment
  (detailed-trace-app {:request-method :get :uri "/" :headers {}}))

;; 流程如下：
;; 1. 最终日志记录器接收请求
;; 2. 请求修改器接收请求（并添加头部）
;; 3. 响应修改器接收请求
;; 4. 处理器处理请求
;; 5. 响应修改器处理响应（并添加头部）
;; 6. 请求修改器处理响应
;; 7. 最终日志记录器处理响应

;; ## 实际示例：请求体解析中间件
;;
;; 让我们创建一个实际场景的示例，处理请求体解析：

(defn parse-json-body [request]
  "如果存在则解析 JSON 体并作为 :parsed-body 添加到请求"
  (if-let [body (:body request)]
    (if (string? body)
      (try 
        (assoc request :parsed-body (read-string body))
        (catch Exception e 
          (assoc request :parsed-body-error (.getMessage e))))
      request)
    request))

(defn wrap-json-body-parser [handler]
  (fn [request]
    (let [parsed-request (parse-json-body request)]
      (handler parsed-request))))

;; Create a handler that uses the parsed body:
(defn json-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str "{\"received\": " (pr-str (:parsed-body request)) 
              ", headers\": " (pr-str (:headers request)) "}")})

;; Test the JSON body parser:
(def json-app
  (-> json-handler
      wrap-json-body-parser))

(json-app {:request-method :post 
           :uri "/test" 
           :headers {"content-type" "application/json"} 
           :body "{\"name\": \"clerk\", \"value\": 42}"})


;; ## 有状态中间件示例：会话管理
;;
;; 让我们创建模拟会话管理的中间件：

(def sessions (atom {}))  ; 在实际应用程序中，您会使用适当的会话存储

(defn wrap-session [handler]
  (fn [request]
    ;; Get session ID from cookie or create new one
    (let [session-id (get-in request [:cookies "session-id" :value])
          session-data (get @sessions session-id {})
          new-request (assoc request :session session-data :session-id session-id)]
      (let [response (handler new-request)]
        ;; Save session back to atom and add cookie to response
        (let [updated-session (:session response)
              new-session-id (or session-id (str "sess-" (System/currentTimeMillis)))]
          (swap! sessions assoc new-session-id updated-session)
          (assoc-in response [:headers "Set-Cookie"] (str "session-id=" new-session-id "; Path=/; HttpOnly")))))))

;; Create a handler that uses sessions:
(defn session-handler [request]
  (let [current-count (get-in request [:session :visit-count] 0)
        updated-session {:visit-count (inc current-count)
                         :last-visit (System/currentTimeMillis)}]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (str "{\"visit-count\": " (inc current-count) "}")
     :session updated-session}))

;; Test session middleware:
(def session-app
  (-> session-handler
      wrap-session))

(session-app {:request-method :get :uri "/" :headers {} :cookies {}})
(session-app {:request-method :get :uri "/" :headers {} :cookies {"session-id" {:value "sess-1234567890"}}})

;; ## 错误处理中间件
;;
;; 让我们创建优雅处理错误的中间件：

(defn wrap-error-handler [handler]
  (fn [request]
    (try 
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (str "{\"error\": \"Internal server error\", \"message\": \"" (.getMessage e) "\"}")}))))

;; 创建可能抛出错误的处理器：
(defn risky-handler [request]
  (if (= (:uri request) "/error")
    (throw (Exception. "出现问题！"))
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body "{\"result\": \"success\"}"}))

;; Test error handling:
(def error-handling-app
  (-> risky-handler
      wrap-error-handler))

(error-handling-app {:request-method :get :uri "/ok" :headers {}})
(error-handling-app {:request-method :get :uri "/error" :headers {}})

;; ## 中间件组合模式
;;
;; 让我们看看如何为不同的路由组组织中间件：

(defn public-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"message\": \"Public content\"}"})

(defn private-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str "{\"message\": \"Private content for user\", \"user-id\": \""
              (get-in request [:user :id] "unknown") "\"}")})

;; Authentication middleware
(defn wrap-authentication [handler]
  (fn [request]
    ;; 简单的身份验证检查 - 在实际应用程序中，这会更复杂
    (if (= (get-in request [:headers "authorization"]) "Bearer valid-token")
      (handler (assoc request :user {:id 123 :name "爱丽丝"}))
      {:status 401 :headers {"Content-Type" "application/json"}
       :body "{\"error\": \"未授权\"}"})))

;; 我们使用但未在早期笔记本中定义的中间件的辅助函数
(defn wrap-timing [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)]
      (assoc-in response [:headers "X-Response-Time"] (str duration "ms")))))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization")))))

;; Apply different middleware to different routes
(def app-routes
  {:public (-> public-handler
               wrap-timing
               wrap-cors)
   :private (-> private-handler
                wrap-authentication
                wrap-timing
                wrap-cors)})

;; Simulate routing function
(defn route-dispatcher [request]
  (if (= (subs (:uri request) 0 (min 7 (count (:uri request)))) "/private")
    ((:private app-routes) request)
    ((:public app-routes) request)))

;; Test routing
(route-dispatcher {:request-method :get :uri "/public" :headers {}})
(route-dispatcher {:request-method :get :uri "/private" :headers {"authorization" "Bearer valid-token"}})
(route-dispatcher {:request-method :get :uri "/private" :headers {}})

;; ## 总结
;;
;; 在本笔记本中，我们学习了：
;; 1. 中间件链如何逐步工作
;; 2. 如何跟踪通过中间件的请求
;; 3. 如何创建请求修改和响应修改中间件
;; 4. 实际示例，如 JSON 解析、会话和错误处理
;; 5. 如何为不同的路由组结构化中间件
;;
;; 接下来，我们将添加 Swagger 支持以自动记录我们的 API。
