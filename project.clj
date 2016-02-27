(defproject pylos "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx4G"]
  :min-lein-version "2.0.0"
  :dependencies [[ch.qos.logback/logback-classic "1.1.3"]
                 [com.stuartsierra/component "0.3.0"]
                 [com.taoensso/sente "1.6.0"]
                 [com.taoensso/timbre "4.2.0"]
                 [compojure "1.4.0"]
                 [devcards "0.2.1-6"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.18"]
                 [io.aviso/pretty "0.1.18"]
                 [org.apache.xmlgraphics/batik-codec "1.7"]
                 [org.apache.xmlgraphics/batik-transcoder "1.7"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.omcljs/om "1.0.0-alpha30"]
                 [org.slf4j/log4j-over-slf4j "1.7.13"]
                 [prismatic/om-tools "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [secretary "1.2.3"]
                 [valichek/component-compojure "0.2-SNAPSHOT"]]
  :repl-options {:init-ns user
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

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
                   :dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-2"]
                                  [org.clojure/tools.namespace "0.3.0-alpha3"]]
                   :plugins [[lein-figwheel "0.5.0"]]}})
