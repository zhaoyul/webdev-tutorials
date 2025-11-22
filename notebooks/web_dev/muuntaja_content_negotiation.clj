;; # 使用 Muuntaja 进行内容协商

;; Muuntaja 是一个 Clojure 的可插拔编解码库，用于处理不同格式的数据传输。
;; 它提供了一种灵活且可扩展的方式来处理内容协商和编解码。

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns web-dev.muuntaja-content-negotiation
  (:require [clojure.data.json :as json]
            [cheshire.core :as cheshire]
            [hato.client :as http]
            [muuntaja.core :as m]
            [muuntaja.middleware :as middleware]
            [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v])
  (:import [java.io InputStream]))

;; ## 什么是内容协商？

;; 内容协商是 HTTP 协议的一个特性，它允许客户端和服务器就响应的内容格式达成一致。
;; 例如，客户端可以请求 JSON 格式，也可以请求 EDN 格式，服务器根据其支持的格式和客户端偏好来决定响应的格式。

;; ## Muuntaja 基础用法

;; Muuntaja 可以用来编码和解码多种格式的数据

;; ### JSON 编码/解码

(def json-example {:name "Clojure" :type "Lisp" :features ["functional" "concurrent" "dynamic"]})

;; 将 Clojure 数据结构编码为 JSON
(->> json-example
     (m/encode "application/json")
     (.readAllBytes)
     (String.))

;; 将 JSON 解码为 Clojure 数据结构
(m/decode "application/json" (m/encode "application/json" json-example))

;; ### 附加格式支持

;; Muuntaja 默认支持多种格式，包括:
;; - JSON
;; - Transit (JSON, MessagePack)
;; - EDN
;; - MessagePack

;; EDN 格式示例
(m/encode "application/edn" {:language "Clojure" :features ["LISP" "JVM"]})

;; Muuntaja 默认就支持 Transit，不需要额外配置

;; 现在我们可以使用 Transit 格式
(->> (m/encode m/instance "application/transit+json" {:language "Clojure" :features ["LISP" "JVM"]})
     (.readAllBytes)
     (String.))

;; ## 在 Web 服务中使用 Muuntaja

;; 让我们创建一个支持多种格式的 Web 服务
(defn api-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {:message "Hello from content negotiation demo"
          :format (get-in request [:headers "accept"])
          :method (:request-method request)
          :params (:parameters request)}})

;; 创建一个使用 muuntaja 的 Ring 处理器
(def app
  (ring/ring-handler
   (ring/router
    [["/api" {:get api-handler
              :post api-handler}]]
    {:data {:muuntaja m/instance
            :middleware [;; 添加参数解析中间件
                         parameters/parameters-middleware
                         ;; 添加 muuntaja 内容协商中间件
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware]}})
   (ring/create-default-handler)))

;; ## 实际的请求示例

;; 由于我们不能直接发起 HTTP 请求，让我们模拟不同请求格式的处理

;; 模拟 JSON 请求
(def json-request {:uri "/api"
                   :request-method :get
                   :headers {"accept" "application/json"
                             "content-type" "application/json"}})

;; 模拟 EDN 请求
(def edn-request {:uri "/api"
                  :request-method :get
                  :headers {"accept" "application/edn"
                            "content-type" "application/edn"}})

;; 通过中间件处理请求
(def processed-json-request
  (-> json-request
      (assoc :headers (update (:headers json-request) "accept" str))
      app))

;; ## 自定义格式支持

;; Muuntaja 支持添加自定义的编解码格式
;; 使用 cheshire 库进行 JSON 处理
(defn encode-with-cheshire [data]
  (cheshire/encode data))

(defn decode-with-cheshire [data]
  (cheshire/decode data))

;; 使用 cheshire 进行编码的示例
(encode-with-cheshire {:custom true :using "cheshire"})

;; ## 创建支持内容协商的 API 示例

(defn user-api-handler [request]
  (let [user-data {:id 123
                   :name "Alice"
                   :email "alice@example.com"
                   :preferences {:theme "dark"
                                 :notifications true}}]
    {:status 200
     :body user-data}))

(defn users-api-handler [request]
  (let [users-data [{:id 1 :name "Alice" :email "alice@example.com"}
                    {:id 2 :name "Bob" :email "bob@example.com"}]]
    {:status 200
     :body users-data}))

;; 定义一个完整支持内容协商的路由表
(def content-negotiation-app
  (ring/ring-handler
   (ring/router
    [["/user" {:get user-api-handler}]
     ["/users" {:get users-api-handler}]]
    {:data {:muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware]}})
   (ring/create-default-handler)))

;; ## Muuntaja 的内部工作原理

;; 让我们看看 Muuntaja 如何确定响应格式
(defn format-detection-demo []
  {:json (m/encode m/instance "application/json" {:format :detected})
   :edn (m/encode m/instance "application/edn" {:format :detected})
   :transit (m/encode m/instance "application/transit+json" {:format :detected})})

(format-detection-demo)

;; Muuntaja 支持内容协商，这意味着它可以根据客户端的 Accept 头来决定返回格式
;; 让我们构建一个更实际的示例，展示如何在不同的请求中使用不同的格式
(defn content-negotiation-demo []
  (let [sample-data {:service "web-tutorial"
                     :features ["content-negotiation" "multiple-formats" "flexible-api"]}
        json-response (m/encode "application/json" sample-data)
        edn-response (m/encode "application/edn" sample-data)
        transit-response (m/encode m/instance "application/transit+json" sample-data)]
    {:json json-response
     :edn edn-response
     :transit transit-response
     :decoded-json (m/decode "application/json" json-response)
     :decoded-edn (m/decode "application/edn" edn-response)
     :decoded-transit (m/decode m/instance "application/transit+json" transit-response)}))

