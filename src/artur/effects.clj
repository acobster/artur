(ns artur.effects)

(defmulti effect! first)

(defmethod effect! :download [[_ download]]
  (let [url (:url download)
        file (last (clojure.string/split url #"/"))]
    (spit file (slurp url))))

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
