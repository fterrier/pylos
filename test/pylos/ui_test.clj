(ns pylos.ui-test
  (:require [clojure.test :refer [deftest is testing]]
            [game.game :refer [generate-moves]]
            [pylos
             [game :refer [map->PylosGamePosition]]
             [ui :refer [move-status]]]))

(deftest move-status-test
  (testing "Move status on squares when ordering matters"
    (let [move-status (move-status pylos.core-test/square-level2-test :white
                                   (generate-moves 
                                    (map->PylosGamePosition 
                                     {:board pylos.core-test/square-level2-test
                                      :player :white
                                      :outcome nil})))]
      (println move-status)
      (is (= 2 (count (-> move-status
                          (get [18 18])
                          :moves))))
      (is (not (nil? (-> move-status
                         (get [18 18])
                         :intermediate-board)))))))
