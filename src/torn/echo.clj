(ns echo-server
  (:import (java.net InetAddress DatagramPacket DatagramSocket)))

(defonce udp-server (atom nil))
(defonce state (atom []))

(def port 1111)

(defn localhost [] (.getLocalHost InetAddress))

(defn message [text]
  (DatagramPacket. (.getBytes text)
                   (.length text)
                   (localhost)
                   port))

(defn send-message [text]
  (.send @udp-server (message text)))

(defn create-udp-server []
  (DatagramSocket. port))

(defn start-udp-server []
  (reset! udp-server (create-udp-server)))

(defn stop-udp-server []
  (.close @udp-server)
  (reset! udp-server nil))

(defn empty-message [n]
  (new DatagramPacket (byte-array n) n))

(defn start-print-loop []
  (loop []
    (let [orig-packet (empty-message 1024)]
      (.receive @udp-server orig-packet)
      (.send @udp-server orig-packet)
      (let [data(.getData orig-packet)]
        ; so you can stop repl and play with messages
        (swap! state conj data)
        (println (String. (.getData orig-packet) "UTF-8"))))
    (recur)))
