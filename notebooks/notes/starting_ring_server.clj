(ns notes.starting-ring-server
  (:require [nextjournal.clerk :as clerk]))

;; # Starting a Ring Server
;;
;; This notebook demonstrates how to create a more sophisticated Ring server
;; with routing and better response handling.

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response])
  (require '[ring.middleware.params :as params])
  (require '[ring.middleware.json :as json]))

(load-libraries)

;; ## Creating a more complex handler with routing
;;
;; Let's build a handler that can respond differently based on the URI:

(defn route-handler [request]
  (case (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "text/html"}
         :body "<h1>Home Page</h1><p>Welcome to our web service!</p><ul><li><a href=\"/api/users\">Users API</a></li><li><a href=\"/api/status\">Status API</a></li></ul>"}
    "/api/users" {:status 200
                  :headers {"Content-Type" "application/json"}
                  :body "[{\"id\": 1, \"name\": \"Alice\"}, {\"id\": 2, \"name\": \"Bob\"}]"}
    "/api/status" {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body "{\"status\": \"running\", \"version\": \"1.0.0\"}"}
    {:status 404
     :headers {"Content-Type" "text/html"}
     :body "<h1>Not Found</h1><p>The requested resource was not found.</p>"}))

;; Let's test our route handler:
(route-handler {:request-method :get :uri "/" :headers {}})

(route-handler {:request-method :get :uri "/api/users" :headers {}})

(route-handler {:request-method :get :uri "/nonexistent" :headers {}})

;; ## Using Ring middleware
;;
;; Ring middleware allows us to modify requests and responses.
;; Let's add some common middleware to our handler:

(defn wrap-logger [handler]
  "Middleware that logs requests"
  (fn [request]
    (let [start-time (System/nanoTime)
          response (handler request)
          duration (/ (- (System/nanoTime) start-time) 1000000.0)]
      (println (str "Request: " (:request-method request) " " (:uri request) 
                    ", Status: " (:status response) 
                    ", Duration: " (format "%.2f" duration) "ms"))
      response)))

(defn wrap-content-type [handler]
  "Middleware that adds default content type if not present"
  (fn [request]
    (let [response (handler request)]
      (if (nil? (get-in response [:headers "Content-Type"]))
        (assoc-in response [:headers "Content-Type"] "text/plain")
        response))))

;; Let's compose our middleware with our handler:
(def app
  (-> route-handler
      wrap-logger
      wrap-content-type))

;; Test the composed application:
(app {:request-method :get :uri "/" :headers {}})

;; ## Serving JSON responses properly
;;
;; Let's enhance our handler to properly serve JSON using Ring middleware:

(defn json-handler [request]
  (case (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "application/json"}
         :body {:message "Welcome to the API", :version "1.0.0"}}
    "/api/users" {:status 200
                  :headers {"Content-Type" "application/json"}
                  :body {:users [{:id 1 :name "Alice"} {:id 2 :name "Bob"}]}}
    "/api/status" {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body {:status "running" :version "1.0.0"}}
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body {:error "Not Found"}}))

;; ## Creating a proper JSON response with serialization
;;
;; Let's create a helper function to properly serialize JSON responses:

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (clojure.data.json/write-str data)})

;; Now let's create a better handler that uses this helper:
(defn better-json-handler [request]
  (case (:uri request)
    "/" (json-response {:message "Welcome to the API", :version "1.0.0"})
    "/api/users" (json-response {:users [{:id 1 :name "Alice"} {:id 2 :name "Bob"}]})
    "/api/status" (json-response {:status "running" :version "1.0.0"})
    (json-response {:error "Not Found"} 404)))

;; Test the better JSON handler:
(better-json-handler {:request-method :get :uri "/" :headers {}})

;; ## Starting a server with middleware stack
;;
;; Now let's start a complete server with a middleware stack:

(defn start-ring-server []
  (require '[ring.middleware.json :refer [wrap-json-response]])
  
  (let [full-app
        (-> better-json-handler
            (wrap-json-response {:pretty true})
            wrap-logger)]
    (jetty/run-jetty full-app {:port 3001 :join? false})))

;; To start the server, you would call:
(comment
  (def server (start-ring-server))
  ;; To stop the server later:
  ;; (.stop server)
  )

;; ## Summary
;;
;; In this notebook, we learned:
;; 1. How to create route-based handlers
;; 2. How to write custom middleware functions
;; 3. How to compose middleware with handlers using the thread-first macro
;; 4. How to properly handle JSON responses
;; 5. How to start a server with a full middleware stack
;;
;; Next, we'll explore how to add and use various middleware to enhance our web service.
