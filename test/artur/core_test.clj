(ns artur.core-test
  (:require
    [clojure.test :refer [deftest is are]]
    [kaocha.repl :as k]
    [artur.core :as core]))

(deftest test-state-transitions
  (are
    [res req] (= res (core/respond req))

    ;; Robot is confused
    {:body [:Response
            [:Message
             (str "Beep boop, I'm just a robot 🤖"
                  " To get started, send me a torrent file URL.")]]
     :conversation :does-not-change}
    {:params {:Body "something"}
     :conversation :does-not-change}

    ;; HALP
    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state :waiting-for-url
                    :lang :en}}
    {:params {:Body "help"}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ;; ...   HALP   ...
    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state :waiting-for-url
                    :lang :en}}
    {:params {:Body "   help   "}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ;; Test case insensitivity
    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state :waiting-for-url
                    :lang :en}}
    {:params {:Body "   Help   "}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ;; Robot should understand other languages, too
    {:body [:Response [:Message
                       "Para comenzar, envíeme una URL de archivo torrent."]]
     :conversation {:state :waiting-for-url
                    :lang :es}}
    {:params {:Body "TODO help"}
     :conversation {:state :waiting-for-url
                    :lang :es}}

    ;; Change languages
    {:body [:Response [:Message
                       "👍 Para comenzar, envíeme una URL de archivo torrent."]]
     :conversation {:state :waiting-for-url
                    :lang :es}}
    {:params {:Body "lang es"}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ;; Finally, an actual URL to download...
    {:body [:Response [:Message
                       (str "Downloading! I'll notify you when it's finished."
                            " Text \"update\" to see progress.")]]
     :conversation {:state :in-progress
                    :lang :en}
     :effects [[:download {:url "http://example.com/starwars.torrent"
                           :from "+12345556789"}]]}
    {:params {:Body "http://example.com/starwars.torrent"
              :From "+12345556789"}
     :conversation {:state :waiting-for-url
                    :lang :en}}

    ))

(comment
  (k/run))
