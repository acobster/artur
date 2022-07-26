;; In-memory storage layer, backed by a flat file.
(ns artur.store
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [mount.core :as mount :refer [defstate]]

    [artur.config :refer [env]]))

(defonce ^:private state (atom {:conversations {}}))

(defstate load-persisted-state
  :start (let [persisted (some-> (:state-file env "resources/state.edn")
                                 io/resource
                                 slurp
                                 edn/read-string)]
           (swap! state merge (or persisted {}))))

(comment
  (reset! state {:conversations {}})
  (with-open [w (io/writer "resources/state.edn")]
    (.write w "{:conversations {}}")
    (.flush w))
  (deref state))

(defn wrap-conversation
  "Wraps handler in a fn that closes around the state atom, updating the
  corresponding conversation state with the value of (:conversation res)"
  [handler]
  (fn [req]
    (let [from (:From (:params req))
          convo (get-in @state [:conversations from])
          {updated :conversation :as res}
          (handler (assoc req :conversation convo))]
      ;; NOTE: as long as you're not doing something weird like sending
      ;; multiple concurrent requests per phone number, this is totally
      ;; thread safe.
      (when updated
        (swap! state assoc-in [:conversations from] updated)
        (with-open [w (io/writer (:state-file env "resources/state.edn"))]
          (.write w (prn-str @state))
          (.flush w)))
      res)))
