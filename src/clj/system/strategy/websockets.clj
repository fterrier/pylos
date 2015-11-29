(ns system.strategy.websockets
  (:require [game.game :refer :all]
            [clojure.core.async :as async :refer [<! go chan sub]]))

(defn wait-for-websocket-move [game-ch game-position]
  (go
    (let [{:keys [game-infos]} (<! game-ch)]
      (println "Websockets Strategy - got move from channel" game-infos)
      {:next-move (:move game-infos)})))

(defrecord WebsocketsStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position]
                    (wait-for-websocket-move game-ch game-position)))

(defn websockets [game-ch close-ch]
  (->WebsocketsStrategy game-ch))
