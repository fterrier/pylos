(ns pylos.core-test
  (:require [clojure.test :refer :all]
            [pylos.core :refer :all]))

(def four-square-test (-> four 
                          (add-ball :black [1 1 1]) 
                          (add-ball :white [1 2 1]) 
                          (add-ball :black [1 1 2]) 
                          (add-ball :white [1 2 2])
                          (add-ball :white [1 2 3])))

(def two-layers-test (-> four 
                         (add-ball :black [1 1 1]) 
                         (add-ball :white [1 1 2]) 
                         (add-ball :black [1 1 3]) 
                         (add-ball :white [1 2 1])
                         (add-ball :black [1 2 2])
                         (add-ball :white [1 2 3])
                         (add-ball :black [1 3 1]) 
                         (add-ball :white [1 3 2])
                         (add-ball :black [1 3 3])
                         (add-ball :black [1 4 1])
                         (add-ball :white [2 1 1])
                         (add-ball :black [2 1 2])
                         (add-ball :white [2 2 1])
                         (add-ball :black [2 2 2])))

(def full-board-test (-> four
                         (add-ball :black [1 1 1]) 
                         (add-ball :white [1 1 2]) 
                         (add-ball :black [1 1 3]) 
                         (add-ball :white [1 1 4]) 
                         
                         (add-ball :white [1 2 1])
                         (add-ball :black [1 2 2])
                         (add-ball :white [1 2 3])
                         (add-ball :black [1 2 4])
                         
                         (add-ball :black [1 3 1]) 
                         (add-ball :white [1 3 2])
                         (add-ball :black [1 3 3])
                         (add-ball :white [1 3 4])
                         
                         (add-ball :white [1 4 1])
                         (add-ball :black [1 4 2])
                         (add-ball :white [1 4 3])
                         (add-ball :black [1 4 4])
                         
                         (add-ball :white [2 1 1])
                         (add-ball :black [2 1 2])
                         (add-ball :white [2 1 3])
                         
                         (add-ball :black [2 2 1])
                         (add-ball :white [2 2 2])
                         (add-ball :black [2 2 3])
                         
                         (add-ball :white [2 3 1])
                         (add-ball :black [2 3 2])
                         (add-ball :white [2 3 3])
                         
                         (add-ball :black [3 1 1])
                         (add-ball :white [3 1 2])
                         
                         (add-ball :white [3 2 1])
                         (add-ball :black [3 2 2])))

(def full-board-square-top (-> four
                         (add-ball :black [1 1 1]) 
                         (add-ball :white [1 1 2]) 
                         (add-ball :black [1 1 3]) 
                         (add-ball :white [1 1 4]) 
                         
                         (add-ball :white [1 2 1])
                         (add-ball :black [1 2 2])
                         (add-ball :white [1 2 3])
                         (add-ball :black [1 2 4])
                         
                         (add-ball :black [1 3 1]) 
                         (add-ball :white [1 3 2])
                         (add-ball :black [1 3 3])
                         (add-ball :black [1 3 4])
                         
                         (add-ball :white [1 4 1])
                         (add-ball :black [1 4 2])
                         (add-ball :white [1 4 3])
                         (add-ball :black [1 4 4])
                         
                         (add-ball :white [2 1 1])
                         (add-ball :black [2 1 2])
                         (add-ball :white [2 1 3])
                         
                         (add-ball :black [2 2 1])
                         (add-ball :white [2 2 2])
                         (add-ball :black [2 2 3])
                         
                         (add-ball :white [2 3 1])
                         (add-ball :black [2 3 2])
                         (add-ball :black [2 3 3])
                         
                         (add-ball :white [3 1 1])
                         (add-ball :white [3 1 2])
                         
                         (add-ball :white [3 2 1])))

(deftest is-in-board-test
  (testing "Is in board"
    (is (is-in-board four [1 1 1]))
    (is (is-in-board four [2 1 1]))
    (is (is-in-board four [3 1 1]))
    (is (is-in-board four [4 1 1]))
    (is (false? (is-in-board four [4 1 2])))
    (is (false? (is-in-board four [4 2 1])))
    (is (false? (is-in-board four [5 1 1])))
    (is (false? (is-in-board four [1 4 5])))
    (is (is-in-board four [1 4 4]))))

(deftest position-opens-test
  (testing "Square opens layer above"
    (let [four-test (-> four 
                        (add-ball :black [1 1 1]) 
                        (add-ball :white [1 2 1]) 
                        (add-ball :black [1 1 2])
                        (add-ball :black [1 2 2]))]
      (is (= :open (cell four-test [2 1 1]))))))

(deftest has-square-test
  (testing "Has square"
    (is (has-square four-square-test [1 1 1]))
    (is (false? (has-square four-square-test [1 2 1])))
    (is (false? (has-square four-square-test [1 1 2])))
    (is (false? (has-square four-square [1 3 1])))))

