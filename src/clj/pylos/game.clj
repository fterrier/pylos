(ns pylos.game
  (:require [game.game :refer [GamePosition Game other-color]]
            [pylos.init :refer [starting-board]]
            [pylos.move :refer [order-moves generate-all-moves make-move-on-board game-over? winner]]))

(declare next-game-position)

; the rest here is not needed in cljc but there for convenience
(defrecord PylosGamePosition [board player outcome]
  GamePosition
  (generate-moves [game-position]
                  (order-moves board (generate-all-moves game-position)))
  (make-move [game-position move]
             (next-game-position game-position move (make-move-on-board board move))))

(defn next-game-position [{:keys [player] :as game-position} move board]
  {:pre [(= player (:color move))]}
  (let [game-over?         (game-over? board)
        next-game-position (map->PylosGamePosition {:board   board
                                                    :player  (other-color player)
                                                    :outcome (if game-over? (winner board) nil)})]
    next-game-position))

(defrecord Pylos [size]
  Game
  (initial-game-position [this first-player]
    (map->PylosGamePosition {:board   (starting-board size)
                             :player  first-player
                             :outcome nil})))

(defn new-pylos-game [size]
  (->Pylos size))
