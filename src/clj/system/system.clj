(ns system.system
  (:gen-class)
  (:require [clojure.core.async :refer [chan]]
            [com.stuartsierra.component :as component]
            [system
             [events :refer [new-event-handler]]
             [server :refer [new-web-server]]
             [websockets :refer [new-channel-sockets]]
             [routes :refer [new-server-routes]]
             [game :refer [new-game-runner new-game-output]]]
            [taoensso.sente.server-adapters.http-kit
             :refer
             [sente-web-server-adapter]]))


(defn get-system-map [port]
  (component/system-map :websockets-ch  (chan)
                        :routes         (component/using (new-server-routes) [:game-runner :websockets])
                        :web-server     (component/using (new-web-server port) [:routes])
                        :websockets     (component/using (new-channel-sockets sente-web-server-adapter) [:event-handler])
                        :event-handler  (component/using (new-event-handler) [:websockets-ch])
                        :game-runner    (component/using (new-game-runner) [:websockets-ch :game-output])
                        :game-output    (component/using (new-game-output) [:websockets])))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (component/start (get-system-map port))))
