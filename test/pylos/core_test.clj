(ns pylos.core-test
  (:require [clojure.test :refer :all]
            [pylos.board :refer :all]
            [pylos.game :refer :all]))

(def four (starting-board 4))
(def three (starting-board 3))
;(def game-four (initial-game 4 :white))

(def four-square-test (-> four
                          (add-ball :black (ind four [1 1 1]))
                          (add-ball :white (ind four [1 2 1]))
                          (add-ball :black (ind four [1 1 2]))
                          (add-ball :white (ind four [1 2 2]))
                          (add-ball :white (ind four [1 2 3]))))

(def two-layers-test (-> four
                         (add-ball :black (ind four [1 1 1]))
                         (add-ball :white (ind four [1 1 2]))
                         (add-ball :black (ind four [1 1 3]))

                         (add-ball :white (ind four [1 2 1]))
                         (add-ball :black (ind four [1 2 2]))
                         (add-ball :white (ind four [1 2 3]))

                         (add-ball :black (ind four [1 3 1]))
                         (add-ball :white (ind four [1 3 2]))
                         (add-ball :black (ind four [1 3 3]))

                         (add-ball :black (ind four [1 4 1]))
                         (add-ball :white (ind four [2 1 1]))
                         (add-ball :black (ind four [2 1 2]))
                         (add-ball :white (ind four [2 2 1]))
                         (add-ball :black (ind four [2 2 2]))))

(def full-board-test (-> four
                         (add-ball :black (ind four [1 1 1]))
                         (add-ball :white (ind four [1 1 2]))
                         (add-ball :black (ind four [1 1 3]))
                         (add-ball :white (ind four [1 1 4]))

                         (add-ball :white (ind four [1 2 1]))
                         (add-ball :black (ind four [1 2 2]))
                         (add-ball :white (ind four [1 2 3]))
                         (add-ball :black (ind four [1 2 4]))

                         (add-ball :black (ind four [1 3 1]))
                         (add-ball :white (ind four [1 3 2]))
                         (add-ball :black (ind four [1 3 3]))
                         (add-ball :white (ind four [1 3 4]))

                         (add-ball :white (ind four [1 4 1]))
                         (add-ball :black (ind four [1 4 2]))
                         (add-ball :white (ind four [1 4 3]))
                         (add-ball :black (ind four [1 4 4]))

                         (add-ball :white (ind four [2 1 1]))
                         (add-ball :black (ind four [2 1 2]))
                         (add-ball :white (ind four [2 1 3]))

                         (add-ball :black (ind four [2 2 1]))
                         (add-ball :white (ind four [2 2 2]))
                         (add-ball :black (ind four [2 2 3]))

                         (add-ball :white (ind four [2 3 1]))
                         (add-ball :black (ind four [2 3 2]))
                         (add-ball :white (ind four [2 3 3]))

                         (add-ball :black (ind four [3 1 1]))
                         (add-ball :white (ind four [3 1 2]))

                         ; (add-ball :white (ind four [3 2 1]))
                         (add-ball :black (ind four [3 2 2]))))

(def full-board-square-top (-> four
                               (add-ball :black (ind four [1 1 1]))
                               (add-ball :white (ind four [1 1 2]))
                               (add-ball :black (ind four [1 1 3]))
                               (add-ball :white (ind four [1 1 4]))

                               (add-ball :white (ind four [1 2 1]))
                               (add-ball :black (ind four [1 2 2]))
                               (add-ball :white (ind four [1 2 3]))
                               (add-ball :black (ind four [1 2 4]))

                               (add-ball :black (ind four [1 3 1]))
                               (add-ball :white (ind four [1 3 2]))
                               (add-ball :black (ind four [1 3 3]))
                               (add-ball :black (ind four [1 3 4]))

                               (add-ball :white (ind four [1 4 1]))
                               (add-ball :black (ind four [1 4 2]))
                               (add-ball :white (ind four [1 4 3]))
                               (add-ball :black (ind four [1 4 4]))

                               (add-ball :white (ind four [2 1 1]))
                               (add-ball :black (ind four [2 1 2]))
                               (add-ball :white (ind four [2 1 3]))

                               (add-ball :black (ind four [2 2 1]))
                               (add-ball :white (ind four [2 2 2]))
                               (add-ball :black (ind four [2 2 3]))

                               (add-ball :white (ind four [2 3 1]))
                               (add-ball :black (ind four [2 3 2]))
                               (add-ball :black (ind four [2 3 3]))

                               (add-ball :white (ind four [3 1 1]))
                               (add-ball :white (ind four [3 1 2]))

                               (add-ball :white (ind four [3 2 1]))))

