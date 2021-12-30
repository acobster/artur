(ns artur.core-test
  (:require
    [clojure.test :refer [deftest is are]]
    [kaocha.repl :as k]
    [artur.core :as core]))

(deftest test-state-transitions
  (are
    [res req] (= res (core/respond req))

    {:body [:Response
            [:Message
             (str "Beep boop, I'm just a robot. 🤖"
                  " To get started, send me a torrent file URL.")]]
     :conversation :does-not-change}
    {:params {:Body "something"}
     :conversation :does-not-change}

    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state :waiting-for-url
                    :lang :en}}
    {:params {:Body "help"}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state :waiting-for-url
                    :lang :en}}
    {:params {:Body "   help   "}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ))

(comment
  (k/run))
