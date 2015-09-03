(defproject pylos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [io.aviso/pretty "0.1.18"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.taoensso/timbre "4.1.1"]
                 [clj-stacktrace "0.2.8"]
                 [org.clojure/data.int-map "0.2.1"]]
  :repl-options {:init (do (require 'clj-stacktrace.repl))
                 :caught clj-stacktrace.repl/pst+}
  :main ^:skip-aot pylos.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.3"]]}})