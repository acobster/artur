(ns torn.torrent-test
  (:require
    [clojure.test :refer [deftest is]]
    [torn.torrent :as torrent]))

(deftest test-info-hash
  (is
    (= "5e7886d42a52ae66da4541d88882a04f9a34a649"
       (-> "big-buck-bunny.5e7886d42a52ae66da4541d88882a04f9a34a649.torrent"
           torrent/parse
           torrent/info-hash))
    (= "fcf8424bc3006e21678eb4b3389bb1bc6bca4ef6"
       (-> "nixos-minimal.fcf8424bc3006e21678eb4b3389bb1bc6bca4ef6.torrent"
           torrent/parse
           torrent/info-hash))))

(comment
  (require '[kaocha.repl :as k])
  (k/run))
