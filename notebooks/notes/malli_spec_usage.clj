(ns notes.web-tutorial.06-malli-spec-usage
  (:require [nextjournal.clerk :as clerk]))

;; # Demonstrating Malli Spec Usage
;;
;; This notebook demonstrates how to use Malli for data validation and 
;; specification in Clojure web services.

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[malli.core :as m])
  (require '[malli.util :as mu])
  (require '[malli.transform :as transform])
  (require '[malli.error :as me]))

(load-libraries)

;; ## Introduction to Malli
;;
;; Malli is a fast and functional schema library for Clojure and ClojureScript.
;; It allows you to:
;; - Define data specifications (schemas)
;; - Validate data against schemas
;; - Transform data between different representations
;; - Generate documentation and forms from schemas

;; Basic schema validation example
(def SimpleUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email :string]])

;; Validating data against our schema
(def valid-user {:id 1 :name "Alice" :email "alice@example.com"})
(def invalid-user {:id "not-an-int" :name "Bob" :email "bob@example.com"})

;; Check if data is valid according to schema
(m/validate SimpleUserSchema valid-user)
(m/validate SimpleUserSchema invalid-user)

;; ## Different Malli Schema Types
;;
;; Malli provides various types for different validation needs:

(def UserSchema
  [:map
   [:id :int]
   [:name [:and :string [:min 1] [:max 100]]]  ; String with length constraints
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]  ; Email regex validation
   [:age {:optional true} :int]  ; Optional field
   [:role [:enum "admin" "user" "moderator"]]  ; Enumerated values
   [:tags [:vector :string]]  ; Vector of strings
   [:metadata [:map-of :keyword :string]]])  ; Map with keyword keys and string values

;; Test validation with the more complex schema
(def complex-valid-user 
  {:id 1 
   :name "Alice" 
   :email "alice@example.com" 
   :age 30 
   :role "admin" 
   :tags ["developer" "clojurian"] 
   :metadata {:department "engineering" :level "senior"}})

(m/validate UserSchema complex-valid-user)

;; Test with an invalid user
(def complex-invalid-user 
  {:id 1 
   :name "Bob" 
   :email "not-an-email"  ; Invalid email
   :age "thirty"  ; Should be an integer
   :role "superadmin"  ; Not in enum
   :tags ["developer" 123]  ; Vector contains non-string
   :metadata {"department" "engineering"}})  ; Keys should be keywords

(m/validate UserSchema complex-invalid-user)

;; ## Getting detailed validation errors
;;
;; Malli provides detailed information about validation failures:

(defn explain-validation [schema data]
  (when-let [explainer (m/explain schema data)]
    (me/humanize explainer)))

(explain-validation UserSchema complex-invalid-user)

;; ## Schema transformation
;;
;; Malli can transform data between different representations (e.g., string to int):

(def StringUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:age {:optional true} :int]
   [:active :boolean]])

;; Define a transformation to convert strings to appropriate types
(def string-transformer
  (transform/transformer
    (transform/string-transformer)  ; Convert strings to appropriate types
    (transform/strip-extra-keys-transformer)))  ; Remove keys not in schema

;; Test transformation
(def string-user {"id" "123" "name" "Charlie" "age" "45" "active" "true"})
(def transformed-user
  (m/decode StringUserSchema string-user string-transformer))

;; Show the transformation result
(println "Original:" string-user)
(println "Transformed:" transformed-user)
(m/validate StringUserSchema transformed-user)

;; ## Creating and using custom validators
;;
;; You can create custom validation functions:

(defn positive-integer? [x]
  (and (integer? x) (pos? x)))

(def custom-validator
  (m/-validator
    [:function positive-integer?]))

(def ProductSchema
  [:map
   [:id [:function positive-integer?]]
   [:name [:and :string [:min 1]]]
   [:price [:and :double [:min 0.01]]]
   [:in-stock :boolean]])

;; Test custom validator
(m/validate ProductSchema {:id 1 :name "Laptop" :price 999.99 :in-stock true})
(m/validate ProductSchema {:id -5 :name "Laptop" :price 999.99 :in-stock true})  ; Invalid: negative id

;; ## Schema composition and reuse
;;
;; Malli schemas can be composed and reused:

