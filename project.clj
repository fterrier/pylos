(defproject pylos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx4G"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0-RC3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.slf4j/log4j-over-slf4j "1.7.13"]
                 [io.aviso/pretty "0.1.18"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.taoensso/timbre "4.1.1"]
                 [clj-stacktrace "0.2.8"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.omcljs/om "0.9.0"  :exclusions [cljsjs/react]]
                 [cljsjs/react "0.14.3-0"]
                 [prismatic/om-tools "0.3.12"]
                 [com.stuartsierra/component "0.3.0"]
                 [http-kit "2.1.18"]
                 [secretary "1.2.3"]
                 [compojure "1.4.0"]
                 [com.taoensso/sente "1.6.0"]
                 [ring/ring-defaults "0.1.5"]
                 [hiccup "1.0.5"]
                 [ring/ring-json "0.4.0"]
                 [org.apache.xmlgraphics/batik-transcoder "1.7"]
                 [org.apache.xmlgraphics/batik-codec "1.7"]
                 [valichek/component-compojure "0.2-SNAPSHOT"]]
  :repl-options {:port 7888
                 :init-ns system.init-dev
                 :init (do (require 'clj-stacktrace.repl))
                 :caught clj-stacktrace.repl/pst+}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :uberjar-name "pylos.jar"

  :profiles {:uberjar {:source-paths ["env/prod/src/clj"]
                       :omit-source true
                       :plugins [[lein-cljsbuild "1.1.2"]]
                       :aot :all
                       :hooks [leiningen.cljsbuild]
                       :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc" "env/prod/src/cljs"]
                                                  :compiler {:output-to "resources/public/js/main.js"
                                                             :optimizations :advanced
                                                             :pretty-print false}}}}}
             :dev {:source-paths ["env/dev/src/clj"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [figwheel-sidecar "0.5.0-2"]
                                  [devcards "0.2.1"]
                                        ;[alembic "0.3.2"]
                                  ]
                   :plugins [[lein-figwheel "0.5.0-2"]]}})
