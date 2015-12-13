(ns system.game-test
  (:require [clojure.core.async :refer [<!! alt! alt!! chan close! go tap timeout]]
            [clojure.test :refer [deftest is testing]]
            [pylos
             [game :refer [new-pylos-game]]
             [move :refer [move-add]]
             [score :refer [score-middle-blocked]]]
            [strategy.negamax :refer [negamax]]
            [system.game
             :refer
             [channel-stats
              join-game
              leave-all-games
              new-game
              new-game-runner
              player-move
              start-game
              stop-game]]
            [system.strategy.websockets :refer [websockets]]))

(def test-system 
  )

(defn new-game-runner-test []
  (let [game-runner      (new-game-runner)
        negamax-strategy (negamax score-middle-blocked 2)
        game-id          (new-game (:games game-runner) (new-pylos-game 4)
                                   {:white negamax-strategy :black negamax-strategy} :white)]
    [game-runner game-id]))

(defn new-game-runner-websockets-test []
  (let [game-runner      (new-game-runner)
        negamax-strategy (negamax score-middle-blocked 2)
        websockets-strategy (websockets)
        game-id          (new-game (:games game-runner) (new-pylos-game 4)
                                   {:white websockets-strategy :black negamax-strategy} :white)]
    [game-runner game-id]))


(deftest new-game-test
  (testing "New game adds playable game"
    (let [[game-runner game-id] (new-game-runner-test)
          game             (get-in @(:games game-runner) [:games game-id])
          output-ch        (chan)
          output-ch-tap    (tap (:result-mult-ch game) output-ch)]
      (is (not (nil? game)))
      (is (false? (:started game)))
      (start-game (:games game-runner) game-id)
      (is (:started (get-in @(:games game-runner) [:games game-id])))
      (is (not (nil? (<!! output-ch))))))
  (testing "New game times out after a while"
                                        ; TODO
    )
  (testing "New game can be stopped and will free resources"
    (let [[game-runner game-id] (new-game-runner-test)
          game                  (get-in @(:games game-runner) [:games game-id])]
      (stop-game (:games game-runner) game-id)
      (is (nil? (get-in @(:games game-runner) [:games game-id])))
      (is (nil? (<!! (:result-ch game)))))))


(deftest join-game-test
  (testing "Joining game "
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch             (join-game (:games game-runner) 123 game-id)
          game                  (get-in @(:games game-runner) [:games game-id])]
      (start-game (:games game-runner) game-id)
      (is (not (nil? (<!! output-ch))))
      (is (= #{123} (:joined-uids game)))
      (is (= {game-id {:output-ch output-ch}} (get-in @(:games game-runner) [:uids 123])))))

  (testing "Leaving game frees resources"
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch-1           (join-game (:games game-runner) 1 game-id)
          output-ch-2           (join-game (:games game-runner) 2 game-id)]
      (start-game (:games game-runner) game-id)
      (<!! output-ch-2)
      (is (= :timeout (alt!!
                        (timeout 10) :timeout
                        output-ch-2 :move)))
      (<!! output-ch-1)
      (leave-all-games (:games game-runner) 1)
      (is (= :move (alt!!
                     (timeout 100) :timeout
                     output-ch-2 :move)))
      (is (nil? (get-in @(:games game-runner) [:uids 1])))
      (is (= #{2} (get-in @(:games game-runner) [:games game-id :joined-uids])))))

  (testing "Joining already joined game does not block"
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch-1           (join-game (:games game-runner) 1 game-id)
          output-ch-2           (join-game (:games game-runner) 1 game-id)]
      (start-game (:games game-runner) game-id)
      (is (= output-ch-1 output-ch-2))
      (is (not (nil? (<!! output-ch-1)))))))

(deftest play-move-test
  (testing "Playing move"
    (let [[game-runner game-id] (new-game-runner-websockets-test)
          output-ch             (join-game (:games game-runner) 1 game-id)
          all-done-ch           (chan)]
      (start-game (:games game-runner) game-id)
      (<!! output-ch)
      (go (is (= :white (get-in (alt! 
                                   (timeout 1000) :timeout
                                   output-ch ([move] move))
                                [:game-position :board 0])))
          (close! all-done-ch))
      (player-move (:games game-runner) game-id :white (move-add :white 0))
      (<!! all-done-ch)))

  (testing "Playing move on inexistant game"
    (let [[game-runner game-id] (new-game-runner-websockets-test)]
        (player-move (:games game-runner) "inexistant" :white (move-add :white 0)))))

(deftest channel-stats-test
  (testing "Retrieving channel stats"
    (let [[game-runner game-id] (new-game-runner-test)]
      (is (not (nil? (channel-stats game-runner)))))))
