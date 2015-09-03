(ns pylos.score
  (:require [pylos.board :refer :all]
             [pylos.game :refer :all]))

(defn balls-in-middle [board player]
  (let [middle-positions (middle-positions board)]
    (filter #(= player (cell board %)) middle-positions)))

(defn balls-blocked [board player]
  (let [balls-on-board (balls-on-board board player)]
    (filter #(not (can-remove-ball board %)) balls-on-board)))

(defn score-middle-blocked [{:keys [player board]}]
  ; (if outcome 100 
    (let [other-player                 (other-color player)
          player-balls                 (count (balls-on-board board player))
          other-player-balls           (count (balls-on-board board other-player))
          ball-difference              (- other-player-balls player-balls)
          player-balls-in-middle       (count (balls-in-middle board player))
          other-player-balls-in-middle (count (balls-in-middle board other-player))
          balls-in-middle-difference   (- player-balls-in-middle other-player-balls-in-middle)
          player-balls-blocked         (count (balls-blocked board player))
          other-player-balls-blocked   (count (balls-blocked board other-player))
          balls-blocked-difference     (- player-balls-blocked other-player-balls-blocked)]
      (- (+ ball-difference (/ balls-in-middle-difference 2))
         (/ balls-blocked-difference 3))))