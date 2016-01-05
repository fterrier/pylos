(ns strategy.multi
  (:require [clojure.core.async :refer [alts! go]]
            [game.strategy
             :refer
             [choose-next-move get-input-channel notify-end-game Strategy]]))

(defrecord MultiStrategy [strategies]
  Strategy
  (choose-next-move [this game-position]
    (let [channels (map #(choose-next-move % game-position) strategies)]
      (go (alts! channels))))
  (get-input-channel [this])
  (notify-end-game [this]
    (doseq [strategy strategies]
      (notify-end-game strategy))))

(defn multi-channel [strategies]
  (->MultiStrategy strategies))
