(ns system.figwheel
  (:require
    [figwheel-sidecar.repl-api :as ra]
    [com.stuartsierra.component :as component]))

(def figwheel-config
  {:figwheel-options {:css-dirs ["resources/public/css"]} ;; <-- figwheel server config goes here
   :build-ids ["app"]   ;; <-- a vector of build ids to start autobuilding
   :all-builds          ;; <-- supply your build configs here
   [{:id "app"
     :source-paths ["src/cljs" "src/cljc"]
     :figwheel {:websocket-host "localhost"
                :on-jsload "pylos.core/fig-reload"}
     :compiler {:main "pylos.core"
                :asset-path "js/out"
                :output-to "resources/public/js/dev.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :pretty-print true
                :source-map true}}]})

(defrecord Figwheel []
  component/Lifecycle
  (start [config]
    (ra/start-figwheel! config)
    config)
  (stop [config]
    ;; Please note that when you stop the Figwheel Server http-kit throws
    ;; a java.util.concurrent.RejectedExecutionException, this is expected
    (ra/stop-figwheel!)
    config))

(defn cljs-repl []
  (ra/cljs-repl))
