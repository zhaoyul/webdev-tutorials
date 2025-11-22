;; # 添加 Swagger 支持
;;
;; 演示如何使用 Reitit 为 Clojure Web 服务添加 Swagger API 文档.

;; 增加依赖
^{:nextjournal.clerk/toc true}
(ns notes.adding-swagger-support
  (:require [nextjournal.clerk :as clerk]
            [ring.adapter.jetty :as jetty]
            [reitit.coercion.malli :as malli]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [muuntaja.core :as m]))


;; ## Reitit 和 Swagger 介绍
;;
;; Reitit 是一个方便的数据驱动路由器, 用于 Clojure. 它提供:
;; - 路由 route
;; - Swagger/OpenAPI 支持 swagger
;; - 基于模式的输入/输出强制转换 malli
;; - 内容协商 muuntaja
;;
;; 我们将使用它来创建一个具有自动 Swagger UI 的文档完善的 API.

;; 首先, 让我们创建一个带有文档的简单 API:

(def swagger-docs
  ["/api/swagger.json"
   {:get {:no-doc true
          :swagger {:info {:title "Web 教程 API"
                           :description "Web 开发教程的 API"
                           :version "1.0.0"}}
          :handler (swagger/create-swagger-handler)}}])

;; ## 创建带有文档的 API 端点
;;
;; 让我们定义一些带有详细文档的路由:

(def api-routes
  [["/"
    {:get {:summary "首页端点"
           :description "返回欢迎消息"
           :responses {200 {:body {:message string?}}}
           :handler (fn [_] {:status 200
                             :body {:message "欢迎使用 Web 教程 API"}})}}]

   ["/users"
    {:get {:summary "获取所有用户"
           :description "返回系统中所有用户列表"
           :parameters {}
           :responses {200 {:body {:users vector?}}}
           :handler (fn [_] {:status 200
                             :body {:users [{:id 1 :name "爱丽丝" :email "ailisi@example.com"}
                                            {:id 2 :name "鲍勃" :email "baobei@example.com"}]}})}
     :post {:summary "创建新用户"
            :description "使用提供的信息创建新用户"
            :parameters {:body {:name string? :email string?}}
            :responses {201 {:body {:id int? :name string? :email string?}}}
            :handler (fn [request]
                       {:status 201
                        :body {:id (inc (rand-int 1000))
                               :name (get-in request [:parameters :body :name])
                               :email (get-in request [:parameters :body :email])}})}}]

   ["/users/:id"
    {:get {:summary "根据 ID 获取用户"
           :description "根据 ID 返回特定用户"
           :parameters {:path {:id int?}}
           :responses {200 {:body {:id int? :name string? :email string?}}}
           :handler (fn [request]
                      {:status 200
                       :body {:id (get-in request [:parameters :path :id])
                              :name (str "用户 " (get-in request [:parameters :path :id]))
                              :email (str "yonghu" (get-in request [:parameters :path :id]) "@example.com")}})}}

    {:delete {:summary "删除用户"
              :description "根据 ID 删除用户"
              :parameters {:path {:id int?}}
              :responses {200 {:body {:message string?}}}
              :handler (fn [request]
                         {:status 200
                          :body {:message (str "用户 " (get-in request [:parameters :path :id]) " 已删除")}})}}]])

;; ## 创建 Reitit 路由器
;;
;; 现在让我们创建一个将我们的 API 路由与 Swagger 文档相结合的路由器:

(def app
  (ring/ring-handler
   (ring/router
    [swagger-docs
     ["/api" api-routes]
     ["/swagger-ui/*" (swagger-ui/create-swagger-ui-handler {:path "api"
                                                             :url  "api/swagger.json"})]]
    {:data {:coercion malli/coercion
            :muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         #_coercion/coercion-middleware]}})
   (ring/create-default-handler
    {:not-found (constantly {:status 404 :body "未找到"})
     :method-not-allowed (constantly {:status 405 :body "方法不允许"})
     :not-acceptable (constantly {:status 406 :body "无法接受"})})))

;; ## 测试 API 端点
;;
;; 让我们模拟对 API 的请求, 看看其行为:

;; 对于此示例, 我们需要一个辅助函数来模拟参数的处理方式
;; 在实际的 Reitit 应用程序中, 参数将自动强制转换

(defn simulate-api-call [path method & [params]]
  {:path path
   :method method
   :parameters (or params {})
   :url (str "http://localhost:3000" path)})

(simulate-api-call "/" :get)

(simulate-api-call "/api/users" :get)

(simulate-api-call "/api/users" :post {:body {:name "查理" :email "charli@example.com"}})

(simulate-api-call "/api/users/123" :get {:path {:id 123}})

;; ## Swagger UI 端点
;;
;; API 将在 /swagger-ui/ 自动提供 Swagger UI
;; 这为您的 API 提供交互式文档界面.

;; ## 使用 Malli 定义 API 模式(供以后使用)
;;
;; 虽然我们现在专注于 Swagger, 但也让我们看看如何定义
;; 将用于文档和验证的模式:

(def UserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]])

(def CreateUserRequest
  [:map
   [:name :string]
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]])

