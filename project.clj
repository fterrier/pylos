(defproject pylos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx4G"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [io.aviso/pretty "0.1.18"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.taoensso/timbre "4.1.1"]
                 [clj-stacktrace "0.2.8"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.3.12"]
                 [com.stuartsierra/component "0.3.0"]
                 [http-kit "2.1.18"]
                 [compojure "1.4.0"]
                 [com.taoensso/sente "1.6.0"]
                 [ring/ring-defaults "0.1.5"]]
  :repl-options {:port 7888
                 :init (do (require 'clj-stacktrace.repl))
                 :caught clj-stacktrace.repl/pst+}
  :main ^:skip-aot system.init-dev
  :target-path "target/%s"
  :source-paths ["src/clj" "src/cljs"]

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]}}}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.3"]
                                  [figwheel-sidecar "0.4.1"]
                                  ;[alembic "0.3.2"]
                                  ]
                   :plugins [[lein-figwheel "0.4.1"]]}})
