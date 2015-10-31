(ns pylos.pylos
  (:require [game.game :refer :all]
            [pylos.board :refer :all]
            [pylos.game :refer :all]))

(declare next-game-position)

(defrecord GamePosition [board player outcome]
  Game
  (generate-moves [game-position]
                  (order-moves board (generate-all-moves game-position)))
  (make-move [game-position move]
             (next-game-position game-position move (make-move-on-board board move))))


(defn next-game-position [{:keys [player] :as game-position} move board]
  {:pre [(= player (:color move))]}
  (let [game-over?         (game-over? board)
        next-game-position (map->GamePosition {:board board
                                               :player (other-color player)
                                               :outcome (if game-over? (winner board) nil)})]
    next-game-position))

(defn initial-game [size first-player]
  (map->GamePosition {:board (starting-board size)
                      :player first-player
                      :outcome nil}))
