(ns rc.web-tutorial.server
  (:require [muuntaja-content-negotiation :refer [complete-api-app]]
            [ring.adapter.jetty :as jetty]))

(defn start-server
  "Start the content negotiation demo server on port 3000"
  []
  (jetty/run-jetty complete-api-app {:port 3000 :join? true}))