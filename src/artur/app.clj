(ns artur.app
  (:gen-class)
  (:require
    [config.core :as config]
    [mount.core :as mount :refer [defstate]]
    [org.httpkit.server :as http]
    [reitit.core :as reitit]
    [ring.middleware.defaults :as ring]

    [artur.xml :as xml]))

(defstate env
  :start (config/load-env))

(defn handler [req]
  (prn req)
  {:status 200
   :headers {"content-type" "application/xml"}
   :body [:Response [:Message "body text"]]})

(def app
  (-> handler
      xml/wrap-xml-document
      (ring/wrap-defaults
        (assoc ring/api-defaults
               ;; NOTE: params are already urlencoded and keywordized.
               :cookies false
               :security {:frame-options :deny}))))

(defonce stop-server (atom nil))
(defstate http-server
  :start (reset! stop-server (http/run-server
                               #'app
                               {:port (:port env 3000)}))
  :stop (when (fn? @stop-server) (@stop-server)))

(defn -main [& _]
  (mount/start))

(comment
  (do
    (mount/stop)
    (mount/start))

  (slurp "http://localhost:3003")

  (require '[kaocha.repl :as k])
  (k/run :unit))
