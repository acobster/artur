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

(defn- sha1 [data]
  (->> (.digest (MessageDigest/getInstance "sha1") (.getBytes data))
       #_
       (map #(+ (bit-and % 0xff) 0x100))
       (map #(.substring
               (Integer/toString
                 (+ (bit-and % 0xff) 0x100) 16) 1))
       (apply str)))

(defn info-hash [torrent]
  (-> (doto (ByteArrayOutputStream.)
        (ben/write-bencode (info torrent)))
      .toString
      sha1))

(defn announce-url [torrent]
  (String. (torrent :announce) "utf-8"))


(comment
  (with-open [in (io/input-stream (io/resource "BigBuckBunny_124_archive.torrent"))]
    (-> in PushbackInputStream. ben/read-bencode (get "announce") (String. "utf-8"))))
