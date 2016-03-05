(ns pylos.serializer
  (:require [game.serializer :refer [GameSerializer]]
            [pylos.board :refer [board-size new-pylos-board]]))

(defrecord PylosSerializer []
  GameSerializer
  (deserialize-game-position [_ {:keys [size board player outcome]}]
    {:board (new-pylos-board board size)
     :player player
     :outcome outcome})
  (serialize-game-position [_ {:keys [board player outcome]}]
    {:board (:board board) :size (board-size board) :player player :outcome outcome}))

(defn new-pylos-serializer []
  (->PylosSerializer))
