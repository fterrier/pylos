(ns pylos.board-test
  (:require [clojure.test :refer :all]
            [pylos.core-test :refer :all]
            [pylos.game :refer :all]
            [pylos.init :refer :all]
            [pylos.board :refer :all]))

(deftest position-on-top-test
  (testing "Position on top"
    (is (= 29 (position-on-top four (ind four [3 1 1]))))
    (is (= nil (position-on-top four (ind four [3 2 1]))))
    (is (= nil (position-on-top four (ind four [3 1 2]))))
    (is (= nil (position-on-top four (ind four [3 2 2]))))))

(deftest positions-under-position-test
  (testing "Positions under position"
    (is (= 25 (count (positions-under-position four (ind four [3 1 1])))))))

(deftest square-positions-at-position-test
  (testing "Square positions at position"
    (is (= #{(ind four [1 1 1])} (square-positions-at-position four (ind four [1 1 1]))))
    (is (= #{(ind four [1 3 3])} (square-positions-at-position four (ind four [1 4 4]))))
    (is (= #{(ind four [1 1 1]) (ind four [1 1 2]) (ind four [1 2 1]) (ind four [1 2 2])} (square-positions-at-position four (ind four [1 2 2]))))))

(deftest squares-at-position-test
  (testing "Squares at position"
    (is (= #{(ind four [1 1 1])} (squares-at-position four-square-test (ind four [1 1 1]))))
    (is (= #{(ind four [1 1 1])} (squares-at-position four-square-test (ind four [1 2 2]))))))

(deftest square-corners-test
  (testing "Square corners"
    (is (= #{} (square-corners four (ind four [1 4 4]))))
    (is (= #{(ind four [1 2 2]) (ind four [1 2 1]) (ind four [1 1 2]) (ind four [1 1 1])} (square-corners four (ind four [1 1 1]))))))

(deftest calculate-all-positions-test
  (testing "All positions"
    (is (= (calculate-all-positions 4) [[1 1 1] [1 1 2] [1 1 3] [1 1 4]
                                        [1 2 1] [1 2 2] [1 2 3] [1 2 4]
                                        [1 3 1] [1 3 2] [1 3 3] [1 3 4]
                                        [1 4 1] [1 4 2] [1 4 3] [1 4 4]
                                        [2 1 1] [2 1 2] [2 1 3]
                                        [2 2 1] [2 2 2] [2 2 3]
                                        [2 3 1] [2 3 2] [2 3 3]
                                        [3 1 1] [3 1 2]
                                        [3 2 1] [3 2 2]
                                        [4 1 1]]))))
