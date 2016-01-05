(ns strategy.channel
  (:require clojure.core
            [clojure.core.async :as async :refer [chan close!]]
            [game.strategy :refer [Strategy]]))

(defrecord ChannelStrategy [game-ch]
  Strategy
  (choose-next-move [this game-position] 
    game-ch)
  (get-input-channel [this] game-ch)
  (notify-end-game [this] (close! game-ch)))

(defn channel []
  (->ChannelStrategy (chan)))
