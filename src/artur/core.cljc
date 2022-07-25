(ns artur.core
  (:require
    [clojure.string :refer [join lower-case trim starts-with?]]
    [artur.i18n :as i18n]))

(defn- twiml [& strs]
  [:Response [:Message (join " " strs)]])

(defn- status-key [status]
  (keyword (str "status/" (name status))))

(defn respond [{{text :Body from :From} :params
                convo :conversation}]
  (let [t (partial i18n/translate (:lang convo :en))
        trimmed (trim text)
        text (lower-case trimmed)]
    (cond
      ;; Robot wishes to be helpful
      (= (t :help) text)
      {:body (twiml (t :help-text))
       :conversation convo}

      ;; Robot habla espanol
      (starts-with? trimmed "lang ")
      (let [lang (keyword (subs text 5))
            t (partial i18n/translate lang)]
        {:body (twiml "👍" (t :help-text))
         :conversation (assoc convo :lang lang)})

      (or (starts-with? trimmed "http://")
          (starts-with? trimmed "https://")
          (starts-with? trimmed "magnet:"))
      (let [url trimmed
            download (get-in convo [:state url])]
        (if download
          {:effects [[:check {:url url
                              :id (get-in convo [:state url :deluge-id])}]]}
          {:body (twiml (t :downloading))
           :conversation (assoc-in convo [:state url :status] :in-progress)
           :effects [[:download {:url url :from from}]]}))

      ;; Robot is confused
      :else
      {:body (twiml (t :sorry) (t :help-text))
       :conversation convo})))
