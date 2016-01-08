(ns strategy.multi-test
  (:require [clojure.core.async :refer [<! <!! >! alt!! go timeout]]
            [clojure.test :refer [deftest is testing]]
            [game
             [game :refer [initial-game-position]]
             [strategy :refer [choose-next-move get-input-channel notify-end-game]]]
            [pylos
             [game :refer [new-pylos-game]]
             [move :refer [move-add]]]
            [strategy
             [channel :refer [channel]]
             [multi :refer [add-strategies multi-channel]]]))

(defn- new-initial-game-position []
  (initial-game-position (new-pylos-game 4) :white))

(deftest multi-strategy-test
  (testing "Receives move with simple strategy"
    (let [multi         (multi-channel)
          game-position (new-initial-game-position)
          channel       (channel)
          input-ch      (get-input-channel channel)]
      ;; we add the strategy to the multi
      (add-strategies multi {:channel channel})
      ;; we put a move on the input channel
      (go (>! input-ch (move-add :white 9)))
      ;; we wait for the result on the multi strategy
      (is (= {:type :add :position 9 :color :white} 
             (alt!! (choose-next-move multi game-position) ([value _] value)
                    (timeout 100) :timeout)))))

  (testing "Receives move with strategy added later"
    (let [multi         (multi-channel)
          game-position (new-initial-game-position)
          channel       (channel)
          input-ch      (get-input-channel channel)]
      ;; we put a move on the input channel
      (go (>! input-ch (move-add :white 9)))
      ;; we add the channel to the multi after 50 ms
      (go (<! (timeout 50))
          (add-strategies multi {:channel channel}))

      ;; we wait for the result on the multi-strategy
      (is (= {:type :add :position 9 :color :white} 
             (alt!! (choose-next-move multi game-position) ([value _] value)
                    (timeout 100) :timeout)))))
  
  (testing "Closes strategy closes all others"
    (let [multi         (multi-channel)
          game-position (new-initial-game-position)
          channel       (channel)
          input-ch      (get-input-channel channel)]
      (add-strategies multi {:channel channel})
      ;; we put a move on the channel
      (go (>! input-ch (move-add :white 9)))
      ;; we close the strategy
      (notify-end-game multi)
      ;; chan
      (is (nil? (<!! (choose-next-move multi game-position))))
      (is (nil? (<!! (choose-next-move channel game-position)))))))
