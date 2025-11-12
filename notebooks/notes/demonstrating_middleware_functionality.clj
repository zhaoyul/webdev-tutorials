(ns notes.demonstrating-middleware-functionality
  (:require [nextjournal.clerk :as clerk]))

;; # Demonstrating Middleware Functionality
;;
;; This notebook demonstrates the actual functionality of middleware,
;; showing how data flows through the middleware chain and how each
;; piece of middleware transforms the request and response.

^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## Understanding the Middleware Chain
;;
;; Middleware functions are applied in a specific order, and each one 
;; can modify the request before passing it to the next middleware,
;; and the response after getting it from the next middleware.

;; To visualize this, let's create middleware that logs what happens at each step:

(defn wrap-logger-step [handler step-name]
  (fn [request]
    (println (str "  -> " step-name ": Processing request"))
    (let [response (handler request)]
      (println (str "  <- " step-name ": Processing response"))
      response)))

;; ## Creating a middleware chain with step logs
;;
;; Let's create a handler and a chain of middleware that we can trace:

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

;; As you can see from the output, the request flows:
;; 1. Middleware-1 receives request
;; 2. Middleware-2 receives request 
;; 3. Middleware-3 receives request
;; 4. Handler processes request
;; 5. Middleware-3 processes response
;; 6. Middleware-2 processes response
;; 7. Middleware-1 processes response

;; ## Demonstrating request modification
;;
;; Let's create middleware that modifies the request:

(defn wrap-request-modifier [handler]
  (fn [request]
    (println "  -> Request modifier: Adding custom header to request")
    (let [modified-request (assoc-in request [:headers "x-modified-by"] "request-modifier")]
      (handler modified-request))))

;; ## Demonstrating response modification
;;
;; Let's create middleware that modifies the response:

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

;; The flow is:
;; 1. Final Logger receives request
;; 2. Request modifier receives request (and adds header)
;; 3. Response modifier receives request
;; 4. Handler processes request
;; 5. Response modifier processes response (and adds header)
;; 6. Request modifier processes response
;; 7. Final Logger processes response

;; ## Practical example: Request body parsing middleware
;;
;; Let's create a real-world example with request body parsing:

(defn parse-json-body [request]
  "Parse JSON body if present and add as :parsed-body to request"
  (if-let [body (:body request)]
    (if (string? body)
      (try 
        (assoc request :parsed-body (clojure.data.json/read-str body))
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


;; ## Stateful middleware example: Session management
;;
;; Let's create middleware that simulates session management:

(def sessions (atom {}))  ; In real applications, you'd use a proper session store

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

;; ## Error handling middleware
;;
;; Let's create middleware that handles errors gracefully:

(defn wrap-error-handler [handler]
  (fn [request]
    (try 
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (str "{\"error\": \"Internal server error\", \"message\": \"" (.getMessage e) "\"}")}))))

;; Create a handler that might throw an error:
(defn risky-handler [request]
  (if (= (:uri request) "/error")
    (throw (Exception. "Something went wrong!"))
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body "{\"result\": \"success\"}"}))

;; Test error handling:
(def error-handling-app
  (-> risky-handler
      wrap-error-handler))

(error-handling-app {:request-method :get :uri "/ok" :headers {}})
(error-handling-app {:request-method :get :uri "/error" :headers {}})

;; ## Middleware composition patterns
;;
;; Let's look at how to organize middleware for different route groups:

(defn public-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"message\": \"Public content\"}"})

(defn private-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"message\": \"Private content for user\", \"user-id\": "
          (get-in request [:user :id] "unknown") "}"})

;; Authentication middleware
(defn wrap-authentication [handler]
  (fn [request]
    ;; Simple auth check - in real apps, this would be more sophisticated
    (if (= (get-in request [:headers "authorization"]) "Bearer valid-token")
      (handler (assoc request :user {:id 123 :name "Alice"}))
      {:status 401 :headers {"Content-Type" "application/json"}
       :body "{\"error\": \"Unauthorized\"}"})))

;; Apply different middleware to different routes
(defroutes app-routes
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

;; ## Summary
;;
;; In this notebook, we learned:
;; 1. How the middleware chain works step-by-step
;; 2. How to trace requests through middleware
;; 3. How to create request-modifying and response-modifying middleware
;; 4. Practical examples like JSON parsing, sessions, and error handling
;; 5. How to structure middleware for different route groups
;;
;; Next, we'll add Swagger support to document our API automatically.

;; Helper functions for middleware we used but didn't define in earlier notebooks
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
