(ns artur.core
  (:require
    [clojure.string :refer [join trim]]
    [artur.i18n :as i18n]))

(defn- twiml [& strs]
  [:Response [:Message (join " " strs)]])

(defn respond [{{text :Body} :params
                convo :conversation}]
  (let [t (partial i18n/translate (:lang convo :en))
        text (trim text)]
    (cond
      ;; Robot wishes to be helpful
      (= "help" text)
      {:body (twiml (t :help-text))
       :conversation convo}

      ;; Robot is confused
      :else
      {:body (twiml (t :sorry) (t :help-text))
       :conversation convo}
      )))
