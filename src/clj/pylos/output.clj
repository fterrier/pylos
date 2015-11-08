(ns pylos.output
  "game is {:player _ :board _ :past-moves _}
  move is {:board _ :move _}"
  (:require [strategy.compare :refer :all]
            [pylos.pprint :refer :all]))

(defn output-with-fn [play output-fn]
  (map #(output-fn %) play))

(defn output [play]
  (output-with-fn play print-game))

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
