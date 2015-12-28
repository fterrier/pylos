(ns system.handlers
  (:require [com.stuartsierra.component :as component]
            [server.handlers.websockets :refer [websockets-handler]]
            [server.handlers.telegram :refer [telegram-handler]]
            [server.handlers.handler :refer [start-handler
                                             stop-handler
                                             get-routes]]))

(defrecord HandlerComponent [handler gamerunner-ch routes]
  component/Lifecycle
  (start [component]
    (let [handler (start-handler handler gamerunner-ch)]
      (assoc component :handler handler :routes (get-routes handler))))
  (stop [component] 
    (assoc component :handler (stop-handler handler))))

(defn new-handler [handler]
  (map->HandlerComponent {:handler handler}))

(defn new-websockets-handler []
  (new-handler (websockets-handler)))

(defn new-telegram-handler [bot-id]
  (new-handler (telegram-handler bot-id)))

