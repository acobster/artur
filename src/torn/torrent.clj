;; Torrent file parsing helpers
(ns torn.torrent
  (:require
    [clojure.java.io :as io]
    [nrepl.bencode :as ben]
    [clojure.walk :as walk])
  (:import
    [java.io PushbackInputStream ByteArrayOutputStream]
    [java.security MessageDigest]))

(defn parse [path]
  (with-open [in (io/input-stream (io/resource path))]
    (-> in PushbackInputStream. ben/read-bencode walk/keywordize-keys)))

(defn info [torrent]
  (get torrent :info))

(defn size [torrent]
  (let [files (:files (info torrent))]
    (if files
      (reduce + (map :length files))
      (:length (info torrent)))))

(defn- bencode-str [data]
  (-> (doto (ByteArrayOutputStream.)
        (ben/write-bencode data))
      .toString))

(comment
  ;; Copied from Snark:
  ;; https://github.com/akerigan/born-again-snark/blob/0e325a6457727d353b106cb106aa528fecc2216d/src/main/java/org/torrent/basnark/storage/builder/TorrentInfoBuilder.java#L39
  (def md (MessageDigest/getInstance "SHA1"))
  (.reset md)
  (def out (ByteArrayOutputStream.))
  (def torrent (parse "big-buck-bunny.5e7886d42a52ae66da4541d88882a04f9a34a649.torrent"))
  (keys torrent)
  (let [{:keys [info]} torrent]
    (ben/write-bencode out info))
  (sha1 (.toByteArray out))
  (= "fa26be19de6bff93f70bc2308434e4a440bbad02" (sha1 "this is a test")))

(defn- sha1 [data]
  (->> (.digest (MessageDigest/getInstance "SHA1") data)
       (map #(.substring
               (Integer/toString
                 (+ (bit-and % 0xff) 0x100) 16) 1))
       (apply str)))

(defn info-hash [torrent]
  (-> (doto (ByteArrayOutputStream.)
        (ben/write-bencode (info torrent)))
      (.toByteArray)
      sha1))

(defn announce-url [torrent]
  (String. (torrent :announce) "utf-8"))
