(ns strategy.random
  (:require [game.game :refer :all]))


(defrecord RandomStrategy []
  Strategy
  (choose-next-move [this game-position] 
                    ))

(defn random []
  (->RandomStrategy))