;; 这些模式可用于文档和运行时验证.

;; ## 创建更详细的 API 示例
;;
;; 让我们创建一个更全面的 API, 具有不同类型的参数:

(def detailed-api-routes

  [["/products"

    {:get {:summary "查询产品"
           :description "根据可选的筛选条件查询产品"
           :parameters {:query [:map {:closed true}
                                [:category {:optional true} :string]
                                [:min-price {:optional true} :double]
                                [:max-price {:optional true} :double]
                                [:page {:optional true} :int]]}
           :responses {200 {:body [:map
                                   [:products [:vector [:map
                                                        [:id :int]
                                                        [:name :string]
                                                        [:price :double]
                                                        [:category :string]]]]
                                   [:total :int]
                                   [:page :int]]}}
           :handler (fn [request]
                      (let [query-params (get-in request [:parameters :query] {})]
                        {:status 200
                         :body {:products [{:id 1 :name "笔记本电脑" :price 999.99 :category "电子产品"}
                                           {:id 2 :name "书籍" :price 19.99 :category "教育"}]
                                :total 2 :page (get query-params :page 1)}}))}}]



   ["/products/:id"
    {:get {:summary "根据ID获取产品"
           :description "返回特定产品的详细信息"
           :parameters {:path [:map [:id :int]]}
           :responses {200 {:body [:map
                                   [:id :int]
                                   [:name :string]
                                   [:description :string]
                                   [:price :double]
                                   [:category :string]]}}

           :handler (fn [request]
                      {:status 200
                       :body {:id (get-in request [:parameters :path :id])
                              :name (str "产品 " (get-in request [:parameters :path :id]))
                              :description (str "产品 " (get-in request [:parameters :path :id]) " 的描述")
                              :price (double (+ 10 (get-in request [:parameters :path :id])))
                              :category "通用"}})}
     :put {:summary "更新产品"
           :description "使用新信息更新现有产品"
           :parameters {:path [:map [:id :int]]
                        :body [:map
                               [:name {:optional true} :string]
                               [:description {:optional true} :string]
                               [:price {:optional true} :double]
                               [:category {:optional true} :string]]}
           :responses {200 {:body [:map
                                   [:id :int]
                                   [:name :string]
                                   [:description :string]
                                   [:price :double]
                                   [:category :string]]}}

           :handler (fn [request]
                      {:status 200
                       :body (merge {:id (get-in request [:parameters :path :id])}
                                    (get-in request [:parameters :body]))})}}]])

;; ## 设置具有详细 API 的完整应用程序
;;
;; 让我们创建包含简单和详细 API 的完整路由器:

(def complete-app
  (ring/ring-handler
   (ring/router
    [swagger-docs
     ["/api" api-routes]
     ["/api" detailed-api-routes]]
    {:data {:coercion malli/coercion
            :muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         #_coercion/coercion-middleware]}})
   (ring/routes
    (ring/redirect-trailing-slash-handler)
    (swagger-ui/create-swagger-ui-handler {:path "/swagger-ui"
                                           :url "/api/swagger.json"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "未找到"})
      :method-not-allowed (constantly {:status 405 :body "方法不允许"})
      :not-acceptable (constantly {:status 406 :body "无法接受"})}))))

;; ## 启动带有 Swagger 文档的服务器
;;
;; 要启动具有此 API 和自动 Swagger 文档的服务器:

(defn start-swagger-server []
  (jetty/run-jetty #'complete-app {:port 3002 :join? false}))

;; 要启动服务器, 请调用:
(comment
  (def server (start-swagger-server))
  ;; 稍后停止服务器:
  (.stop server)
  ;;
  ;; 然后访问 http://localhost:3002/swagger-ui/ 查看交互式文档
  ;; Swagger JSON 文档可在 http://localhost:3002/api/swagger.json 访问
  )

(clerk/html [:iframe {:width 600
                      :height 800
                      :src "http://localhost:3002/swagger-ui/"}] )

;; ## 使用 Swagger 和 Reitit 的主要好处
;;
;; 1. 自动生成文档
;; 2. 在浏览器中进行交互式 API 测试
;; 3. 清晰的端点规范
;; 4. 参数验证
;; 5. 客户端 SDK 生成
;; 6. 标准合规性(OpenAPI 3.0)

;; ## 总结
;;
;; 在本笔记本中, 我们学习了:
;; 1. 如何将 Swagger 文档与 Reitit 集成
;; 2. 如何使用丰富的文档定义 API 端点
;; 3. 如何使用模式进行文档和验证
;; 4. 如何创建交互式 API 文档
;; 5. 如何设置参数验证
;;
;; 接下来, 我们将探索 Malli 规范以进行高级数据验证.

;; Helper functions we reference but don't fully implement in this notebook
(defn wrap-timing [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)]
      (assoc-in response [:headers "X-Response-Time"] (str duration "ms")))))
