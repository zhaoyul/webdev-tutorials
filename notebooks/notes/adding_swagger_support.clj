(ns notes.adding-swagger-support
  (:require [nextjournal.clerk :as clerk]))

;; # Adding Swagger Support
;;
;; This notebook demonstrates how to add Swagger API documentation 
;; to our Clojure web service using Reitit.

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response])
  (require '[metosin.reitit.coercion.malli])
  (require '[metosin.reitit.swagger :as swagger])
  (require '[metosin.reitit.swagger-ui :as swagger-ui])
  (require '[metosin.reitit.ring :as ring])
  (require '[metosin.reitit.ring.middleware.muuntaja :as muuntaja])
  (require '[metosin.reitit.ring.middleware.parameters :as parameters])
  (require '[metosin.reitit.ring.coercion :as coercion])
  (require '[muuntaja.core :as m]))

(load-libraries)

;; ## Introduction to Reitit and Swagger
;;
;; Reitit is a fast data-driven router for Clojure. It provides:
;; - Fast routing
;; - Swagger/OpenAPI support
;; - Schema-based input/output coercion
;; - Content negotiation
;;
;; We'll use it to create a well-documented API with automatic Swagger UI.

;; First, let's create a simple API with documentation:

(def swagger-docs
  ["/swagger.json"
   {:get {:no-doc true
          :swagger {:info {:title "Web Tutorial API"
                           :description "API for the web development tutorial"
                           :version "1.0.0"}}
          :handler (swagger/create-swagger-handler)}}])

;; ## Creating API endpoints with documentation
;;
;; Let's define some routes with detailed documentation:

(def api-routes
  [["/"
    {:get {:summary "Home endpoint"
           :description "Returns a welcome message"
           :responses {200 {:body {:message string?}}}
           :handler (fn [_] {:status 200 
                            :body {:message "Welcome to the Web Tutorial API"}})}}]
   
   ["/users"
    {:get {:summary "Get all users"
           :description "Returns a list of all users in the system"
           :parameters {}
           :responses {200 {:body {:users vector?}}}
           :handler (fn [_] {:status 200 
                            :body {:users [{:id 1 :name "Alice" :email "alice@example.com"}
                                          {:id 2 :name "Bob" :email "bob@example.com"}]}})}
     :post {:summary "Create a new user"
            :description "Creates a new user with the provided information"
            :parameters {:body {:name string? :email string?}}
            :responses {201 {:body {:id int? :name string? :email string?}}}
            :handler (fn [request] 
                      {:status 201 
                       :body {:id (inc (rand-int 1000)) 
                              :name (get-in request [:parameters :body :name])
                              :email (get-in request [:parameters :body :email])}})}}]
   
   ["/users/:id"
    {:get {:summary "Get user by ID"
           :description "Returns a specific user by their ID"
           :parameters {:path {:id int?}}
           :responses {200 {:body {:id int? :name string? :email string?}}}
           :handler (fn [request] 
                     {:status 200 
                      :body {:id (get-in request [:parameters :path :id])
                             :name (str "User " (get-in request [:parameters :path :id]))
                             :email (str "user" (get-in request [:parameters :path :id]) "@example.com")}})}}
    
    {:delete {:summary "Delete a user"
              :description "Deletes a user by their ID"
              :parameters {:path {:id int?}}
              :responses {200 {:body {:message string?}}}
              :handler (fn [request] 
                        {:status 200 
                         :body {:message (str "User " (get-in request [:parameters :path :id]) " deleted")}})}}]])

;; ## Creating the Reitit router
;;
;; Now let's create a router that combines our API routes with Swagger documentation:

(def app
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/api" api-routes]
       ["/swagger-ui/*" (swagger-ui/create-swagger-ui-handler {:path "/"})]]
      {:data {:coercion (metosin.reitit.coercion.malli/create)
              :muuntaja m/instance
              :middleware [parameters/parameters-middleware
                           muuntaja/format-middleware
                           coercion/coercion-middleware]}})
    (ring/create-default-handler
      {:not-found (constantly {:status 404 :body "Not Found"})
       :method-not-allowed (constantly {:status 405 :body "Method Not Allowed"})
       :not-acceptable (constantly {:status 406 :body "Not Acceptable"})})))

;; ## Testing the API endpoints
;;
;; Let's simulate requests to our API to see how it behaves:

;; For this example, we need a helper to simulate how the parameters would be processed
;; In a real Reitit application, parameters would be automatically coerced

(defn simulate-api-call [path method & [params]]
  {:path path
   :method method
   :parameters (or params {})
   :url (str "http://localhost:3000" path)})

(simulate-api-call "/" :get)

(simulate-api-call "/api/users" :get)

(simulate-api-call "/api/users" :post {:body {:name "Charlie" :email "charlie@example.com"}})

(simulate-api-call "/api/users/123" :get {:path {:id 123}})

;; ## Swagger UI endpoint
;;
;; The API will automatically have a Swagger UI available at /swagger-ui/
;; This provides an interactive documentation interface for your API.

;; ## Defining API schemas with Malli (for later use)
;;
;; While we're focusing on Swagger now, let's also see how we can define
;; schemas that will be used for both documentation and validation:

(def UserSchema
  [:map
   [:id :int]
   [:name :string]
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]])

(def CreateUserRequest
  [:map
   [:name :string]
   [:email [:and :string [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"]]]])

;; These schemas can be used for both documentation and runtime validation.

;; ## Creating a more detailed API example
;;
;; Let's create a more comprehensive API with different types of parameters:

(def detailed-api-routes
  [["/products"
    {:get {:summary "Search for products"
           :description "Search for products with optional filters"
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
                         :body {:products [{:id 1 :name "Laptop" :price 999.99 :category "Electronics"}
                                           {:id 2 :name "Book" :price 19.99 :category "Education"}]
                                :total 2 :page (get query-params :page 1)}}))}}]
   
   ["/products/:id"
    {:get {:summary "Get product by ID"
           :description "Returns detailed information about a specific product"
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
                              :name (str "Product " (get-in request [:parameters :path :id]))
                              :description (str "Description for product " (get-in request [:parameters :path :id]))
                              :price (double (+ 10 (get-in request [:parameters :path :id])))
                              :category "General"}})}]
    
    {:put {:summary "Update a product"
           :description "Updates an existing product with new information"
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

;; ## Setting up the complete application with detailed API
;;
;; Let's create the full router with both simple and detailed APIs:

(def complete-app
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/api" api-routes]
       ["/api" detailed-api-routes]
       ["/swagger-ui/*" (swagger-ui/create-swagger-ui-handler {:path "/"})]]
      {:data {:coercion (metosin.reitit.coercion.malli/create)
              :muuntaja m/instance
              :middleware [parameters/parameters-middleware
                           muuntaja/format-middleware
                           coercion/coercion-middleware]}})
    (ring/create-default-handler
      {:not-found (constantly {:status 404 :body "Not Found"})
       :method-not-allowed (constantly {:status 405 :body "Method Not Allowed"})
       :not-acceptable (constantly {:status 406 :body "Not Acceptable"})})))

;; ## Starting a server with Swagger documentation
;;
;; To start a server with this API and automatic Swagger documentation:

(defn start-swagger-server []
  (jetty/run-jetty complete-app {:port 3002 :join? false}))

;; To start the server, you would call:
(comment
  (def server (start-swagger-server))
  ;; To stop the server later:
  ;; (.stop server)
  ;;
  ;; Then visit http://localhost:3002/swagger-ui/ to see the interactive documentation
)

;; ## Key benefits of using Swagger with Reitit
;;
;; 1. Automatic documentation generation
;; 2. Interactive API testing in the browser
;; 3. Clear endpoint specifications
;; 4. Parameter validation
;; 5. Client SDK generation
;; 6. Standards compliance (OpenAPI 3.0)

;; ## Summary
;;
;; In this notebook, we learned:
;; 1. How to integrate Swagger documentation with Reitit
;; 2. How to define API endpoints with rich documentation
;; 3. How to use schemas for both documentation and validation
;; 4. How to create interactive API documentation
;; 5. How to set up parameter validation
;;
;; Next, we'll explore Malli specifications for advanced data validation.

;; Helper functions we reference but don't fully implement in this notebook
(defn wrap-timing [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)]
      (assoc-in response [:headers "X-Response-Time"] (str duration "ms")))))
