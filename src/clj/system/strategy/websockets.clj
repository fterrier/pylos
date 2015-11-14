(ns system.strategy.websockets
  (:require [game.game :refer :all]
            [system.app :refer :all]
            [clojure.core.async :as async :refer [<!! go chan sub]]))

(defn wait-for-websocket-move [game-ch game-position]
  (let [{:keys [game-infos]} (<!! game-ch)]
    (println game-infos)
    {:next-move (:move game-infos)}))

; TODO not sure this is the right place to do cleanup
(defrecord WebsocketsStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position]
                    (wait-for-websocket-move game-ch game-position)))

(defn websockets [game-ch]
  (->WebsocketsStrategy game-ch))
