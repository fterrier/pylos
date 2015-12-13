(ns system.strategy.websockets
  (:require [game.strategy :refer [Strategy]]
            [clojure.core.async :as async :refer [<! go chan sub]]))

(defrecord WebsocketsStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position] game-ch)
  (get-input-channel [this] game-ch))

(defn websockets []
  (->WebsocketsStrategy (chan)))
