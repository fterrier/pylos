(ns strategy.random
  (:require [clojure.core.async :refer [go]]
            [clojure.tools.logging :as log]
            [game.game :refer [generate-moves make-move]]
            [game.strategy :refer [Strategy]]))


(defrecord RandomStrategy []
  Strategy
  (choose-next-move [this game-position]
    (log/debug "Choosing next move from" game-position)
    (go {:next-move (rand-nth (generate-moves game-position))}))
  (get-input-channel [this]))

(defn random []
  (->RandomStrategy))
