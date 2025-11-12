(ns notes.starting-web-server
  (:require [nextjournal.clerk :as clerk]))

;; # Starting a Web Server with Clojure
;;
;; This notebook demonstrates how to start a basic web server in Clojure.
;; We'll explore the fundamentals of HTTP servers and how to set them up.

;; First, let's load the necessary libraries
^{::clerk/visibility {:code :hide :result :hide}}
(defn load-libraries []
  (require '[ring.adapter.jetty :as jetty])
  (require '[ring.util.response :as response]))

(load-libraries)

;; ## The basics: Creating a simple handler function
;;
;; In Ring (the standard Clojure web application interface), a handler is just a function
;; that takes a request map and returns a response map.

(defn simple-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<h1>Welcome to our Web Tutorial!</h1><p>This is a simple web server.</p>"})

;; Let's test our handler with a mock request:
(def mock-request {:request-method :get :uri "/" :headers {}})

(simple-handler mock-request)

;; ## Starting the server
;;
;; Now let's start a real server using Ring's Jetty adapter.
;; The server will run on port 3000.

(defn start-server []
  (jetty/run-jetty simple-handler {:port 3000 :join? false}))

;; To start the server, call the function:
(comment
  (def server (start-server))
  ;; To stop the server later:
  (.stop server)
  )

;; ## Understanding the request map
;;
;; When an HTTP request comes in, Ring provides a map with all the request details.
;; Let's create a handler that shows the structure of the request:

(defn request-inspector [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (str (with-out-str (clojure.pprint/pprint request)))})

;; This handler will print the entire request map as a response.
;; Try accessing different routes to see how the request map changes:

(defn request-info-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<h2>Request Information</h2>"
              "<p><strong>URI:</strong> " (:uri request) "</p>"
              "<p><strong>Method:</strong> " (name (:request-method request)) "</p>"
              "<p><strong>Headers:</strong> " (pr-str (:headers request)) "</p>"
              "<p><strong>Query String:</strong> " (:query-string request) "</p>")})

;; Let's see what the request info handler returns:
(request-info-handler {:request-method :get :uri "/hello" :headers {"host" "localhost:3000"} :query-string "name=clerk"})

;; ## Summary
;;
;; In this notebook, we learned:
;; 1. How to create basic Ring handler functions
;; 2. How requests are represented as maps in Ring
;; 3. How to start a web server using Jetty
;; 4. How to inspect and work with request data
;;
;; Next, we'll explore Ring middleware which allows us to enhance our handlers with additional functionality.

(comment
  (clerk/serve! {:port 4455 })

  (clerk/serve! {:port 4456 :watch-paths ["notebooks/notes"]})

  (clerk/show! 'notes.starting-web-server)
  )
