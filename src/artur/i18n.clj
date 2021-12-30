(ns artur.i18n)

(def i18n
  {:en {:sorry "Beep boop, I'm just a robot. 🤖"
        :help-text "To get started, send me a torrent file URL."}})

;; TODO tongue https://github.com/tonsky/tongue/
(defn translate [lang k]
  (get-in i18n [lang k]))
