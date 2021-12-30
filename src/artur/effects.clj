(ns artur.effects)

(defmulti effect! first)

(defn wrap-effects [handler]
  (fn [req]
    (let [res (handler req)]
      (doseq [effect (:effects res)]
        (effect! effect))
      res)))
