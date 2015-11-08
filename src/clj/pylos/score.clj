(ns pylos.score
  (:require [pylos.board :refer :all]
            [pylos.game :refer :all]))

(defn balls-around [board player]
  (let [balls-on-board (balls-on-board board player)]
    (vals (select-keys (number-of-positions-around board) balls-on-board))))

(defn balls-blocked [board player]
  (let [balls-on-board (balls-on-board board player)]
    (filter #(not (can-remove-ball board %)) balls-on-board)))

(defn score-middle-blocked [{:keys [player board]}]
  ; (if outcome 100
    (let [other-player                 (other-color player)
          player-balls                 (count (balls-on-board board player))
          other-player-balls           (count (balls-on-board board other-player))
          ball-difference              (- other-player-balls player-balls)
          player-balls-around          (apply + (balls-around board player))
          other-player-balls-around    (apply + (balls-around board other-player))
          balls-around-difference      (- player-balls-around other-player-balls-around)
          player-balls-blocked         (count (balls-blocked board player))
          other-player-balls-blocked   (count (balls-blocked board other-player))
          balls-blocked-difference     (- player-balls-blocked other-player-balls-blocked)
          ]
      (-
        (+ ball-difference (/ balls-around-difference 276))
        (/ balls-blocked-difference 4))))
