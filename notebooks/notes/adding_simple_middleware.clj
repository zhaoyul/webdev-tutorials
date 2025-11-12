(ns notes.adding-simple-middleware
  (:require [nextjournal.clerk :as clerk]
            [clojure.data.json :refer [read-str]]))

;; # Adding Simple Middleware
;;
;; This notebook demonstrates how to create and add simple middleware
;; to enhance our web service functionality.

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## Understanding Ring Middleware
;;
;; Ring middleware is a higher-order function that takes a handler and returns a new handler.
;; It can modify requests before passing them to the handler, and/or modify responses
;; returned by the handler.

;; Simple middleware template:
(defn my-middleware [handler]
  (fn [request]
    ;; Process request before handler
    (let [response (handler request)]
      ;; Process response after handler
      response)))

;; ## Adding CORS (Cross-Origin Resource Sharing) middleware
;;
;; This middleware adds headers that allow cross-origin requests:

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization")))))

;; ## Adding request ID middleware
;;
;; This middleware adds a unique ID to each request for tracking purposes:

(defn wrap-request-id [handler]
  (fn [request]
    (let [request-id (str "req-" (System/currentTimeMillis) "-" (rand-int 10000))]
      (handler (assoc request :request-id request-id)))))

;; ## Adding timing middleware
;;
;; This middleware measures how long it takes to process each request:

(defn wrap-timing [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)]
      (assoc-in response [:headers "X-Response-Time"] (str duration "ms")))))

;; ## Adding authentication simulation middleware
;;
;; This middleware simulates checking for authentication:

(defn wrap-auth [handler]
  (fn [request]
    (if (= (:uri request) "/protected")
      (let [auth-header (get-in request [:headers "authorization"])]
        (if (= auth-header "Bearer secret-token")
          (handler (assoc request :user {:id 1 :name "authenticated-user"}))
          {:status 401 :headers {"Content-Type" "application/json"}
           :body "{\"error\": \"Unauthorized\"}"}))
      (handler request))))

;; ## Creating a handler to test our middleware
;;
;; Let's create a handler that responds to different routes:

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

;; ## Combining middleware
;;
;; Let's combine multiple middleware functions:

(def app-with-middleware
  (-> api-handler
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors))

;; Let's test our application with middleware:
(app-with-middleware {:request-method :get :uri "/" :headers {}})

;; Test protected endpoint without auth:
(app-with-middleware {:request-method :get :uri "/protected" :headers {}})

;; Test protected endpoint with auth:
(app-with-middleware {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}})

;; ## Creating a custom middleware that transforms the response body
;;
;; This middleware adds a timestamp to JSON responses:

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

;; ## Testing combined middleware stack
;;
;; Let's create an application with all our middleware:

(def full-app
  (-> api-handler
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors
      wrap-timestamp))

;; Test the full application:
(full-app {:request-method :get :uri "/" :headers {}})

(full-app {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}})

;; ## Creating a debugging middleware
;;
;; This middleware helps us debug what's happening in the request/response cycle:

(defn wrap-debug [handler]
  (fn [request]
    (println "DEBUG: Processing request to" (:uri request) "with method" (:request-method request))
    (println "DEBUG: Request headers:" (pr-str (:headers request)))
    (let [response (handler request)]
      (println "DEBUG: Responding with status" (:status response))
      (println "DEBUG: Response headers:" (pr-str (:headers response)))
      response)))

;; Let's create a debug version:
(def debug-app
  (-> api-handler
      wrap-debug
      wrap-auth
      wrap-request-id
      wrap-timing
      wrap-cors
      wrap-timestamp))

;; To see debug output, uncomment the following:
(comment
  (debug-app {:request-method :get :uri "/" :headers {}})
  (debug-app {:request-method :get :uri "/protected" :headers {"authorization" "Bearer secret-token"}}))

;; ## Summary
;;
;; In this notebook, we learned:
;; 1. How to create various types of middleware (CORS, request ID, timing, auth)
;; 2. How to combine middleware using the thread-first operator
;; 3. How middleware can modify both requests and responses
;; 4. How to create middleware that transforms response bodies
;; 5. How to create debugging middleware
;;
;; Next, we'll demonstrate middleware functionality more in-depth and see how they integrate.
