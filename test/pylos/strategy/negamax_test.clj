(ns pylos.strategy.negamax-test
  (:require [clojure.test :refer :all]
            [strategy.negamax :refer :all]
            [pylos.core-test :refer :all]
            [pylos.move :refer :all]
            [pylos.game :refer :all]
            [pylos.score :refer :all]))

(deftest full-square-top-negamax-test
  (testing "Negamax on full square top"
    (is (= :square (:type (:next-move (negamax-choose-move (map->PylosGamePosition {:board full-board-square-top :player :white}) 6 score-middle-blocked [])))))
    (is (> 0 (:best-possible-score (:negamax-values (negamax-choose-move (map->PylosGamePosition {:board pylos.core-test/full-board-test :player :black}) 6 score-middle-blocked [])))))
    (is (> 0 (:best-possible-score (:negamax-values (negamax-choose-move (map->PylosGamePosition {:board pylos.core-test/full-board-test :player :white}) 6 score-middle-blocked [])))))))
