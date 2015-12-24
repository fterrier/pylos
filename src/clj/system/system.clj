(ns system.system
  (:gen-class)
  (:require [clojure.core.async :refer [chan]]
            [com.stuartsierra.component :as component]
            [system
             [events :refer [new-event-handler]]
             [game :refer [new-game-runner]]
             [routes :refer [new-server-routes]]
             [server :refer [new-web-server]]
             [telegram :refer [new-telegram]]
             [websockets :refer [new-channel-sockets]]]
            [taoensso.sente.server-adapters.http-kit
             :refer
             [sente-web-server-adapter]]))

(defn get-system-map [port]
  (component/system-map :gamerunner-ch  (chan)
                        :routes         (component/using (new-server-routes) [:game-runner :websockets :telegram])
                        :web-server     (component/using (new-web-server port) [:routes])
                        :websockets     (component/using (new-channel-sockets sente-web-server-adapter) [:event-handler])
                        :event-handler  (component/using (new-event-handler) [:gamerunner-ch])
                        :telegram       (component/using (new-telegram 
                                                          "152122841:AAE4iFW3JNdANZ0lPpibZ5pf-vYmH5z-p2w") 
                                                         [:gamerunner-ch])
                        :game-runner    (component/using (new-game-runner) [:gamerunner-ch])))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (component/start (get-system-map port))))
