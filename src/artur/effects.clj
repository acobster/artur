(ns artur.effects
  (:require
    [artur.config :refer [env]]))

(defmulti effect! first)

(defmethod effect! :download [[_ download]]
  (let [url (:url download)
        file (last (clojure.string/split url #"/"))
        dir (:download-dir env "./downloads")
        file-path (str dir java.io.File/separator file)
        torrent (try
                  (slurp url)
                  (catch java.io.FileNotFoundException e
                    (println "Error downloading torrent file:" e)))]
    (when torrent
      (when (.mkdir (java.io.File. dir))
        (println "created directory " dir))
      (println (format "downloading torrent at %s to %s (%d B)" url file-path (count torrent)))
      (spit file-path torrent))))

(defn wrap-effects [handler]
  (fn [req]
    (let [res (handler req)]
      (doseq [effect (:effects res)]
        (try
          (effect! effect)
          (catch Throwable e
            ;; TODO actual error handling
            (prn 'ERROR e))))
      res)))