; w b w b     o o o     - -     -
; w b b b     o o -     - -
; w w b o     - - -
; w o o o

(def square-level2-test (-> four
                         (add-ball :black (ind four [1 1 1]))
                         (add-ball :white (ind four [1 1 2]))
                         (add-ball :black (ind four [1 1 3]))
                         (add-ball :white (ind four [1 1 4]))

                         (add-ball :white (ind four [1 2 1]))
                         (add-ball :white (ind four [1 2 2]))
                         (add-ball :white (ind four [1 2 3]))
                         (add-ball :black (ind four [1 2 4]))

                         (add-ball :black (ind four [1 3 1]))
                         (add-ball :white (ind four [1 3 2]))
                         (add-ball :black (ind four [1 3 3]))
                         (add-ball :white (ind four [1 3 4]))

                         (add-ball :white (ind four [1 4 1]))
                         (add-ball :black (ind four [1 4 2]))
                         (add-ball :white (ind four [1 4 3]))
                         (add-ball :black (ind four [1 4 4]))

                         (add-ball :white (ind four [2 1 1]))
                         (add-ball :white (ind four [2 1 2]))
                        ;  (add-ball :white (ind four [2 1 3]))

                         (add-ball :black (ind four [2 2 1]))
                         (add-ball :white (ind four [2 2 2]))
                         (add-ball :white (ind four [2 2 3]))

                         (add-ball :black (ind four [2 3 1]))
                         (add-ball :black (ind four [2 3 2]))
                         (add-ball :black (ind four [2 3 3]))

                         (add-ball :black (ind four [3 1 1]))
                         (add-ball :black (ind four [3 2 1]))

                         ; (add-ball :white (ind four [3 2 1]))
                         (add-ball :black (ind four [3 2 2]))))

; (def moves-optimize
;   (-> four
;       (add-ball :white (ind four [1 1 1]))
;       (add-ball :black (ind four [1 1 2]))
;       (add-ball :white (ind four [1 1 3]))
;       (add-ball :black (ind four [1 1 4]))

;       (add-ball :white (ind four [1 2 1]))
;       (add-ball :black (ind four [1 2 2]))
;       (add-ball :black (ind four [1 2 3]))
;       (add-ball :black (ind four [1 2 4]))

;       (add-ball :white (ind four [1 3 1]))
;       (add-ball :white (ind four [1 3 2]))
;       (add-ball :black (ind four [1 3 3]))

;       (add-ball :white (ind four [1 4 1]))))

; (def moves-bug-add
;   (-> four
;       (add-ball :white (ind four [1 1 1]))
;       (add-ball :black (ind four [1 1 2]))
;       (add-ball :white (ind four [1 1 3]))
;       (add-ball :black (ind four [1 1 4]))
;       (add-ball :white (ind four [1 2 1]))
;       (add-ball :black (ind four [1 2 3]))
;       (add-ball :white (ind four [1 3 1]))
;       (add-ball :black (ind four [1 3 2]))
;       (add-ball :white (ind four [1 2 2]))
;       (add-ball :black (ind four [1 3 3]))
;       (remove-ball :white (ind four [1 1 3])) (add-ball :white (ind four [2 1 1]))
;       (add-ball :black (ind four [1 1 3]))
;       (add-ball :white (ind four [1 2 4]))
;       (remove-ball :black (ind four [1 2 3])) (add-ball :black (ind four [2 2 1]))
;       (add-ball :white (ind four [1 4 2]))
;       (add-ball :black (ind four [1 2 3]))
;       (remove-ball :white (ind four [1 2 4])) (add-ball :white (ind four [2 1 2]))
;       (add-ball :black (ind four [1 2 4])) (remove-ball :black (ind four [2 2 1]))))
