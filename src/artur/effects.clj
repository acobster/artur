(ns artur.effects
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.string :refer [trim]]
    [artur.config :refer [env]]
    [artur.i18n :as i18n]
    [artur.store :as store]))

(defmulti effect! (fn [effect _]
                    (first effect)))

(comment
  (sh "deluge-console" "add" "http://example.com/xxx.torrent"))

(defmethod effect! :download [[_ {:keys [url]}] res]
  (let [{:keys [exit err out]} (sh "deluge-console" "add" url)]
    (when (seq err)
      (throw (ex-info "Could not add torrent"
                      {:code :could-not-download
                       :message err
                       :exit exit}))))
  res)

(defmethod effect! :check [[_ {:keys [id]}] res]
  (let [{:keys [exit err out]} (sh "deluge-console" "info" id)]
    (assoc res :body [:Response
                      [:Message
                       out]])))

(defmulti handle-error (fn [e _ _] (:code (ex-data e))))

(defmethod handle-error :could-not-download
  [_ [_ {:keys [url]}] {convo :conversation :as res}]
  (let [t (partial i18n/translate (:lang convo :en))]
    (-> res
        (assoc :body [:Response
                      [:Message
                       (t :error/could-not-download)]])
        (update-in [:conversation :state] dissoc url))))

(defn- try-effect [effect res]
  (try
    (effect! effect res)
    (catch clojure.lang.ExceptionInfo ex
      (handle-error ex effect res))
    (catch Throwable ex
      (handle-error {:code :uncaught :exception ex} effect res))))

(defn wrap-effects [handler]
  (fn -wrap-effects [req]
    (let [res (handler req)]
      (loop [res res [effect & effects] (:effects res)]
        (if effect
          (recur (try-effect effect res) effects)
          res)))))
