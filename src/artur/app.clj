(ns artur.app
  (:gen-class)
  (:require
    [config.core :as config]
    [mount.core :as mount :refer [defstate]]
    [org.httpkit.server :as http]
    [ring.middleware.defaults :as ring]

    [artur.core :as core]
    [artur.effects :as fx]
    [artur.store :as store]
    [artur.xml :as xml]))

(defstate env
  :start (config/load-env))

(defn handler [req]
  (merge
    {:status 200
     :headers {"content-type" "application/xml"}}
    (core/respond req)))

(def app
  (-> handler
      store/wrap-conversation
      fx/wrap-effects
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
