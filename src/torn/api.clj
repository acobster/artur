(ns torn.api
  (:require
    [clojure.core.async :as async :refer [go]]
    [clojure.java.io :as io]
    [nrepl.bencode :as ben])
  (:import
    [java.io PushbackInputStream]
    [java.nio ByteBuffer]
    [java.net DatagramSocket DatagramPacket InetAddress URL]))

(defn- parse [path]
  (with-open [in (io/input-stream (io/resource path))]
    (-> in PushbackInputStream. ben/read-bencode)))

(defn- parse-url [url]
  (let [url* (URL. url)]
    {:protocol (.getProtocol url*)
     :host (.getHost url*)
     :port (.getPort url*)}))

(defonce socket (atom (DatagramSocket. 1112)))

(defn- open-socket! [port]
  (reset! socket (DatagramSocket. port)))

(defn- empty-message [n]
  (DatagramPacket. (byte-array n) n))

(defn- response-action [buf]
  (.getInt buf 0))

(defmulti response response-action)

(defmethod response 0 [buf]
  {:response/action :connect
   :response/transaction-id (.getInt buf 4)
   :response/connection-id (ByteBuffer/wrap (.array buf) 8 8)})

(defn- parse-response [data]
  (response (ByteBuffer/wrap data)))

(defn- listen! [socket f]
  (go
    (loop []
      (when (and socket (not (.isClosed socket)))
        (let [recv-packet (empty-message 1024)]
          (.receive socket recv-packet)
          (f (.getData recv-packet) socket))
        (recur)))))

(def tx-ids (cycle (range 1000 Integer/MAX_VALUE)))
(defonce tx-idx (atom 0))
(defn- tx-id []
  (let [id (nth tx-ids @tx-idx)]
    (swap! tx-idx inc)
    id))

(def on-response nil)
(defmulti on-response (fn [res _ _]
                        (:response/action res)))

(defn- conn-req []
  (doto (ByteBuffer/allocate 16)
    (.putLong 0 0x41727101980)
    (.putInt 8 0)
    (.putInt 12 (tx-id))))

(defn- announce-req [connection-id torrent]
  {:accounce! 'TODO
   :connection-id connection-id
   :torrent torrent})

(defn- announce-url [torrent]
  (String. (torrent "announce") "utf-8"))

(defmethod on-response :connect
  [{:response/keys [transaction-id connection-id]} socket torrent]
  (let [req (announce-req connection-id torrent)]
    (prn #_req (announce-url torrent))
    #_
    (udp-send socket req (announce-url torrent))))

(defmethod on-response :announce
  [_ _ _]
  (prn 'TODO))

(defn- packet [msg-buf url]
  (let [arr (.array msg-buf)
        len (count arr)
        {:keys [host port]} (parse-url url)
        addr (InetAddress/getByName host)]
    (DatagramPacket. arr len addr port)))

(defn- udp-send [socket message url]
  (.send socket (packet message url)))

(defn get-peers [torrent socket f]
  (let [url (announce-url torrent)]
    [socket url]
    (udp-send socket (conn-req) (announce-url torrent))))

(comment
  (require '[clojure.repl :refer [doc]])

  ((juxt .getPort) (InetAddress/getByName "localhost:8000"))

  (-> (.getBytes "5:nrepl2:is7:awesomee" "UTF-8")
      ByteArrayInputStream.
      PushbackInputStream.
      ben/read-bencode)

  (with-open [in (io/input-stream (io/resource "BigBuckBunny_124_archive.torrent"))]
    (-> in PushbackInputStream. ben/read-bencode (get "announce") (String. "utf-8")))

  (def $torrent (parse "BigBuckBunny_124_archive.torrent"))
  (type ($torrent "announce"))
  (count ($torrent "announce"))
  (String. ($torrent "announce") "utf-8")
  (announce-url $torrent)

  ((juxt #(.getProtocol %) #(.getHost %) #(.getPort %))
   (URL. (announce-url $torrent)))
  (parse-url (announce-url $torrent))

  (defonce $resp (atom nil))

  {:action (.getInt (ByteBuffer/wrap @$resp) 0)
   :transaction-id (.getInt (ByteBuffer/wrap @$resp) 4)
   :connection-id (ByteBuffer/wrap @$resp 8 8)}

  (do
    (.close @socket)
    (open-socket! 1112)
    (listen! @socket (fn [data sock]
                       (on-response (parse-response data) sock $torrent)))
    (get-peers $torrent @socket #()))
  (parse-response (deref $resp))

  (packet "hello there" (announce-url $torrent))

  ;;
  )
