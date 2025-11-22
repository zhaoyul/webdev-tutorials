(ns notes.adding-simple-middleware
  (:require [nextjournal.clerk :as clerk]
            [clojure.data.json :refer [read-str]]))

;; # 添加简单的中间件
;;
;; 本 notebook 演示了如何创建和添加简单的中间件
;; 来增强我们的 Web 服务功能。

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## 理解 Ring 中间件
;;
;; Ring 中间件是一个高阶函数，它接收一个处理函数 (handler) 并返回一个新的处理函数。
;; 它可以在将请求传递给处理函数之前修改请求，和/或在处理函数返回响应后修改响应。

;; 简单的中间件模板：
(defn my-middleware [handler]
  (fn [request]
    ;; 在处理函数之前处理请求
    (let [response (handler request)]
      ;; 在处理函数之后处理响应
      response)))

;; ## 添加 CORS (跨域资源共享) 中间件
;;
;; 这个中间件添加了允许跨域请求的头信息：

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization")))))

;; ## 添加请求 ID 中间件
;;
;; 这个中间件为每个请求添加一个唯一的 ID，用于跟踪：

(defn wrap-request-id [handler]
  (fn [request]
    (let [request-id (str "req-" (System/currentTimeMillis) "-" (rand-int 10000))]
      (handler (assoc request :request-id request-id)))))

;; ## 添加计时中间件
;;
;; 这个中间件测量处理每个请求所需的时间：

(defn wrap-timing [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)]
      (assoc-in response [:headers "X-Response-Time"] (str duration "ms")))))

;; ## 添加模拟身份验证的中间件
;;
;; 这个中间件模拟检查身份验证：

(defn wrap-auth [handler]
  (fn [request]
    (if (= (:uri request) "/protected")
      (let [auth-header (get-in request [:headers "authorization"])]
        (if (= auth-header "Bearer secret-token")
          (handler (assoc request :user {:id 1 :name "authenticated-user"}))
          {:status 401 :headers {"Content-Type" "application/json"}
           :body "{\"error\": \"Unauthorized\"}"}))
      (handler request))))

;; ## 创建一个处理函数来测试我们的中间件
;;
;; 让我们创建一个响应不同路由的处理函数：

(defn api-handler [request]
  (case (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "application/json"}
         :body "{\"message\": \"Public endpoint\", \"request-id\": \""
         (get request :request-id "none") "\"}"}
    "/protected" {:status 200
                  :headers {"Content-Type" "application/json"}
                  :body (str "{\"message\": \"Protected content\", \"user\": \""
                             (get-in request [:user :name] "unknown") "\"}")}
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body "{\"error\": \"Not Found\"}"}))

;; ## 组合中间件
;;
;; 让我们组合多个中间件函数：

(def app-with-middleware
  (-> api-handler
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors))

;; 让我们用中间件来测试我们的应用程序：
(app-with-middleware {:request-method :get :uri "/" :headers {}})

;; 测试受保护的端点（无身份验证）：
(app-with-middleware {:request-method :get :uri "/protected" :headers {}})

;; 测试受保护的端点（有身份验证）：
(app-with-middleware {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}})

;; ## 创建一个转换响应体的自定义中间件
;;
;; 这个中间件为 JSON 响应添加一个时间戳：

(defn wrap-timestamp [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and (= (get-in response [:headers "Content-Type"]) "application/json")
               (string? (:body response)))
        (let [parsed-body (try (clojure.data.json/read-str (:body response))
                               (catch Exception _ nil))]
          (if (map? parsed-body)
            (assoc response :body (clojure.data.json/write-str (assoc parsed-body :timestamp (System/currentTimeMillis))))
            response))
        response))))

;; ## 测试组合的中间件堆栈
;;
;; 让我们创建一个包含所有中间件的应用程序：

(def full-app
  (-> api-handler
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors
      wrap-timestamp))

;; 测试完整的应用程序：
(full-app {:request-method :get :uri "/" :headers {}})

(full-app {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}})

;; ## 创建一个调试中间件
;;
;; 这个中间件帮助我们调试请求/响应周期中发生的事情：

(defn wrap-debug [handler]
  (fn [request]
    (println "调试：处理请求到" (:uri request) "，方法为" (:request-method request))
    (println "调试：请求头信息：" (pr-str (:headers request)))
    (let [response (handler request)]
      (println "调试：响应状态" (:status response))
      (println "调试：响应头信息：" (pr-str (:headers response)))
      response)))

;; 让我们创建一个调试版本：
(def debug-app
  (-> api-handler
      wrap-debug
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors
      wrap-timestamp))

;; 要查看调试输出，请取消以下代码的注释：
(comment
  (debug-app {:request-method :get :uri "/" :headers {}})
  (debug-app {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}}))

;; ## 总结
;;
;; 在本 notebook 中，我们学到了：
;; 1. 如何创建各种类型的中间件 (CORS, 请求 ID, 计时, 身份验证)
;; 2. 如何使用线程优先操作符组合中间件
;; 3. 中间件如何修改请求和响应
;; 4. 如何创建转换响应体的中间件
;; 5. 如何创建调试中间件
;;
;; 接下来，我们将更深入地演示中间件的功能，并了解它们如何集成。
