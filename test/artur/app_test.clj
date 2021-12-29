(ns artur.app-test
  (:require
    [clojure.test :refer [deftest is are]]
    [kaocha.repl :as k]))

(deftest does-a-thing
  (is (true? true)))

(comment
  (k/run *ns*))
