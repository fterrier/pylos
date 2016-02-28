(ns chess.game
  (:require [game.game :refer [GamePosition]]))

(defrecord ChessGamePosition [board player outcome]
  GamePosition
  (move-allowed? [game-position move])
  (generate-moves [game-position])
  (make-move [game-position move]))
