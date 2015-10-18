(ns system.server
  (:require
    [org.httpkit.server :refer [run-server]]
    [com.stuartsierra.component :as component]))

(defrecord WebServer [options server handler]
  component/Lifecycle
  (start [component]
    (let [handler (condp #(%1 %2) handler
                    fn? handler
                    var? handler
                    (:app handler))
          server (run-server handler options)]
      (println "Started http-kit with options" options)
      (assoc component :server server)))
  (stop [component]
    (when server (server) component)))

(def allowed-opts
  [:ip :port :thread :worker-name-prefix :queue-size :max-body :max-line])

(defn new-web-server
  ([port] (map->WebServer {:options {:port port}}))
  ([port handler] (new-web-server port handler {}))
  ([port handler options]
  ;  (util/assert-only-contains-options! "http-kit" options allowed-opts)
   (map->WebServer {:options (merge {:port port} options)
                    :handler handler})))
