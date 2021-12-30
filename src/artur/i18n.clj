(ns artur.i18n)

(def i18n
  {:en {:sorry "Beep boop, I'm just a robot 🤖"
        :help-text "To get started, send me a torrent file URL."
        :help "help"}

   :es {:sorry "Lo siento, solo soy un robot 🤖"
        :help-text "Para comenzar, envíeme una URL de archivo torrent."
        :help "todo help"}})

;; TODO tongue https://github.com/tonsky/tongue/
(defn translate [lang k]
  (get-in i18n [lang k]))
