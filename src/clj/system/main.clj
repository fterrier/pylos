(ns system.main
  (:gen-class)
  (:require [clojure.core.async :refer [chan]]
            [com.stuartsierra.component :as component]
            [system
             [handlers :refer [new-telegram-handler new-websockets-handler]]
             [game-runner :refer [new-game-runner]]
             [routes :refer [new-server-routes]]
             [server :refer [new-web-server]]]))

(defn get-system-map [port]
  (component/system-map 
   :gamerunner-ch       (chan)
   :routes              (component/using (new-server-routes) [:gamerunner :websockets-handler :telegram-handler])
   :web-server          (component/using (new-web-server port) [:routes])
   :websockets-handler  (component/using (new-websockets-handler) [:gamerunner-ch])
   :telegram-handler    (component/using (new-telegram-handler "152122841:AAE4iFW3JNdANZ0lPpibZ5pf-vYmH5z-p2w") [:gamerunner-ch])
   :gamerunner         (component/using (new-game-runner) [:gamerunner-ch])))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (component/start (get-system-map port))))
