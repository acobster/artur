(ns artur.i18n)

(def i18n
  {:en {:sorry "Beep boop, I'm just a robot 🤖"
        :help-text "To get started, send me a torrent file URL."
        :help "help"
        :downloading "Downloading! Text %s for an update."
        :same-url "the same URL again"

        :status/in-progress "In progress"

        :cmd/check "check"

        :error/generic "Oh no, an error happened! 😨"
        :error/could-not-download "Oops! I couldn't download that torrent. Is the URL correct?"
        :error/unrecognized-category "Didn't recognize category: %s. Choose one of these: %s"
        }

   :es {:sorry "Lo siento, solo soy un robot 🤖"
        :help-text "Para comenzar, envíeme una URL de archivo torrent."
        :help "todo help"
        :downloading "TODO downloading"
        :same-url "the same URL again"

        :status/in-progress "TODO in progress"

        :cmd/check "TODO check"

        :error/generic "TODO ERROR 😨"
        :error/could-not-download "TODO error could-not-download"
        :error/unrecognized-category "TODO Didn't recognize category"
        }})

;; TODO tongue https://github.com/tonsky/tongue/
(defn translate [lang k]
  (get-in i18n [lang k]))
