(ns pylos.game-test
  (:require [clojure.test :refer :all]
            [pylos.core-test :refer :all]
            [pylos.board :refer [ind cell can-remove-ball]]
            [pylos.game :refer :all]))

(deftest board-move-map-test
  (testing "All board-move-map test"
    (is (= (into #{} [(move-add :black (ind four [1 1 4]))
            (move-add :black (ind four [1 2 4]))
            (move-add :black (ind four [1 3 4]))
            (move-add :black (ind four [1 4 2]))
            (move-add :black (ind four [1 4 3]))
            (move-add :black (ind four [1 4 4]))
            (move-add :black (ind four [3 1 1]))
            (move-rise :black (ind four [1 4 1]) (ind four [3 1 1]))]) (into #{} (generate-all-moves {:board two-layers-test :player :black}))))
    (is (= (into #{} (let [original-move (move-add :white (ind four [3 2 2]))]
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
           (into #{} (generate-all-moves {:board full-board-square-top :player :white}))))))
