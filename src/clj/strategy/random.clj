(ns strategy.random
  (:require [clojure.core.async :refer [go]]
            [game.game :refer [generate-moves make-move]]
            [game.strategy :refer [Strategy]]))


(defrecord RandomStrategy []
  Strategy
  (choose-next-move [this game-position]
                    (go {:next-move (rand-nth (generate-moves game-position))})))

(defn random []
  (->RandomStrategy))
