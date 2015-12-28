(ns strategy.channel
  (:require [game.strategy :refer [Strategy]]
            [clojure.core.async :as async :refer [<! go chan sub]]))

(defrecord ChannelStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position] game-ch)
  (get-input-channel [this] game-ch))

(defn channel []
  (->ChannelStrategy (chan)))
