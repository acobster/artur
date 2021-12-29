(ns artur.xml-test
  (:require
    [artur.xml :as xml]
    [clojure.test :refer [deftest is are]]
    [kaocha.repl :as k]))

(deftest renders-xml
  (are
    [expected form] (= expected (xml/render form))

    nil nil
    nil []
    "" ""

    "<a>stuff</a>"
    [:a "stuff"]

    "<a><b>stuff</b></a>"
    [:a [:b "stuff"]]

    "<a><b><c>stuff</c></b></a>"
    [:a [:b [:c "stuff"]]]
    ))

(deftest expand-transforms-vectors-into-clojure-dot-xml-maps
  (are
    [expected form] (= expected (xml/expand form))

    "" nil
    "" []
    "" ""
    "abc" :abc

    {:tag :a :content ["keyword"]}
    [:a :keyword]

    {:tag :a :content ["stuff"]}
    [:a "stuff"]

    {:tag :a :content [{:tag :b :content ["stuff"]}]}
    [:a [:b "stuff"]]

    {:tag :a :content [{:tag :b :content [{:tag :c :content ["stuff"]}]}]}
    [:a [:b [:c "stuff"]]]

    {:tag :a
     :content [{:tag :b
                :content [{:tag :c
                           :content ["stuff"]}]}
               {:tag :bb
                :content [{:tag :cc
                           :content ["more stuff"]}
                           {:tag :dd
                           :content ["and more"]}]}]}
    [:a [:b [:c "stuff"]] [:bb [:cc "more stuff"] [:dd "and more"]]]))

(deftest wrap-xml-document-converts-response-body-to-xml-string
  (are
    [body v] (= body (let [handler (xml/wrap-xml-document identity)
                           res {:status 200
                                :body v}]
                       (-> res handler :body)))

    "<?xml version='1.0' encoding='UTF-8'?>\n<a>\nxyz\n</a>\n"
    [:a :xyz]

    "<?xml version='1.0' encoding='UTF-8'?>\n<a>\n<xyz>\n</xyz>\n</a>\n"
    [:a [:xyz]]

    "<?xml version='1.0' encoding='UTF-8'?>\n<a>\n<b>\n<c>\nxyz\n</c>\n</b>\n</a>\n"
    [:a [:b [:c :xyz]]]
    ))


(comment
  (k/run))
