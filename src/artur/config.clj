(ns artur.config
  (:require
    [config.core :as config]
    [mount.core :as mount :refer [defstate]]))

(defstate env
  :start (config/load-env))
