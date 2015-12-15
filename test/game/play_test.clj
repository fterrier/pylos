(ns game.play-test
  (:require [clojure.core.async :refer [<! <!! >!! alt!! chan close! go timeout]]
            [clojure.test :refer [deftest is testing]]
            [game
             [play :refer [play]]
             [strategy :refer [get-input-channel]]]
            [pylos
             [game :refer [new-pylos-game]]
             [score :refer [score-middle-blocked]]]
            [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]))

(deftest game-cancellation-test
  (testing "Can cancel game"
    (let [game      (new-pylos-game 4)
          negamax   (negamax score-middle-blocked 5)
          result-ch (play game {:white negamax :black negamax} :white)] 
      ; we take the first move out of the channel
      (<!! result-ch)
      ; we close the channel after 30 msecs
      (go (<! (timeout 30))
          (close! result-ch))
      ; we take the second move or timeout, we expect a nil result cause the channel is closed
      (let [result (alt!! result-ch     ([value] value)
                         (timeout 50) :timeout)]
        (is (nil? result)))
      (close! result-ch)))
  (testing "Result channels closed at end of game"
    ; TODO
    )
  (testing "Game input channels closed at end of game"
    ; TODO
    ))

(deftest move-test
  (testing "Does not accept stupid moves"
    (let [game       (new-pylos-game 4)
          negamax    (negamax score-middle-blocked 10)
          websockets (websockets)
          input-ch   (get-input-channel websockets)
          result-ch  (play game {:white negamax :black websockets} :black)]
      ; we take the first move out of the channel
      (<!! result-ch)
      ; we pass a stupid move to the game
      (>!! input-ch {:next-move "bullshit"})
      ; we take the result, there shouldn't be anything
      (is (= :timeout (alt!! result-ch :result
                             (timeout 50) :timeout)))
      ; we pass a real move to the game
      (>!! input-ch {:next-move {:type :add :position 0 :color :black}})
      ; this time we should get something
      (is (= :result (alt!! result-ch :result
                            (timeout 50) :timeout)))
      (close! result-ch)))

  (testing "Does not accept move from wrong player"
    (let [game      (new-pylos-game 4)
          negamax   (negamax score-middle-blocked 10)
          websockets (websockets)
          input-ch  (get-input-channel websockets)
          result-ch (play game {:white negamax :black websockets} :black)]
      ; we take the first move out of the channel
      (<!! result-ch)
      ; we pass a move from the wrong player to the game
      (>!! input-ch {:next-move {:type :add :position 0 :color :white}})
      ; we take the result, there shouldn't be anything
      (is (= :timeout (alt!! result-ch :result
                             (timeout 50) :timeout)))
      (close! result-ch))))

