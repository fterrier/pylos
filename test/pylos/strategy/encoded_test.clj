(ns pylos.strategy.encoded-test
  (:require [clojure.core.async :refer [<!! >!! alt!! close! timeout]]
            [clojure.test :refer [deftest is testing]]
            [game
             [play :refer [play play-game]]
             [strategy :refer [get-input-channel]]]
            [pylos
             [board :refer [cell]]
             [game :refer [->PylosGamePosition new-pylos-game]]
             [score :refer [score-middle-blocked]]]
            [pylos.strategy.encoded :refer [encoded]]
            [strategy.negamax :refer [negamax]]))

(deftest encoded-test
  (testing "Can play using simple move"
    (let [game       (new-pylos-game 4)
          negamax    (negamax score-middle-blocked 10)
          encoded    (encoded)
          input-ch   (get-input-channel encoded)
          result-ch  (play game {:white negamax :black encoded} :black)]
      ;; we take the first move out of the channel - it is the empty board
      (<!! result-ch)
      ;; we pass a simple move to the game
      (>!! input-ch 1)
      ;; this time we should get something
      (is (= :black (cell (:board (:game-position 
                                  (alt!! result-ch ([value _] value)
                                         (timeout 1000) :timeout))) 1)))
      (close! result-ch)))

  (testing "Does not crash with BS"
    (let [game       (new-pylos-game 4)
          negamax    (negamax score-middle-blocked 10)
          encoded    (encoded)
          input-ch   (get-input-channel encoded)
          result-ch  (play game {:white negamax :black encoded} :black)]
      ;; we take the first move out of the channel - it is the empty board
      (<!! result-ch)
      ;; we pass a simple move to the game
      (>!! input-ch "lskd")
      (is (= :timeout (alt!! result-ch ([value _] value)
                             (timeout 1000) :timeout)))
      (>!! input-ch 1)
      ;; this time we should get something
      (is (not= :timeout (alt!! result-ch ([value _] value)
                                (timeout 1000) :timeout)))
      (close! result-ch)))
  
  (testing "Can play rise"
    (let [game       {:game-position (->PylosGamePosition pylos.core-test/four-square-test :white nil)}
          negamax    (negamax score-middle-blocked 10)
          encoded    (encoded)
          input-ch   (get-input-channel encoded)
          result-ch  (play-game game {:white encoded :black negamax})]
      ;; we take the first move out of the channel - it is the empty board
      (<!! result-ch)
      ;; we pass a rise to the game
      (>!! input-ch 6)
      (is (= [6] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch 16)
      ;; this time we should get something
      (is (= :white (cell (:board (:game-position 
                                  (alt!! result-ch ([value _] value)
                                         (timeout 1000) :timeout))) 16)))
      (close! result-ch)))

  (testing "Can play rise in one move"
    (let [game       {:game-position (->PylosGamePosition pylos.core-test/four-square-test :white nil)}
          negamax    (negamax score-middle-blocked 10)
          encoded    (encoded)
          input-ch   (get-input-channel encoded)
          result-ch  (play-game game {:white encoded :black negamax})]
      ;; we take the first move out of the channel - it is the empty board
      (<!! result-ch)
      ;; we pass a rise to the game
      (>!! input-ch [6 16])
      ;; this time we should get something
      (is (= :white (cell (:board (:game-position 
                                  (alt!! result-ch ([value _] value)
                                         (timeout 1000) :timeout))) 16)))
      (close! result-ch)))
  
  (testing "Can play square"
    (let [game      {:game-position (->PylosGamePosition pylos.core-test/square-level2-test :white nil)}
          negamax   (negamax score-middle-blocked 2)
          encoded   (encoded)
          input-ch  (get-input-channel encoded)
          result-ch (play-game game {:white encoded :black negamax})]
      (<!! result-ch)
      ;; we pass a square
      (>!! input-ch 18)
      (is (= [18] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch 18)
      (is (= [18 18] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch 3)
      ;; this time we should get the square
      (is (= :white (cell (:board (:game-position 
                                  (alt!! result-ch ([value _] value)
                                         (timeout 1000) :timeout))) 16)))     
      (close! result-ch)))
  
  (testing "Can play square test"
    (let [game      {:game-position (->PylosGamePosition pylos.core-test/full-board-square-top :white nil)}
          negamax   (negamax score-middle-blocked 2)
          encoded   (encoded)
          input-ch  (get-input-channel encoded)
          result-ch (play-game game {:white encoded :black negamax})]
      (<!! result-ch)
      ;; we pass a square
      (>!! input-ch 28)
      (is (= [28] (:selected-positions (:game-position (<!! result-ch)))))
      ;; we try with an array
      (>!! input-ch [28])
      (is (= [28 28] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch 25)
      ;; this time we should get the square
      (is (= :open (cell (:board (:game-position (alt!! result-ch    ([value _] value)
                                                       (timeout 1000) :timeout))) 28)))     
      (close! result-ch)))
  
  (testing "Intermediate board"
    (let [game      {:game-position (->PylosGamePosition pylos.core-test/full-board-square-top :white nil)}
          negamax   (negamax score-middle-blocked 2)
          encoded   (encoded)
          input-ch  (get-input-channel encoded)
          result-ch (play-game game {:white encoded :black negamax})]
      (<!! result-ch)
      ;; we pass a square
      (>!! input-ch 28)
      (is (= :white (cell (:intermediate-board (:game-position (<!! result-ch))) 28)))
      (close! result-ch)))

  (testing "Can play square and stop in middle"
    (let [game      {:game-position (->PylosGamePosition pylos.core-test/full-board-square-top :white nil)}
          negamax   (negamax score-middle-blocked 2)
          encoded   (encoded)
          input-ch  (get-input-channel encoded)
          result-ch (play-game game {:white encoded :black negamax})]
      (<!! result-ch)
      ;; we pass a square
      (>!! input-ch 28)
      (is (= [28] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch 28)
      (is (= [28 28] (:selected-positions (:game-position (<!! result-ch)))))      
      (>!! input-ch :done)
      ;; this time we should get the square
      (is (= :open (cell (:board (:game-position (alt!! result-ch    ([value _] value)
                                                       (timeout 1000) :timeout))) 28)))
      (close! result-ch)))
  
  (testing "Can play square and stop in middle in one move"
    (let [game      {:game-position (->PylosGamePosition pylos.core-test/full-board-square-top :white nil)}
          negamax   (negamax score-middle-blocked 2)
          encoded   (encoded)
          input-ch  (get-input-channel encoded)
          result-ch (play-game game {:white encoded :black negamax})]
      (<!! result-ch)
      ;; we pass a square
      (>!! input-ch 28)
      (is (= [28] (:selected-positions (:game-position (<!! result-ch)))))
      (>!! input-ch [28 :done])
      ;; this time we should get the square
      (let [game-position (:game-position (alt!! result-ch    ([value _] value)
                                                 (timeout 1000) :timeout))]
        (is (= :white (cell (:board game-position) 27)))
        (is (= :open (cell (:board game-position) 28))))
      (close! result-ch))))
