(ns pylos.move-test
  (:require [clojure.test :refer [deftest is testing]]
            [pylos
             [board :refer [add-ball ind]]
             [core-test :as core :refer [four]]
             [move :refer [calculate-next-moves generate-all-moves is-move-allowed move-add move-rise move-square]]]
            [pylos.init :refer [initialize-board-meta]]))

(deftest board-move-map-test
  (testing "All board-move-map test"
    (is (= (into #{} [(move-add :black (ind four [1 1 4]))
            (move-add :black (ind four [1 2 4]))
            (move-add :black (ind four [1 3 4]))
            (move-add :black (ind four [1 4 2]))
            (move-add :black (ind four [1 4 3]))
            (move-add :black (ind four [1 4 4]))
            (move-add :black (ind four [3 1 1]))
            (move-rise :black (ind four [1 4 1]) (ind four [3 1 1]))]) (into #{} (generate-all-moves {:board core/two-layers-test :player :black}))))
    (is (= (into #{} (let [original-move (move-add :white (ind four [3 2 2]))]
                       [(move-square original-move [(ind four [3 1 1])] (ind four [3 1 1]))
                        (move-square original-move [(ind four [3 1 2])] (ind four [3 1 1]))
                        (move-square original-move [(ind four [3 2 1])] (ind four [3 1 1]))
                        (move-square original-move [(ind four [3 2 2])] (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 1 1]) (ind four [3 1 2])} (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 1 1]) (ind four [3 2 1])} (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 1 1]) (ind four [3 2 2])} (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 1 2]) (ind four [3 2 1])} (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 1 2]) (ind four [3 2 2])} (ind four [3 1 1]))
                        (move-square original-move #{(ind four [3 2 1]) (ind four [3 2 2])} (ind four [3 1 1]))]))
           (into #{} (generate-all-moves {:board core/full-board-square-top :player :white}))))
    (is (= 18
           (count (into #{} (generate-all-moves {:board (initialize-board-meta [:open :open :open :open :black :white :black :open :black :white :white :open :white :black :white :open :no-acc :no-acc :no-acc :open :black :no-acc :black :black :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc] 4) :player :black})))))))

(deftest calculate-next-move-test
  (testing "Next move"
    (is (= [{:type :square :color :white :square-position (ind four [2 1 2]) :original-move {:type :add :color :white :position (ind four [2 1 3])} :positions [(ind four [2 1 3]) (ind four [1 1 4])]}
            {:type :square :color :white :square-position (ind four [2 1 2]) :original-move {:type :add :color :white :position (ind four [2 1 3])} :positions [(ind four [2 1 3])]}]
           (calculate-next-moves {:player :white :board core/square-level2-test} (ind four [2 1 3]))))))

(deftest is-allowed-move
  (testing "Move add allowed"
    (is (is-move-allowed four (move-add :black 0))
        (not (is-move-allowed four (move-add :black 16)))))
  (testing "Move square allowed"
    (is (not (is-move-allowed core/full-board-square-top (move-add :black 0))))
    (is (is-move-allowed core/full-board-square-top (move-square (move-add :white (ind four [3 2 2]))
                                                            [(ind four [3 1 1])] (ind four [3 1 1]))))
    (is (not (is-move-allowed core/full-board-square-top (move-square (move-add :white (ind four [3 2 2]))
                                                                 [(ind four [1 1 1])] (ind four [3 1 1]))))))
  (testing "Move rise allowed"
    (is (is-move-allowed (add-ball core/four-square-test :black 2) (move-rise :black 0 17)))
    (is (not (is-move-allowed (add-ball core/four-square-test :black 2) (move-rise :black 0 15)))))
  
  ; this throws an exception
  ;; (testing "Move bullshit not allowed"
  ;;   (is (not (is-move-allowed four {:test "bullshit"}))))
  )
