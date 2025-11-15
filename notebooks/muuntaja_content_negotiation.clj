;; # 使用 Muuntaja 进行内容协商

;; Muuntaja 是一个 Clojure 的可插拔编解码库，用于处理不同格式的数据传输。
;; 它提供了一种灵活且可扩展的方式来处理内容协商和编解码。

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns muuntaja-content-negotiation
  (:require [clojure.data.json :as json]
            [cheshire.core :as cheshire]
            [muuntaja.core :as m]
            [muuntaja.middleware :as middleware]
            [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

;; ## 什么是内容协商？

;; 内容协商是 HTTP 协议的一个特性，它允许客户端和服务器就响应的内容格式达成一致。
;; 例如，客户端可以请求 JSON 格式，也可以请求 EDN 格式，服务器根据其支持的格式和客户端偏好来决定响应的格式。

;; ## Muuntaja 基础用法

;; Muuntaja 可以用来编码和解码多种格式的数据

;; ### JSON 编码/解码

(def json-example {:name "Clojure" :type "Lisp" :features ["functional" "concurrent" "dynamic"]})

;; 将 Clojure 数据结构编码为 JSON
(m/encode "application/json" json-example)

;; 将 JSON 解码为 Clojure 数据结构
(m/decode "application/json" (m/encode "application/json" json-example))

;; ### 附加格式支持

;; Muuntaja 默认支持多种格式，包括:
;; - JSON
;; - Transit (JSON, MessagePack)
;; - EDN
;; - MessagePack
;; - YAML

;; EDN 格式示例
(m/encode "application/edn" {:language "Clojure" :features ["LISP" "JVM"]})

;; YAML 格式示例 (如果已配置)
(m/encode "application/yaml" {:language "Clojure" :features ["LISP" "JVM"]})

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
              :post api-handler}]])
   (ring/create-default-handler)
   {:data {:muuntaja m/instance
           :middleware [;; 添加参数解析中间件
                        parameters/parameters-middleware
                        ;; 添加 muuntaja 内容协商中间件
                        muuntaja/format-negotiate-middleware
                        muuntaja/format-response-middleware
                        muuntaja/format-request-middleware]}}))

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
(def custom-instance
  (m/create
   (-> m/default-options
       (m/encode-with :json cheshire/encode)
       (m/decode-with :json cheshire/decode))))

;; 使用自定义配置的实例
(m/encode custom-instance "application/json" {:custom true :using "cheshire"})

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
     ["/users" {:get users-api-handler}]])
   (ring/create-default-handler)
   {:data {:muuntaja m/instance
           :middleware [parameters/parameters-middleware
                        muuntaja/format-negotiate-middleware
                        muuntaja/format-response-middleware
                        muuntaja/format-request-middleware]}}))

;; ## Muuntaja 的内部工作原理

;; 让我们看看 Muuntaja 如何确定响应格式
(defn format-detection-demo []
  {:json (m/encode m/instance "application/json" {:format :detected})
   :edn (m/encode m/instance "application/edn" {:format :detected})
   :yaml (m/encode m/instance "application/yaml" {:format :detected})})

(format-detection-demo)

;; Muuntaja 支持内容协商，这意味着它可以根据客户端的 Accept 头来决定返回格式
;; 让我们构建一个更实际的示例，展示如何在不同的请求中使用不同的格式
(defn content-negotiation-demo []
  (let [sample-data {:service "web-tutorial"
                     :features ["content-negotiation" "multiple-formats" "flexible-api"]}
        json-response (m/encode "application/json" sample-data)
        edn-response (m/encode "application/edn" sample-data)]
    {:json json-response
     :edn edn-response
     :decoded-json (m/decode "application/json" json-response)
     :decoded-edn (m/decode "application/edn" edn-response)}))

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
                :post post-users-handler}]])
   (ring/create-default-handler)
   {:data {:muuntaja m/instance
           :middleware [parameters/parameters-middleware
                        muuntaja/format-negotiate-middleware
                        muuntaja/format-response-middleware
                        muuntaja/format-request-middleware]}}))

;; 该应用程序现在可以根据客户端请求的 Accept 头返回不同的格式
;; 同时也可以解码不同格式的请求体