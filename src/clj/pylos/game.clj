(ns pylos.game
  (:require [game.game :refer [Game GamePosition other-color]]
            [pylos
             [board :refer [new-pylos-board]]
             [move :refer [game-over? generate-all-moves is-move-allowed make-move-on-board order-moves winner]]]))

(declare next-game-position)

(defrecord PylosGamePosition [board player outcome]
  GamePosition
  (move-allowed? [game-position move]
    (is-move-allowed board move))
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
  (initial-game-position [_ first-player]
    (map->PylosGamePosition {:board   (new-pylos-board size)
                             :player  first-player
                             :outcome nil})))

(defn new-pylos-game [size]
  (->Pylos size))
