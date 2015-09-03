(ns pylos.score-test
  (:require [clojure.test :refer :all]
            [pylos.score :refer :all]
            [pylos.core-test :refer :all]))


(deftest score-test
  (testing "Score"
    (is (= 0 (score-middle-blocked {:board four-square-test :player :black})))
    (is (= 0 (score-middle-blocked {:board four-square-test :player :white})))
    (is (= 11/6 (score-middle-blocked {:board two-layers-test :player :white})))
    (is (= -11/6 (score-middle-blocked {:board two-layers-test :player :black})))))

