(ns artur.i18n)

(def i18n
  {:en {:sorry "Beep boop, I'm just a robot 🤖"
        :help-text "To get started, send me a torrent file URL."
        :help "help"
        :downloading (str "Downloading! I'll notify you when it's finished."
                          " Text the same URL again for an update.")

        :status/in-progress "In progress"

        :error/could-not-download "Oops! I couldn't download that torrent. Is the URL correct?"}

   :es {:sorry "Lo siento, solo soy un robot 🤖"
        :help-text "Para comenzar, envíeme una URL de archivo torrent."
        :help "todo help"
        :downloading "TODO downloading"

        :status/in-progress "TODO in progress"

        :error/could-not-download "TODO error could-not-download"}})

;; TODO tongue https://github.com/tonsky/tongue/
(defn translate [lang k]
  (get-in i18n [lang k]))
