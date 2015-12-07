(ns pylos.board-test
  (:require [clojure.test :refer :all]
            [pylos.core-test :refer :all]
            [pylos.move :refer :all]
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

(deftest position-opens-test
  (testing "Square opens layer above"
    (let [four-test (-> four
                        (add-ball :black (ind four [1 1 1]))
                        (add-ball :white (ind four [1 2 1]))
                        (add-ball :black (ind four [1 1 2]))
                        (add-ball :black (ind four [1 2 2])))]
      (is (= :open (cell four-test (ind four [2 1 1])))))))

(deftest has-square-test
  (testing "Has square"
    (is (has-square four-square-test (ind four [1 1 1])))
    (is (false? (has-square (add-ball four :white (ind four [1 4 4])) (ind four [1 4 4]))))
    (is (false? (has-square four-square-test (ind four [1 2 1]))))
    (is (false? (has-square four-square-test (ind four [1 1 2]))))
    (is (false? (has-square four-square-test (ind four [1 3 1]))))))

(deftest can-remove-test
  (testing "Can remove ball"
    (is (false? (can-remove-ball two-layers-test (ind four [1 1 1]))))
    (is (can-remove-ball two-layers-test (ind four [1 4 1])))
    (is (can-remove-ball full-board-test (ind four [3 2 2])))
    (is (can-remove-ball full-board-test (ind four [3 1 1])))
    (is (can-remove-ball (-> four-square-test
                             (add-ball :white (ind four [2 1 1]))
                             (remove-ball :white (ind four [2 1 1]))) (ind four [1 1 1])))))

; (deftest can-rise
;   (testing "Can rise"
;     (is (false? (can-rise? full-board-test :white (ind four [3 1 2]) (ind four [3 2 1]))))))

(deftest removable-candidates-test
  (testing "Removable candidates"
    (is (= #{(ind four [1 4 1]) (ind four [2 1 2]) (ind four [2 2 2]) (ind four [1 4 2])} (removable-candidates two-layers-test :black (ind four [1 4 2]))))
    (is (= #{(ind four [2 1 1]) (ind four [2 2 1]) (ind four [1 4 2])} (removable-candidates two-layers-test :white (ind four [1 4 2]))))))

(deftest calculate-next-move-test
  (testing "Next move"
    (is (= [{:type :square :color :white :original-move {:type :add :color :white :position (ind four [2 1 3])} :positions #{(ind four [2 1 3]) (ind four [1 1 4])}}
            {:type :square :color :white :original-move {:type :add :color :white :position (ind four [2 1 3])} :positions #{(ind four [2 1 3])}}]
           (calculate-next-moves {:player :white :board square-level2-test} (ind four [2 1 3]))))))

(defn equal-with-meta [a b]
  (and (= a b)
       (= (meta a) (meta b))))

(deftest add-remove-test
  (testing "Add remove test"
    (is (equal-with-meta four (-> four
                                  (add-ball    :black (ind four [1 1 1]))
                                  (remove-ball :black (ind four [1 1 1])))))
    (is (equal-with-meta four (-> four
                                  (add-ball    :black (ind four [1 1 1]))
                                  (add-ball    :black (ind four [1 2 1]))
                                  (add-ball    :black (ind four [1 1 2]))
                                  (add-ball    :black (ind four [1 2 2]))
                                  (remove-ball :black (ind four [1 1 1]))
                                  (remove-ball :black (ind four [1 2 1]))
                                  (remove-ball :black (ind four [1 1 2]))
                                  (remove-ball :black (ind four [1 2 2])))))))

; (deftest full-squares-test
  ; (testing "Full squares"
  ;   (is (= #{(ind four [1 1 1])} (full-squares (-> four
  ;                                                  (add-ball    :black (ind four [1 1 1]))
  ;                                                  (add-ball    :black (ind four [1 2 1]))
  ;                                                  (add-ball    :black (ind four [1 1 2]))
  ;                                                  (add-ball    :black (ind four [1 2 2]))) :black)))
  ;   (is (= #{} (full-squares (-> four
  ;                                (add-ball    :black (ind four [1 1 1]))
  ;                                (add-ball    :black (ind four [1 2 1]))
  ;                                (add-ball    :black (ind four [1 1 2]))
  ;                                (add-ball    :black (ind four [1 2 2]))) :white)))
  ;   (is (= #{} (full-squares (-> four
  ;                                (add-ball    :black (ind four [1 1 1]))
  ;                                (add-ball    :black (ind four [1 2 1]))
  ;                                (add-ball    :black (ind four [1 1 2]))
  ;                                (add-ball    :black (ind four [1 2 2]))
  ;                                (remove-ball :black (ind four [1 1 1]))) :black)))))

; (deftest is-full-square-test
;   (testing "Is full square"
;     (is (is-full-square (-> four
;                             (add-ball :white (ind four [1 1 1]))
;                             (add-ball :white (ind four [1 1 2]))
;                             (add-ball :white (ind four [1 2 1]))
;                             (add-ball :white (ind four [1 2 2]))) (ind four [1 1 1]) :white))
;     (is (false? (is-full-square (-> four
;                                     (add-ball :white (ind four [1 1 1]))
;                                     (add-ball :white (ind four [1 1 2]))
;                                     (add-ball :white (ind four [1 2 1]))
;                                     (add-ball :black (ind four [1 2 2]))) (ind four [1 1 1]) :white)))))


(deftest has-new-full-square-test
  (testing "Has new full square"
    (is (nil? (new-full-square-position (-> four (add-ball :white (ind four [1 1 2])) (add-ball :white (ind four [1 2 1])) (add-ball :black (ind four [1 2 2]))) (ind four [1 1 1]) :white)))
    (is (new-full-square-position (add-ball four-square-test :white (ind four [1 3 2])) (ind four [1 3 1]) :white))
    (is (nil? (new-full-square-position (add-ball four-square-test :white (ind four [1 3 2])) (ind four [1 3 1]) :black)))
    (is (new-full-square-position (add-ball four-square-test :white (ind four [1 3 2])) (ind four [1 3 3]) :white))
    (is (nil? (new-full-square-position (add-ball four-square-test :white (ind four [1 3 2])) (ind four [1 3 3]) :black)))
    (is (nil? (new-full-square-position (add-ball four-square-test :white (ind four [1 3 2])) (ind four [1 3 4]) :white)))))
