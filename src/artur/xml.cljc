;; Mini DSL for hiccup-style XML vectors.
(ns artur.xml
  (:require
    [clojure.xml :as xml]
    [clojure.string :refer [join]]))

(defn render [form]
  (cond
    (string? form) form
    (seq form) (let [[tag & contents] form
                      tag (name tag)]
                 (str "<" tag ">"
                      (join "" (map render contents))
                      "</" tag ">"))))

(defn expand [form]
  (cond
    (sequential? form)
    (let [[tag & contents] form]
      (if tag
        {:tag tag :content (mapv expand contents)}
        ""))
    (keyword? form) (name form)
    :else (str form)))

(comment
  ;; NullPointerException
  (xml/emit [])
  (xml/emit nil)
  (xml/emit :kw)
  (xml/emit {})

  ;; empty
  (xml/emit "")

  (xml/emit {:tag :hi})
  (xml/emit {:tag :hi :content []})

  ;; content must be strings
  (xml/emit {:tag :xyz :content ["abc" "qwerty"]}))

(defn document [form]
  (let [m (expand form)]
    (with-out-str (xml/emit m))))

(defn wrap-xml-document [handler]
  (fn [req]
    (update (handler req) :body document)))

(comment
  (xml/emit (expand [:a :xyz]))
  (document [:a :xyz]))
