(ns artur.core-test
  (:require
    [clojure.test :refer [deftest is are]]
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
     :conversation {:state {}
                    :lang :en}}
    {:params {:Body "help"}
     :conversation {:state {}
                    :lang :en}}

    ;; ...   HALP   ...
    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state {}
                    :lang :en}}
    {:params {:Body "   help   "}
     :conversation {:state {}
                    :lang :en}}

    ;; Test case insensitivity
    {:body [:Response [:Message
                       "To get started, send me a torrent file URL."]]
     :conversation {:state {}
                    :lang :en}}
    {:params {:Body "   Help   "}
     :conversation {:state {}
                    :lang :en}}

    ;; Robot should understand other languages, too
    {:body [:Response [:Message
                       "Para comenzar, envíeme una URL de archivo torrent."]]
     :conversation {:state {}
                    :lang :es}}
    {:params {:Body "TODO help"}
     :conversation {:state {}
                    :lang :es}}

    ;; Change languages
    {:body [:Response [:Message
                       "👍 Para comenzar, envíeme una URL de archivo torrent."]]
     :conversation {:state {}
                    :lang :es}}
    {:params {:Body "lang es"}
     :conversation {:state {}
                    :lang :en}}

    ;; "Add movie https://..."
    {:body [:Response [:Message
                       "Downloading! Text the same URL again for an update."]]
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:status :in-progress}}
                    :lang :en}
     :effects [[:download {:url "http://example.com/starwars.torrent"
                           :path "/home/bob/movies"}]]}
    {:params {:Body "add movie http://example.com/starwars.torrent"
              :From "+12345556789"}
     :env {:target-dirs {"movie" "/home/bob/movies"
                         "show" "/home/bob/shows"}}
     :conversation {:state {}
                    :lang :en}}

    ;; "Add movie https://... TITLE"
    {:body [:Response [:Message
                       "Downloading! Text the same URL again for an update."]]
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:status :in-progress
                             :title "movie title"}}
                    :titles {"movie title" "http://example.com/starwars.torrent"}
                    :lang :en}
     :effects [[:download {:url "http://example.com/starwars.torrent"
                           :path "/home/bob/movies"
                           :title "movie title"}]]}
    {:params {:Body "Add movie http://example.com/starwars.torrent movie title"
              :From "+12345556789"}
     :env {:target-dirs {"movie" "/home/bob/movies"
                         "show" "/home/bob/shows"}}
     :conversation {:state {}
                    :lang :en}}

    ;; "Check TITLE"
    {:effects [[:check {:url "http://example.com/starwars.torrent"
                        :id "asdfqwerty"}]]}
    {:params {:Body "Check movie title"
              :From "+12345556789"}
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:deluge-id "asdfqwerty"
                             :status :in-progress}}
                    :titles {"movie title" "http://example.com/starwars.torrent"}
                    :lang :en}}

    ;; Trying to add a bad category
    {:body [:Response [:Message
                       (str "Didn't recognize category: xyz."
                            " Choose one of these: movie, show")]]}
    {:params {:Body "add xyz whatever"
              :From "+12345556789"}
     :env {:target-dirs {"movie" "/home/bob/movies"
                         "show" "/home/bob/shows"}}
     :conversation {:state {}
                    :lang :en}}

    ;; Just a URL to download...
    {:body [:Response [:Message
                       "Downloading! Text the same URL again for an update."]]
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:status :in-progress}}
                    :lang :en}
     :effects [[:download {:url "http://example.com/starwars.torrent"
                           :from "+12345556789"}]]}
    {:params {:Body "http://example.com/starwars.torrent"
              :From "+12345556789"}
     :conversation {:state {}
                    :lang :en}}

    ;; Text the same URL again for an update.
    {:effects [[:check {:url "http://example.com/starwars.torrent"
                        :id "asdfqwerty"}]]}
    {:params {:Body "http://example.com/starwars.torrent"
              :From "+12345556789"}
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:deluge-id "asdfqwerty"
                             :status :in-progress}}
                    :lang :en}}

    ;; Start a new download with one in progress.
    {:body [:Response [:Message
                       (str "Downloading!"
                            " Text the same URL again for an update.")]]
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:status :in-progress}
                            "http://example.com/another.torrent"
                            {:status :in-progress}}
                    :lang :en}
     :effects [[:download {:url "http://example.com/another.torrent"
                           :from "+12345556789"}]]}
    {:params {:Body "http://example.com/another.torrent"
              :From "+12345556789"}
     :conversation {:state {"http://example.com/starwars.torrent"
                            {:status :in-progress}}
                    :lang :en}}

    ))

(comment
  (require '[kaocha.repl :as k])
  (k/run))
