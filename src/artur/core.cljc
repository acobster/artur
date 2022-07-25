(ns artur.core
  (:require
    [clojure.core.match :refer [match]]
    [clojure.string :refer [join lower-case trim split starts-with?]]
    [artur.i18n :as i18n]))

(defn- twiml [& strs]
  [:Response [:Message (join " " strs)]])

(defn- status-key [status]
  (keyword (str "status/" (name status))))

(defn respond [{{text :Body from :From} :params
                convo :conversation
                env :env}]
  (let [t (partial i18n/translate (:lang convo :en))
        trimmed (trim text)
        text (lower-case trimmed)
        cmd (split text #" +")]
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

      ;; Just a torrent URL means add OR check.
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

      :else
      (match cmd
        ["add" & _]
        (let [[_ category url] cmd
              path (get-in env [:target-dirs category])]
          (if path
            {:body (twiml (t :downloading))
             :conversation (assoc-in convo [:state url :status] :in-progress)
             :effects [[:download {:url url :path path}]]}
            {:body (twiml (format
                            (t :error/unrecognized-category)
                            category
                            (->> env :target-dirs keys sort (join ", "))))}))

        :else
        {:body (twiml (t :sorry) (t :help-text))
         :conversation convo}))))
