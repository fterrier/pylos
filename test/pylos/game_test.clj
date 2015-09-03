(ns pylos.game-test
  (:require [clojure.test :refer :all]
            [pylos.core-test :refer :all]
            [pylos.board :refer [ind cell]]
            [pylos.game :refer :all]))

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
    (is (can-remove-ball full-board-test (ind four [3 1 1])))))

(deftest can-rise
  (testing "Can rise"
    (is (false? (can-rise? full-board-test :white (ind four [3 1 2]) (ind four [3 2 1]))))))

; (deftest rise-candidates-test
;   (testing "Rise candidates"
;     (is (= (rise-candidates four-square-test :white) #{{:low-position (ind four [1 2 3]) :high-position (ind four [2 1 1])}}))
;     (is (= (rise-candidates four-square-test :black) #{}))
;     (is (= (rise-candidates full-board-test :white) #{}))
;     (is (= (rise-candidates full-board-test :black) #{}))
;     (is (= (rise-candidates (-> four-square-test 
;                                 (add-ball :black (ind four [1 3 1])) 
;                                 (add-ball :black (ind four [1 3 2]))) :black) #{{:low-position (ind four [1 1 1]) :high-position (ind four [2 2 1])}
;                                                                                 {:low-position (ind four [1 1 2]) :high-position (ind four [2 2 1])}
;                                                                                 {:low-position (ind four [1 3 1]) :high-position (ind four [2 1 1])}
;                                                                                 {:low-position (ind four [1 3 2]) :high-position (ind four [2 1 1])}}))
;     (is (= (rise-candidates (-> four-square-test 
;                                 (add-ball :black (ind four [1 3 1])) 
;                                 (add-ball :black (ind four [1 3 2]))) :white) #{{:low-position (ind four [1 2 3]) :high-position (ind four [2 1 1])}
;                                                                                 {:low-position (ind four [1 2 3]) :high-position (ind four [2 2 1])}}))
;     (is (= (rise-candidates two-layers-test :black) #{{:low-position (ind four [1 4 1]) :high-position (ind four [3 1 1])}}))
;     (is (= (rise-candidates two-layers-test :wite) #{}))))

(deftest removable-candidates-test
  (testing "Removable candidates"
    (is (= #{(ind four [1 4 1]) (ind four [2 1 2]) (ind four [2 2 2])} (removable-candidates two-layers-test :black)))
    (is (= #{(ind four [2 1 1]) (ind four [2 2 1])} (removable-candidates two-layers-test :white)))))

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

(deftest full-squares-test
  (testing "Full squares"
    (is (= #{(ind four [1 1 1])} (full-squares (-> four
                                                   (add-ball    :black (ind four [1 1 1]))
                                                   (add-ball    :black (ind four [1 2 1]))
                                                   (add-ball    :black (ind four [1 1 2]))
                                                   (add-ball    :black (ind four [1 2 2]))) :black)))
    (is (= #{} (full-squares (-> four
                                 (add-ball    :black (ind four [1 1 1]))
                                 (add-ball    :black (ind four [1 2 1]))
                                 (add-ball    :black (ind four [1 1 2]))
                                 (add-ball    :black (ind four [1 2 2]))) :white)))
    (is (= #{} (full-squares (-> four
                                 (add-ball    :black (ind four [1 1 1]))
                                 (add-ball    :black (ind four [1 2 1]))
                                 (add-ball    :black (ind four [1 1 2]))
                                 (add-ball    :black (ind four [1 2 2]))
                                 (remove-ball :black (ind four [1 1 1]))) :black)))))

(deftest is-full-square-test
  (testing "Is full square"
    (is (is-full-square (-> four 
                            (add-ball :white (ind four [1 1 1])) 
                            (add-ball :white (ind four [1 1 2]))
                            (add-ball :white (ind four [1 2 1])) 
                            (add-ball :white (ind four [1 2 2]))) (ind four [1 1 1]) :white))
    (is (false? (is-full-square (-> four 
                                    (add-ball :white (ind four [1 1 1])) 
                                    (add-ball :white (ind four [1 1 2]))
                                    (add-ball :white (ind four [1 2 1])) 
                                    (add-ball :black (ind four [1 2 2]))) (ind four [1 1 1]) :white)))))


(deftest board-move-map-test
  (testing "All board-move-map test"
    (is (= [(move-add two-layers-test :black (ind four [1 1 4]))
            (move-add two-layers-test :black (ind four [1 2 4]))
            (move-add two-layers-test :black (ind four [1 3 4]))
            (move-add two-layers-test :black (ind four [1 4 2]))
            (move-add two-layers-test :black (ind four [1 4 3]))
            (move-add two-layers-test :black (ind four [1 4 4]))
            (move-add two-layers-test :black (ind four [3 1 1]))
            (move-rise two-layers-test :black (ind four [1 4 1]) (ind four [3 1 1]))] (board-move-map {:board two-layers-test :player :black})))
    (is (= (into #{} (let [original-move (move-add full-board-square-top :white (ind four [3 2 2]))]
                       [(move-square original-move [(ind four [3 1 1])])
                        (move-square original-move [(ind four [3 1 2])])
                        (move-square original-move [(ind four [3 2 1])])
                        (move-square original-move [(ind four [3 2 2])])
                        (move-square original-move [(ind four [3 1 1]) (ind four [3 1 2])])
                        (move-square original-move [(ind four [3 1 1]) (ind four [3 2 1])])
                        (move-square original-move [(ind four [3 1 1]) (ind four [3 2 2])])
                        (move-square original-move [(ind four [3 1 2]) (ind four [3 2 1])])
                        (move-square original-move [(ind four [3 1 2]) (ind four [3 2 2])])
                        (move-square original-move [(ind four [3 2 1]) (ind four [3 2 2])])]))
           (into #{} (board-move-map {:board full-board-square-top :player :white}))))))