(content-negotiation-demo)

;; ## 实际应用：在 Reitit 中使用

;; 在实际的 Web API 中，我们通常需要处理来自客户端的不同格式请求
(defn post-users-handler [request]
  (let [received-data (:body-params request)]
    {:status 201
     :body {:message "User created successfully"
            :received received-data
            :content-type (get-in request [:headers "content-type"])}}))

;; 路由定义支持不同的 HTTP 方法和内容类型
(def complete-api-app
  (ring/ring-handler
   (ring/router
    [["/users" {:get users-api-handler
                :post post-users-handler}]]
    {:data {:muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware]}})
   (ring/create-default-handler)))

;; ## 测试内容协商中间件

;; 让我们模拟不同的 HTTP 请求来测试内容协商功能
(defn test-content-negotiation []
  ;; 直接调用应用处理请求
  (complete-api-app {:uri "/users"
                     :request-method :get
                     :headers {"accept" "application/json"}}))

;; 测试不同内容类型的响应
(defn simulate-requests []
  (let [base-request {:uri "/users"
                      :request-method :get
                      :body "{\"name\":\"Test User\",\"email\":\"test@example.com\"}"}]
    {:json-response (complete-api-app (assoc-in base-request [:headers "accept"] "application/json"))
     :edn-response (complete-api-app (assoc-in base-request [:headers "accept"] "application/edn"))
     :transit-response (complete-api-app (assoc-in base-request [:headers "accept"] "application/transit+json"))}))

;; 该应用程序现在可以根据客户端请求的 Accept 头返回不同的格式
;; 同时也可以解码不同格式的请求体

;; ## 实际演示：启动服务器并测试内容协商

;; 现在我们将启动一个真实的 Jetty 服务器来演示内容协商
;; 注意：在实际的 Clerk 笔记本中运行服务器需要小心，这里仅作为演示代码

;; 启动服务器的函数 (这在实际运行时会被调用)
(defn start-server []
  ;; 使用端口0让系统分配空闲端口, 避免占用冲突
  (let [server (jetty/run-jetty complete-api-app {:port 0 :join? false})
        connector (first (.getConnectors server))
        port (.getLocalPort connector)]
    {:server server
     :port port}))

;; ## 使用 HTTP 客户端查看内容协商结果

;; 下面的代码演示如何在本地启动服务端, 并用 HTTP 客户端直接查看返回值
(defonce server-instance (atom nil))

(defn start-demo-server! []
  (when-not @server-instance
    (reset! server-instance (start-server)))
  @server-instance)

(defn stop-demo-server! []
  (when-let [{:keys [server]} @server-instance]
    (.stop server)
    (reset! server-instance nil)))

(defn decode-body [accept body-bytes]
  (try
    (if (= accept "application/transit+json")
      (m/decode m/instance "application/transit+json" body-bytes)
      (m/decode accept body-bytes))
    (catch Exception _
      (String. body-bytes "UTF-8"))))

(defn http-client-results []
  ;; 自动启动示例服务, 请求完毕后立即关闭, 避免端口占用
  (let [{:keys [port]} (start-demo-server!)
        url (format "http://localhost:%s/users" port)]
    (try
      (let [accept-values ["application/json"
                           "application/edn"
                           "application/transit+json"]]
        (into {}
              (for [accept accept-values
                    :let [response (http/get url {:headers {"Accept" accept}
                                                  :as :byte-array})
                          raw-body (String. ^bytes (:body response) "UTF-8")]]
                [accept {:status (:status response)
                         :content-type (get-in response [:headers "content-type"])
                         :raw-body raw-body
                         :decoded (decode-body accept (:body response))}])))
      (finally
        (stop-demo-server!)))))

(defn http-client-table-data []
  (let [results (http-client-results)]
    (for [[accept {:keys [status content-type raw-body decoded]}] results]
      {:accept-header accept
       :status status
       :content-type content-type
       :raw-body raw-body
       :decoded-value decoded})))

(comment
  ;; 如需查看表格结果, 手动运行此块, 运行后会自动启动并关闭示例服务
  (clerk/table
   (http-client-table-data)))

;; 上面的 (clerk/table (http-client-table-data)) 会直接在 Notebook 中渲染一张表格
;; 每行都展示不同 Accept 头的状态码、原始字符串以及 Muuntaja 解码结果, 更直观地查看内容协商行为

;; 示例：如何测试不同格式的请求
;;
;; JSON 请求:
;; curl -H "Accept: application/json" http://localhost:3000/users
;;
;; EDN 请求:
;; curl -H "Accept: application/edn" http://localhost:3000/users
;;
;; Transit JSON 请求:
;; curl -H "Accept: application/transit+json" http://localhost:3000/users
;;
;; 发送不同格式的请求体:
;;
;; 发送 JSON:
;; curl -X POST -H "Content-Type: application/json" -d '{"name":"Test User","email":"test@example.com"}' http://localhost:3000/users
;;
;; 发送 EDN:
;; curl -X POST -H "Content-Type: application/edn" -d '{:name "Test User" :email "test@example.com"}' http://localhost:3000/users

;; 这样，客户端可以通过设置 Accept 头来指定期望的响应格式
;; 服务器会根据内容协商返回相应的格式
