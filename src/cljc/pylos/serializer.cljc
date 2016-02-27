(ns pylos.serializer
  (:require [game.serializer :refer [GameSerializer]]
            [pylos.init :refer [initialize-board-meta]]
            [pylos.board :refer [board-size]]))

(defrecord PylosSerializer []
  GameSerializer
  (deserialize-game-position [_ {:keys [size board player outcome]}]
    {:board (initialize-board-meta board size)
     :player player
     :outcome outcome})
  (serialize-game-position [_ {:keys [board player outcome]}]
    {:board board :size (board-size board) :player player :outcome outcome}))

(defn new-pylos-serializer []
  (->PylosSerializer))
