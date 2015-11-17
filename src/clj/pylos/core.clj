(ns pylos.core
  (:require [game.game :refer :all]
            [pylos.board :refer :all]
            [pylos.strategy.human :refer :all]
            [strategy.negamax :refer :all]
            [pylos.score :refer :all]
            [pylos.pprint :refer :all]
            [pylos.init :refer [starting-board]]
            [pylos.game :refer [order-moves generate-all-moves make-move-on-board game-over? winner]]))

(set! *warn-on-reflection* true)

(declare next-game-position)

(defrecord GamePosition [board player outcome]
  Game
  (generate-moves [game-position]
                  (order-moves board (generate-all-moves game-position)))
  (make-move [game-position move]
             (next-game-position game-position move (make-move-on-board board move))))

;              PrettyPrint
; (print-game [game-position]
;             (print-pylos-game game-position))

(defn next-game-position [{:keys [player] :as game-position} move board]
  {:pre [(= player (:color move))]}
  (let [game-over?         (game-over? board)
        next-game-position (map->GamePosition {:board   board
                                               :player  (other-color player)
                                               :outcome (if game-over? (winner board) nil)})]
    next-game-position))

(defn initial-game [size first-player]
  (map->GamePosition {:board   (starting-board size)
                      :player  first-player
                      :outcome nil}))

(defn play [size {:keys [white black] :as strategies} first-player]
  (play-game {:game-position (initial-game size first-player)} strategies))

(defn play-human [size human-color first-player negamax-depth]
  (play size {human-color (human) (other-color human-color) (negamax score-middle-blocked negamax-depth)} first-player))

(defn play-negamax [size first-player negamax-depth]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play size {:black negamax-strategy :white negamax-strategy} first-player)))
