(ns pylos.core
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  (:require [strategy.negamax :refer :all]
            [strategy.compare :refer :all]
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

(defn output-and-compare-games [[game1 & rest1] [game2 & rest2]]
  (if (nil? game1) []
    (do
      (print-game (dissoc game1 :additional-infos))
      ; (print-game (assoc-in game2 
      ;                       [:game-position :board] 
      ;                       (with-meta (:board (:game-position game2))
      ;                                  ; TODO change this 4 here
      ;                                  {:helper-meta-board (helper-meta-board 4)})))
      (if (not= (:game-position game1) (:game-position game2))
        (println "Game positions differ")
        (display-compare-additional-infos (:additional-infos game1) (:additional-infos game2)))
      
      (cons game1 (output-and-compare-games rest1 rest2)))))

(defn output-and-compare [play path]
  (output-and-compare-games play (read-string (slurp path))))

(defn save-to-disk [play path]
  (with-open [w (clojure.java.io/writer path)]
    (binding [*print-length* false *out* w]
      (pr play))))

(defn play [size {:keys [white black] :as strategies} first-player]
  (play-game {:game-position (initial-game size first-player)} strategies))

(defn play-human [size human-color first-player negamax-depth]
  (play size {human-color (human) (other-color human-color) (negamax score-middle-blocked negamax-depth)} first-player))

(defn play-negamax [size first-player negamax-depth]
  (let [negamax-strategy (negamax score-middle-blocked negamax-depth)]
    (play size {:black negamax-strategy :white negamax-strategy} first-player)))