(def AddressSchema
  [:map
   [:street :string]
   [:city :string]
   [:country [:enum "US" "CA" "UK" "AU"]]
   [:postal-code [:and :string [:re #"^[A-Z]\d[A-Z] ?\d[A-Z]\d$|^[\d]{5}$"]]]])  ; US or Canadian postal code

(def UserWithAddressSchema
  [:map
   [:id :int]
   [:name :string]
   [:email :string]
   [:address AddressSchema]])  ; Reuse the AddressSchema

(def user-with-address
  {:id 1
   :name "David"
   :email "david@example.com"
   :address {:street "123 Main St" 
             :city "Anytown" 
             :country "US" 
             :postal-code "12345"}})

(m/validate UserWithAddressSchema user-with-address)

;; ## API request/response validation
;;
;; Let's create schemas for API requests and responses:

(def CreateUserRequest
  [:map
   [:name [:and :string [:min 1] [:max 100]]]
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]
   [:age {:optional true} :int]
   [:subscribe-to-newsletter {:optional true} :boolean]])

(def CreateUserResponse
  [:map
   [:success :boolean]
   [:user UserSchema]
   [:message :string]
   [:timestamp inst?]])

;; Validation function for API requests
(defn validate-create-user-request [request]
  (if (m/validate CreateUserRequest request)
    {:valid true :data (m/decode CreateUserRequest request string-transformer)}
    {:valid false :errors (explain-validation CreateUserRequest request)}))

;; Test request validation
(validate-create-user-request {:name "Eve" :email "eve@example.com" :age 25})
(validate-create-user-request {:name "" :email "invalid-email"})

;; ## Schema generation and introspection
;;
;; Malli allows introspection of schemas:

(def schema-info (m/children UserSchema))
(println "Schema children:" schema-info)

;; ## Using Malli in a web request context
;;
;; Let's simulate how you might use Malli validation in a web request:

(defn create-user-handler [request]
  (let [user-data (get-in request [:parameters :body])]
    (if-let [errors (not (m/validate CreateUserRequest user-data))]
      {:status 400
       :body {:success false 
              :message "Validation failed"
              :errors (explain-validation CreateUserRequest user-data)}}
      {:status 201
       :body {:success true
              :user (assoc user-data :id (inc (rand-int 10000)))
              :message "User created successfully"
              :timestamp (java.util.Date.)}})))

;; Simulate request validation
(create-user-handler {:parameters {:body {:name "Frank" :email "frank@example.com"}}})
(create-user-handler {:parameters {:body {:name "" :email "invalid"}}})

;; ## Schema-based form generation (conceptual)
;;
;; Malli schemas can be used to generate forms automatically:

(defn schema-to-form-fields [schema]
  (let [children (m/children schema)]
    (mapv (fn [[key type-info]]
            {:field key 
             :type (if (vector? type-info) (first type-info) type-info)
             :required (not= (second type-info) :optional)}) 
          children)))

;; Generate form fields from our schema
(schema-to-form-fields UserSchema)

;; ## Advanced Malli features
;;
;; Using union types for polymorphic data:

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

;; Test union schema
(def credit-card-payment
  {:id 1 :amount 99.99 :payment-type {:type "credit-card" :card-number "1234" :expiry-month 12 :expiry-year 25}})

(def paypal-payment
  {:id 2 :amount 49.99 :payment-type {:type "paypal" :paypal-email "user@example.com"}})

(m/validate PaymentSchema credit-card-payment)
(m/validate PaymentSchema paypal-payment)

;; ## Schema evolution and compatibility checking
;;
;; Malli helps with schema evolution by providing tools to check compatibility:

(def OldUserSchema
  [:map
   [:id :int]
   [:name :string]])

(def NewUserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email {:optional true} :string]])

;; Check if data valid for old schema is valid for new schema 
(defn compatible? [old-schema new-schema test-data]
  (and (m/validate old-schema test-data)
       (m/validate new-schema test-data)))

(compatible? OldUserSchema NewUserSchema {:id 1 :name "Grace"})

;; ## Summary of Malli benefits
;;
;; Malli provides:
;; 1. Fast and expressive schema definitions
;; 2. Detailed validation error reporting
;; 3. Data transformation capabilities
;; 4. Schema composition and reuse
;; 5. Integration with web frameworks
;; 6. Schema introspection and generation
;; 7. Support for complex data structures
;; 8. Type safety without static compilation
;;
;; When combined with Reitit (as we saw in the previous notebook), 
;; Malli provides comprehensive input/output validation and documentation for web APIs.

;; ## Complete example: Validating a complex API request
;;
;; Let's create a complete example that uses multiple Malli features:

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
    {:error "Invalid order format"
     :validation-errors (explain-validation OrderSchema order)}))

;; Test order processing
(def sample-order
  {:id 123
   :customer {:id 1 :name "Helen" :email "helen@example.com" :role "user" :tags [] :metadata {}}
   :items [{:product-id 1 :quantity 2 :price 29.99}
           {:product-id 2 :quantity 1 :price 19.99}]
   :status "pending"
   :shipping-address {:street "456 Oak Ave" :city "Othertown" :country "US" :postal-code "67890"}})

(process-order sample-order)

;; Now you have a complete understanding of how to build a web service 
;; with Clojure, starting from basic web servers to advanced validation
;; with Malli schemas.
