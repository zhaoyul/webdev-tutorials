(ns notes.malli-spec-usage
  (:require [nextjournal.clerk :as clerk]))

;; # 演示 Malli 规范使用
;;
;; 本笔记本演示如何在 Clojure Web 服务中使用 Malli 进行数据验证和
;; 规范定义。

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[malli.core :as m])
  (require '[malli.util :as mu])
  (require '[malli.transform :as transform])
  (require '[malli.error :as me]))

(load-libraries)

;; ## Malli 介绍
;;
;; Malli 是一个快速且功能性的 Clojure 和 ClojureScript 模式库。
;; 它允许您：
;; - 定义数据规范（模式）
;; - 根据模式验证数据
;; - 在不同表示之间转换数据
;; - 从模式生成文档和表单

;; 基本模式验证示例
(def SimpleUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email :string]])

;; 根据我们的模式验证数据
(def valid-user {:id 1 :name "爱丽丝" :email "alice@example.com"})
(def invalid-user {:id "不是整数" :name "鲍勃" :email "bob@example.com"})

;; 检查数据是否符合模式
(m/validate SimpleUserSchema valid-user)
(m/validate SimpleUserSchema invalid-user)

;; ## 不同的 Malli 模式类型
;;
;; Malli 为不同的验证需求提供各种类型：

(def UserSchema
  [:map
   [:id :int]
   [:name [:string {:min 1 :max 100}]]  ; 有长度约束的字符串
   [:email [:string {:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"}]]  ; 邮箱正则验证
   [:age {:optional true} :int]  ; 可选字段
   [:role [:enum "admin" "user" "moderator"]]  ; 枚举值
   [:tags [:vector :string]]  ; 字符串向量
   [:metadata map?]])  ; 使用关键字键和字符串值的映射

;; 使用更复杂的模式测试验证
(def complex-valid-user
  {:id 1
   :name "爱丽丝"
   :email "alice@example.com"
   :age 30
   :role "admin"
   :tags ["开发者" "clojurian"]
   :metadata {:department "工程部" :level "高级"}})

(m/validate UserSchema complex-valid-user)

;; 使用无效用户进行测试
(def complex-invalid-user
  {:id 1
   :name "鲍勃"
   :email "not-an-email"  ; 无效邮箱
   :age "三十"  ; 应该是整数
   :role "超级管理员"  ; 不在枚举中
   :tags ["开发者" 123]  ; 向量包含非字符串
   :metadata {"department" "engineering"}})  ; 键应该是关键字

(m/validate UserSchema complex-invalid-user)

;; ## 获取详细的验证错误
;;
;; Malli 提供有关验证失败的详细信息：

(defn explain-validation [schema data]
  (when-let [explainer (m/explain schema data)]
    (me/humanize explainer)))

(explain-validation UserSchema complex-invalid-user)

;; ## 模式转换
;;
;; Malli 可以在不同表示之间转换数据（例如，字符串到整数）：

(def StringUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:age {:optional true} :int]
   [:active :boolean]])

;; 定义一个转换以将字符串转换为适当类型
(def string-transformer
  (transform/transformer
    (transform/string-transformer)
    (transform/strip-extra-keys-transformer)))

;; 测试转换
(def string-user {"id" "123" "name" "查理" "age" "45" "active" "true"})
(def keywordized-user (into {} (map (fn [[k v]] [(keyword k) v]) string-user)))
(def transformed-user
  (m/decode StringUserSchema keywordized-user string-transformer))

;; 显示转换结果

;; 原始结果:
string-user

;; 转换后:
transformed-user


(m/validate StringUserSchema transformed-user)

;; ## 创建并使用自定义的验证器
;;
;; You can create custom validation functions:

(defn positive-integer? [x]
  (and (integer? x) (pos? x)))



(def ProductSchema
  [:map
   [:id :int]
   [:name [:string {:min 1}]]
   [:price [:double {:min 0.01}]]
   [:in-stock :boolean]])

;; 测试 custom validator
(m/validate ProductSchema {:id 1 :name "笔记本电脑" :price 999.99 :in-stock true})
(m/validate ProductSchema {:id -5 :name "笔记本电脑" :price 999.99 :in-stock true})  ; 无效：负数ID

;; ## 模式组合和重用
;;
;; Malli 模式可以组合和重用：

(def AddressSchema
  [:map
   [:street :string]
   [:city :string]
   [:country [:enum "US" "CA" "UK" "AU"]]
   [:postal-code [:string {:re #"^[A-Z]\d[A-Z] ?\d[A-Z]\d$|^\d{5}$"}]]])  ; 美国或加拿大邮政编码

(def UserWithAddressSchema
  [:map
   [:id :int]
   [:name :string]
   [:email :string]
   [:address AddressSchema]])  ; 重用 AddressSchema

(def user-with-address
  {:id 1
   :name "大卫"
   :email "david@example.com"
   :address {:street "123 Main St"
             :city "Anytown"
             :country "US"
             :postal-code "12345"}})

(m/validate UserWithAddressSchema user-with-address)

;; ## API 请求/响应验证
;;
;; 让我们为 API 请求和响应创建模式：

(def CreateUserRequest
  [:map
   [:name [:string {:min 1 :max 100}]]
   [:email [:string {:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"}]]
   [:age {:optional true} :int]
   [:subscribe-to-newsletter {:optional true} :boolean]])

(def CreateUserResponse
  [:map
   [:success :boolean]
   [:user UserSchema]
   [:message :string]
   [:timestamp inst?]])

;; API 请求的验证函数
(defn validate-create-user-request [request]
  (if (m/validate CreateUserRequest request)
    {:valid true :data (m/decode CreateUserRequest request string-transformer)}
    {:valid false :errors (explain-validation CreateUserRequest request)}))

;; 测试请求验证
(validate-create-user-request {:name "夏娃" :email "eve@example.com" :age 25})
(validate-create-user-request {:name "" :email "invalid-email"})

;; ## 模式生成和内省
;;
;; Malli 允许对模式进行内省：
^{::clerk/auto-expand-results? true}
(def schema-info (m/children UserSchema))

;; ## 在 Web 请求上下文中使用 Malli
;;
;; 让我们模拟如何在 Web 请求中使用 Malli 验证：

(defn create-user-handler [request]
  (let [user-data (get-in request [:parameters :body])]
    (if-let [errors (not (m/validate CreateUserRequest user-data))]
      {:status 400
       :body {:success false
              :message "验证失败"
              :errors (explain-validation CreateUserRequest user-data)}}
      {:status 201
       :body {:success true
              :user (assoc user-data :id (inc (rand-int 10000)))
              :message "用户创建成功"
              :timestamp (java.util.Date.)}})))

;; 模拟请求验证
(create-user-handler {:parameters {:body {:name "弗兰克" :email "frank@example.com"}}})
(create-user-handler {:parameters {:body {:name "" :email "invalid"}}})

;; ## 基于模式的表单生成（概念性）
;;
;; Malli 模式可用于自动生成表单：

(defn schema-to-form-fields [schema]
  (let [children (m/children schema)]
    (mapv (fn [[key type-info]]
            {:field key
             :type (if (vector? type-info) (first type-info) type-info)
             :required (not= (second type-info) :optional)})
          children)))

;; 从我们的模式生成表单字段
^{::clerk/auto-expand-results? true}
(schema-to-form-fields UserSchema)

;; ## 高级 Malli 功能
;;
;; 使用联合类型处理多态数据：

(def PaymentSchema
  [:map
   [:id :int]
   [:amount :double]
   [:payment-type
    [:or
     [:map
      [:type [:enum "credit-card"]]
      [:card-number :string]
      [:expiry-month :int]
      [:expiry-year :int]]
     [:map
      [:type [:enum "paypal"]]
      [:paypal-email :string]]
     [:map
      [:type [:enum "bank-transfer"]]
      [:account-number :string]
      [:routing-number :string]]]]])

;; 测试联合模式
(def credit-card-payment
  {:id 1 :amount 99.99 :payment-type {:type "credit-card" :card-number "1234" :expiry-month 12 :expiry-year 25}})

(def paypal-payment
  {:id 2 :amount 49.99 :payment-type {:type "paypal" :paypal-email "user@example.com"}})

(m/validate PaymentSchema credit-card-payment)
(m/validate PaymentSchema paypal-payment)

;; ## 模式演进和兼容性检查
;;
;; Malli 通过提供检查兼容性的工具来帮助模式演进：

(def OldUserSchema
  [:map
   [:id :int]
   [:name :string]])

(def NewUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email {:optional true} :string]])

;; 检查对旧模式有效的数据是否对新模式有效
(defn compatible? [old-schema new-schema test-data]
  (and (m/validate old-schema test-data)
       (m/validate new-schema test-data)))

(compatible? OldUserSchema NewUserSchema {:id 1 :name "格蕾丝"})

;; ## Malli 优势总结
;;
;; Malli 提供：
;; 1. 快速且富有表现力的模式定义
;; 2. 详细的验证错误报告
;; 3. 数据转换功能
;; 4. 模式组合和重用
;; 5. 与 Web 框架集成
;; 6. 模式内省和生成
;; 7. 支持复杂数据结构
;; 8. 无需静态编译的类型安全
;;
;; 与 Reitit 结合使用（如我们在前面的笔记本中看到的），
;; Malli 为 Web API 提供全面的输入/输出验证和文档。

;; ## 完整示例：验证复杂的 API 请求
;;
;; 让我们创建一个使用多个 Malli 功能的完整示例：

(def OrderSchema
  [:map
   [:id :int]
   [:customer UserSchema]
   [:items [:vector
            [:map
             [:product-id :int]
             [:quantity :int]
             [:price :double]]]]
   [:total :double]
   [:status [:enum "pending" "processing" "shipped" "delivered" "cancelled"]]
   [:created-at inst?]
   [:shipping-address AddressSchema]])

(defn process-order [order]
  (if (m/validate OrderSchema order)
    (let [total (reduce + (map #(* (:quantity %) (:price %)) (:items order)))]
      (assoc order
             :total total
             :status "pending"
             :created-at (java.util.Date.)))
    {:error "订单格式无效"
     :validation-errors (explain-validation OrderSchema order)}))

;; 测试订单处理
(def sample-order
  {:id 123
   :customer {:id 1 :name "海伦" :email "helen@example.com" :role "user" :tags [] :metadata {}}
   :items [{:product-id 1 :quantity 2 :price 29.99}
           {:product-id 2 :quantity 1 :price 19.99}]
   :status "pending"
   :shipping-address {:street "456 Oak Ave" :city "Othertown" :country "US" :postal-code "67890"}})

^{::clerk/auto-expand-results? true}
(process-order sample-order)

;; 现在您已完全了解如何使用 Clojure 构建 Web 服务，
;; 从基本的 Web 服务器到使用 Malli 模式的高级验证。
