(ns artur.core
  (:require
    [clojure.string :refer [join lower-case trim starts-with?]]
    [artur.i18n :as i18n]))

(defn- twiml [& strs]
  [:Response [:Message (join " " strs)]])

(defn respond [{{text :Body} :params
                convo :conversation}]
  (let [t (partial i18n/translate (:lang convo :en))
        text (lower-case (trim text))]
    (cond
      ;; Robot wishes to be helpful
      (= (t :help) text)
      {:body (twiml (t :help-text))
       :conversation convo}

      ;; Robot habla espanol
      (starts-with? text "lang ")
      (let [lang (keyword (subs text 5))
            t (partial i18n/translate lang)]
        {:body (twiml "👍" (t :help-text))
         :conversation (assoc convo :lang lang)})

      ;; Robot is confused
      :else
      {:body (twiml (t :sorry) (t :help-text))
       :conversation convo})))
