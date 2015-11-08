(ns system.strategy.websockets
  (:require [game.game :refer :all]
            [clojure.core.async :as async :refer [<!! go]]))


(defn wait-for-websocket-move [event-ch game-position]

   (let [game-info (<!! event-ch)]
     (println game-info)
      {:next-move (:move game-info)}))

(defrecord WebsocketsStrategy [event-ch]
  Strategy
  (choose-next-move [this game-position]
                    (wait-for-websocket-move event-ch game-position)))

(defn websockets [event-ch]
  (->WebsocketsStrategy event-ch))
