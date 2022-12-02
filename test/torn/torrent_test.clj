(ns torn.torrent-test
  (:require
    [clojure.test :refer [deftest is]]
    [torn.torrent :as torrent]))

(deftest test-info-hash
  (is
    (= "5e7886d42a52ae66da4541d88882a04f9a34a649"
       (-> "big-buck-bunny.5e7886d42a52ae66da4541d88882a04f9a34a649.torrent"
           torrent/parse
           torrent/info-hash))))

(comment
  (require '[kaocha.repl :as k])
  (k/run))
