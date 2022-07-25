(ns artur.effects
  (:require
    [artur.config :refer [env]]
    [artur.i18n :as i18n]))

(defmulti effect! first)

(defmethod effect! :download [[_ {:keys [url]}]]
  (let [file (last (clojure.string/split url #"/"))
        dir (:download-dir env "./downloads")
        file-path (str dir java.io.File/separator file)
        torrent (try
                  (slurp url)
                  (catch java.io.FileNotFoundException e
                    (throw (ex-info "Couldn't download torrent file"
                                    {:code :could-not-download}
                                    e))))]
    (when torrent
      (when (.mkdir (java.io.File. dir))
        (println "created directory " dir))
      (println (format "downloading torrent at %s to %s (%d B)" url file-path (count torrent)))
      (spit file-path torrent)))
  nil)

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
    (effect! effect)
    res
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
