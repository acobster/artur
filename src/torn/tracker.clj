(ns torn.tracker
  (:require
    [clojure.core.async :as async :refer [go]]
    [clojure.java.io :as io]
    [clojure.string :as string]

    [torn.torrent :as torrent])
  (:import
    [java.nio ByteBuffer]
    [java.net DatagramSocket DatagramPacket InetAddress URI URL]))

(defn- parse-url [url]
  (let [uri (URI. url)]
    {:protocol (.getScheme uri)
     :host (.getHost uri)
     :port (.getPort uri)}))

(defonce socket (atom (DatagramSocket. 1112)))

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
          (prn 'receiving...)
          (.receive socket recv-packet)
          (prn 'received (.getData recv-packet))
          (prn 'calling f)
          (f (.getData recv-packet) socket)
          (recur))))))

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

(defn- betw [buf start end]
  (String. (byte-array (map #(.get buf %) (range start end))) "utf-8"))

(defn- announce-url [torrent]
  (String. (:announce torrent) "utf-8"))

(defn- put-str [buf i s]
  (let [str-bytes (.getBytes s)
        indices (range i (+ i (count str-bytes)))]
    (doseq [[i b] (partition 2 (interleave indices str-bytes))]
      (.put buf i b))))

(defn- str->buffer [s len]
  (let [buf (ByteBuffer/allocate len)]
    (put-str buf 0 s)
    buf))

;; Versioned client-id
(def ^:private client-id "-TO0001-")

(defn- peer-id []
  (let [buf (str->buffer client-id 20)]
    (doseq [i [8 12 16]] (.putInt buf i (rand-int Integer/MAX_VALUE)))
    buf))

(defn- announce-req [connection-id torrent]
  (let [pid (peer-id)
        tid (tx-id)]
    [(doto (ByteBuffer/allocate 98)
       ;; // connection id
       ;; connId.copy(buf, 0);
       (.put connection-id)
       ;; // action
       ;; buf.writeUInt32BE(1, 8);
       ;; action
       (.putInt 8 1)
       ;; // transaction id
       ;; crypto.randomBytes(4).copy(buf, 12);
       (.putInt 12 tid)
       ;; torrentParser.infoHash(torrent).copy(buf, 16);
       ;; info hash
       (put-str 16 (torrent/info-hash torrent))
       ;; util.genId().copy(buf, 36);
       ;; peer ID
       (.putInt 36 (.getInt pid 0))
       (.putInt 40 (.getInt pid 4))
       (.putInt 44 (.getInt pid 8))
       (.putInt 48 (.getInt pid 12))
       (.putInt 52 (.getInt pid 16))
       ;; Buffer.alloc(8).copy(buf, 56);
       ;; downloaded
       ;; 56 -> already 0s
       ;; torrentParser.size(torrent).copy(buf, 64);
       ;; left
       (.putLong 64 (torrent/size torrent))
       ;; uploaded
       (.putLong 72 0)
       ;; buf.writeUInt32BE(0, 80);
       ;; event: 0 = none
       (.putInt 80 0)
       ;; buf.writeUInt32BE(0, 80);
       ;; ip address
       (.putInt 84 0)
       ;; crypto.randomBytes(4).copy(buf, 88);
       ;; key
       (.putInt 88 (rand-int Integer/MAX_VALUE))
       ;; buf.writeInt32BE(-1, 92);
       ;; num want
       (.putInt 92 -1)
       ;; buf.writeUInt16BE(port, 96);
       ;; port
       (.putShort 96 6888))
     tid]))

(defn- peer [buf offset])

(defmethod response 1 [buf]
  ;; Peers are 6 bytes each: 4 for IP addr and 2 for port.
  {:response/action :announce
   :response/timestamp (inst-ms (java.util.Date.))
   :response/transaction-id (.getInt buf 4)
   :response/interval (.getInt buf 8)
   :response/leechers (.getInt buf 12)
   :response/seeders (.getInt buf 16)
   :response/peers
   (set (reduce (fn [peers idx]
                  (let [ip-bytes (map #(bit-and 0xff (.get buf (+ idx %)))
                                      (range 0 4))
                        ip (string/join "." ip-bytes)
                        port (.getShort buf (+ idx 4))]
                    (if (= [0 0 0 0] ip-bytes)
                      (reduced peers)
                      (conj peers {:ip ip :port port}))))
                [] (range 20 (- (.limit buf) 4) 6)))})

(comment
  (defonce $res (atom nil))
  (filter (complement zero?) (.array @$res))
  (response @$res))

(defmethod on-response :announce
  [{:response/keys [transaction-id interval seeders leechers peers]} socket torrent]
  (prn 'response transaction-id interval (format "S/L: %d/%d" seeders leechers))
  (prn (count peers) 'peers peers))

(defn- packet [msg-buf url]
  (let [arr (.array msg-buf)
        len (count arr)
        {:keys [host port]} (parse-url url)
        addr (InetAddress/getByName host)]
    (DatagramPacket. arr len addr port)))

(defn- udp-send [socket message url]
  (prn 'SEND socket message url)
  (.send socket (packet message url)))

(defn get-peers [torrent socket f]
  (let [url (announce-url torrent)]
    (udp-send socket (conn-req) (announce-url torrent))))

(defmethod on-response :connect
  [{:response/keys [transaction-id connection-id]} socket torrent]
  (when-let [[req tid] (announce-req connection-id torrent)]
    (prn 'announce tid)
    (prn :peer-id (betw req 36 44) :hash (betw req 16 36) :port (.getShort req 96))
    (udp-send socket req (announce-url torrent))))

(comment
  (require '[clojure.repl :refer [doc]])

  (deref socket)

  ((juxt .getPort) (InetAddress/getByName "localhost:8000"))

  (-> (.getBytes "5:nrepl2:is7:awesomee" "UTF-8")
      ByteArrayInputStream.
      PushbackInputStream.
      ben/read-bencode)

  (def $torrent
    (torrent/parse
      "big-buck-bunny.5e7886d42a52ae66da4541d88882a04f9a34a649.torrent"))
  (type ($torrent :announce))
  (count ($torrent :announce))
  (String. ($torrent :announce) "utf-8")
  (announce-url $torrent)
  (torrent/size $torrent)
  (torrent/info-hash $torrent)

  (get-peers $torrent @socket #())

  ((juxt #(.getScheme %) #(.getHost %) #(.getPort %))
   (URI. (announce-url $torrent)))
  (parse-url (announce-url $torrent))


  (tx-id)
  (.array (peer-id))
  (String. (byte-array (subvec (vec (.array (peer-id))) 0 8)) "utf-8")

  (do
    (.close @socket)
    (reset! socket (DatagramSocket. 1112))
    (listen! @socket (fn [data sock]
                       (on-response (parse-response data) sock $torrent)))
    (udp-send @socket (conn-req) (announce-url $torrent)))

  (packet "hello there" (announce-url $torrent))

  (let [buf (ByteBuffer/allocate 18)]
    (doseq [[idx [ip1 ip2 ip3 ip4] port] [[0 [10 0 0 1] 8000]
                                          [6 [127 168 1 0] 9000]
                                          [12 [72 0 0 1] 5000]]]
      (.put buf (+ idx 0) (byte (- ip1 128)))
      (.put buf (+ idx 1) (- ip2 128))
      (.put buf (+ idx 2) (- ip3 128))
      (.put buf (+ idx 3) (- ip4 128))
      (.putShort buf (+ idx 4) port))
    {:bytes (.array buf)
     :buffer/len (.limit buf)
     :peers (reduce (fn [peers idx]
                      (let [ip-bytes (map #(+ (.get buf (+ idx %)) 128)
                                          (range 0 4))
                            port (.getShort buf (+ idx 4))]
                        (conj peers {:ip ip-bytes :port port})))
                    [] (range 0 (.limit buf) 6))})

  ;;
  )
