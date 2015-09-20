(ns pylos.core
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  (:gen-class)
  (:require [clojure.tools.namespace.repl :as repl]
            [strategy.negamax :refer :all]
            [game.game :refer :all]
            [pylos.board :refer :all]
            [pylos.game :refer :all]
            [pylos.score :refer :all]
            [pylos.human :refer :all]
            [pylos.pprint :refer :all]))

(set! *warn-on-reflection* true)

(defn initial-game [size first-player]
  (map->GamePosition {:board (starting-board size) 
                      :player first-player
                      :outcome nil}))

(defn output [play]
  (map #(print-game %) play))

(defn output-and-compare [play path]
  ; TODO implement
  )

(defn save-to-disk [play path]
  (with-open [w (clojure.java.io/writer path)]
    (binding [*print-length* false *out* w]
      (pr play))))

(defn refresh []
  (do 
    (repl/refresh)
    (reset! negamax-table {})))

(defn play [size {:keys [white black] :as strategies} first-player]
  (play-game {:game-position (initial-game size first-player)} strategies))

(defn play-human [size human-color first-player negamax-depth]
  (play size {human-color (human) (other-color human-color) (negamax score-middle-blocked negamax-depth)} first-player))

(defn play-negamax [size first-player negamax-depth]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play size {:black negamax-strategy :white negamax-strategy} first-player)))
