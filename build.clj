;; Build with:
;; clojure -T:build uberjar
(ns build
  (:require
    [clojure.tools.build.api :as build]))

(defn clean [_]
  (println "Deleting ./target...")
  (build/delete {:path "target"}))

(defn uberjar [_]
  (clean nil)
  (let [class-dir "target/classes"
        jar-file "target/artur.jar"
        basis (build/create-basis {:project "deps.edn"})]
    (build/write-pom {:class-dir class-dir
                      :lib 'acobster/artur
                      :version "0.1.0"
                      :basis basis
                      :src-dirs ["src"]})
    (build/copy-dir {:src-dirs ["src" "build"]
                     :target-dir class-dir})
    (build/compile-clj {:basis basis
                        :src-dirc ["src"]
                        :class-dir class-dir})
    (build/uber {:class-dir class-dir
                 :uber-file jar-file
                 :basis basis
                 :main 'artur.app})
    (println "Created" jar-file)))
