(ns system.routes
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [routes]]
            [clojure.tools.logging :as log]
            [server.site :refer [view-routes]]))

(defrecord Routes [game-runner websockets-handler telegram-handler]
  component/Lifecycle
  (start [component]
    (log/info "Starting Routes with routes" )
    (assoc component :routes (routes 
                              (:routes websockets-handler) 
                              (:routes telegram-handler)
                              (:routes game-runner)
                              view-routes)))
  (stop [component] component))

(defn new-server-routes []
  "Creates the routes for the server, combining all the given handlers and site"
  (map->Routes {}))
