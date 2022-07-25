(ns artur.effects
  (:require
    [clojure.java.io :refer [file]]
    [clojure.java.shell :refer [sh]]
    [clojure.string :as string :refer [join lower-case split trim]]
    [artur.config :refer [env]]
    [artur.i18n :as i18n]
    [artur.store :as store]))

(defmulti effect! (fn [effect _]
                    (first effect)))

(comment
  (sh "deluge-console" "add" "http://example.com/xxx.torrent"))

(defn- url->infohash [url]
  (string/replace (lower-case (.getName (file url))) #"\.torrent$" ""))

;; TODO maybe use this somehow?
(defn- parse-deluge-info [raw]
  (let [lines (split raw #"\n")
        ;; TODO maybe others
        info-keys {"Name" :deluge-name
                   "ID" :deluge-id}
        info (into {} (map (fn [line]
                             (let [[k & vs] (split (trim line) #": +")
                                   kw (get info-keys k)]
                               (when kw
                                 [kw (join " " vs)])))
                           lines))]
    info))

(comment
  (url->infohash "https://itorrents.org/torrent/C7DBFC4CE08A90207517338D094A49054F0491D7.torrent")
  (parse-deluge-info "Name: The Silence Of The Lambs (1991) [1080p]
    ID: b6ec2a4507b13ade6bc4a1fac5b39ca25888b326
    State: Seeding Up Speed: 0.0 KiB/s
    Seeds: 0 (209) Peers: 1 (16) Availability: 0.00
    Size: 1.5 GiB/1.5 GiB Ratio: 0.152
    Seed time: 0 days 02:04:48 Active: 0 days 02:23:29
    Tracker status: openbittorrent.com: Announce OK"))

(defmethod effect! :download
  [[_ {:keys [url path title]}] {convo :conversation :as res}]
  (let [args (if (seq path) ["add" "--path" path url] ["add" url])
        t (partial i18n/translate (:lang convo :en))
        {:keys [exit err]} (apply sh "deluge-console" args)]
    (if (seq err)
      (throw (ex-info "Could not add torrent"
                      {:code :could-not-download
                       :message err
                       :exit exit}))
      (if-let [infohash (url->infohash url)]
        (-> res
            (assoc-in [:conversation :state url :deluge-id] infohash)
            (assoc :body [:Response
                          [:Message
                           (format (t :downloading)
                                   (str "\"" (t :cmd/check) " " title "\""))]]))
        res))))

(defmethod effect! :check [[_ {:keys [id] :as x}] res]
  (let [{:keys [exit err out]} (sh "deluge-console" "info" id)]
    (assoc res :body [:Response
                      [:Message
                       out]])))

(defmulti handle-error (fn [e _ _] (:code (ex-data e))))

(defmethod handle-error :default
  [ex effect {convo :conversation :as res}]
  (let [t (partial i18n/translate (:lang convo :en))]
    (assoc res :body [:Response [:Message (t :error/generic)]])))

(defmethod handle-error :could-not-download
  [_ [_ {:keys [url]}] {convo :conversation :as res}]
  (let [t (partial i18n/translate (:lang convo :en))]
    (-> res
        (assoc :body [:Response
                      [:Message
                       (t :error/could-not-download)]])
        (update-in [:conversation :state] dissoc url))))

(defn- try-effect [effect res]
  (try
    (effect! effect res)
    (catch clojure.lang.ExceptionInfo ex
      (handle-error ex effect res))
    (catch Throwable ex
      (handle-error {:code :uncaught :exception ex} effect res))))

(defn wrap-effects [handler]
  (fn -wrap-effects [req]
    (let [res (handler req)]
      (loop [res res [effect & effects] (:effects res)]
        (if effect
          (recur (try-effect effect res) effects)
          res)))))