(deftest all-positions-test
  (testing "All positions"
    (is (= (all-positions four 1) #{[1 1 1] [1 1 2] [1 1 3] [1 1 4]
                                    [1 2 1] [1 2 2] [1 2 3] [1 2 4]
                                    [1 3 1] [1 3 2] [1 3 3] [1 3 4]
                                    [1 4 1] [1 4 2] [1 4 3] [1 4 4]}))
    (is (= (all-positions four 2) #{[2 1 1] [2 1 2] [2 1 3]
                                    [2 2 1] [2 2 2] [2 2 3]
                                    [2 3 1] [2 3 2] [2 3 3]}))
    (is (= (all-positions four 3) #{[3 1 1] [3 1 2]
                                    [3 2 1] [3 2 2]}))
    (is (= (all-positions four 4) #{[4 1 1]}))))

(deftest can-remove-test
  (testing "Can remove ball"
    (is (false? (can-remove-ball two-layers-test [1 1 1])))
    (is (can-remove-ball two-layers-test [1 4 1]))))

(deftest rise-candidates-test
  (testing "Rise candidates"
    (is (= (rise-candidates four-square-test :white) #{{:low-position [1 2 3] :high-position [2 1 1]}}))
    (is (= (rise-candidates four-square-test :black) #{}))
    (is (= (rise-candidates (-> four-square-test 
                                (add-ball :black [1 3 1]) 
                                (add-ball :black [1 3 2])) :black) #{{:low-position [1 1 1] :high-position [2 2 1]}
                                                                     {:low-position [1 1 2] :high-position [2 2 1]}
                                                                     {:low-position [1 3 1] :high-position [2 1 1]}
                                                                     {:low-position [1 3 2] :high-position [2 1 1]}}))
    (is (= (rise-candidates (-> four-square-test 
                                (add-ball :black [1 3 1]) 
                                (add-ball :black [1 3 2])) :white) #{{:low-position [1 2 3] :high-position [2 1 1]}
                                                                     {:low-position [1 2 3] :high-position [2 2 1]}}))
    (is (= (rise-candidates two-layers-test :black) #{{:low-position [1 4 1] :high-position [3 1 1]}}))
    (is (= (rise-candidates two-layers-test :wite) #{}))))

(deftest removable-candidates-test
  (testing "Removable candidates"
    (is (= #{[2 2 2] [1 4 1] [2 1 2]} (removable-candidates two-layers-test :black)))
    (is (= #{[2 2 1] [2 1 1]} (removable-candidates two-layers-test :white)))))

(defn equal-with-meta [a b]
  (and (= a b)
       (= (meta a) (meta b))))

(deftest add-remove-test
  (testing "Add remove test"
    (is (equal-with-meta four (-> four
                                  (add-ball    :black [1 1 1])
                                  (remove-ball :black [1 1 1]))))
    (is (equal-with-meta four (-> four
                                  (add-ball    :black [1 1 1])
                                  (add-ball    :black [1 2 1])
                                  (add-ball    :black [1 1 2])
                                  (add-ball    :black [1 2 2])
                                  (remove-ball :black [1 1 1])
                                  (remove-ball :black [1 2 1])
                                  (remove-ball :black [1 1 2])
                                  (remove-ball :black [1 2 2]))))))

(deftest full-squares-test
  (testing "Full squares"
    (is (= #{[1 1 1]} (full-squares (-> four
                                        (add-ball    :black [1 1 1])
                                        (add-ball    :black [1 2 1])
                                        (add-ball    :black [1 1 2])
                                        (add-ball    :black [1 2 2])) :black)))
    (is (= #{} (full-squares (-> four
                                 (add-ball    :black [1 1 1])
                                 (add-ball    :black [1 2 1])
                                 (add-ball    :black [1 1 2])
                                 (add-ball    :black [1 2 2])) :white)))
    (is (= #{} (full-squares (-> four
                                 (add-ball    :black [1 1 1])
                                 (add-ball    :black [1 2 1])
                                 (add-ball    :black [1 1 2])
                                 (add-ball    :black [1 2 2])
                                 (remove-ball :black [1 1 1])) :black)))))

(deftest next-player-test
  (testing "Next player"
    (is (= :black (next-player {:board full-board-test :player :black})))
    (is (= :black (next-player {:board full-board-test :player :white})))))

(deftest score-test
  (testing "Score"
    (is (= 1  (score-for-player four-square-test :black)))
    (is (= -1 (score-for-player four-square-test :white)))))

(deftest is-full-square-test
  (testing "Is full square"
    (is (is-full-square (-> four 
                            (add-ball :white [1 1 1]) 
                            (add-ball :white [1 1 2])
                            (add-ball :white [1 2 1]) 
                            (add-ball :white [1 2 2])) [1 1 1] :white))
    (is (false? (is-full-square (-> four 
                                    (add-ball :white [1 1 1]) 
                                    (add-ball :white [1 1 2])
                                    (add-ball :white [1 2 1]) 
                                    (add-ball :black [1 2 2])) [1 1 1] :white)))))

(deftest full-square-on-top
  (testing "Full square on top works"
    (is (= 10 (count (moves {:board full-board-square-top :player :white}))))))
