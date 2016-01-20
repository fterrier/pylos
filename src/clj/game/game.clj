(ns game.game)

(defn other-color [color]
  (if (= color :white) :black :white))

(defprotocol GamePosition
  (board [this])
  (player [this])
  (score [this]) ;; TODO move to board ?
  (outcome [this])
  (generate-moves [this])
  (move-allowed? [this move])
  (make-move [this move]))

(defprotocol Game
  (initial-game-position [this first-player]))
