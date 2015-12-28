(ns system.game-test
  (:require [clojure.core.async :refer [<!! alt! alt!! chan close! go tap timeout]]
            [clojure.test :refer [deftest is testing]]
            [pylos
             [game :refer [new-pylos-game]]
             [move :refer [move-add]]
             [score :refer [score-middle-blocked]]]
            [strategy
             [channel :refer [channel]]
             [negamax :refer [negamax]]]
            [server.game-runner
             :refer
             [channel-stats
              join-game
              leave-all-games
              new-game
              game-runner
              player-move
              start-game
              stop-game]]))

(def test-system 
  )

(defn new-game-runner-test []
  (let [game-runner      (game-runner (chan))
        negamax-strategy (negamax score-middle-blocked 2)
        game-id          (new-game (:games game-runner) (new-pylos-game 4)
                                   {:white negamax-strategy :black negamax-strategy} :white)]
    [game-runner game-id]))

(defn new-game-runner-websockets-test []
  (let [game-runner      (game-runner (chan))
        negamax-strategy (negamax score-middle-blocked 2)
        channel-strategy (channel)
        game-id          (new-game (:games game-runner) (new-pylos-game 4)
                                   {:white channel-strategy :black negamax-strategy} :white)]
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

(deftest start-game-test
  (testing "Starting game twice does nothing"
    (let [[game-runner game-id] (new-game-runner-test)
          game                  (get-in @(:games game-runner) [:games game-id])]
      (is (false? (:started game)))
      (start-game (:games game-runner) game-id)
      (is (:started (get-in @(:games game-runner) [:games game-id])))
      (start-game (:games game-runner) game-id)
      (is (:started (get-in @(:games game-runner) [:games game-id]))))))

(deftest join-game-test
  (testing "Joining game "
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch             (join-game (:games game-runner) {:id 123} game-id :white)
          game                  (get-in @(:games game-runner) [:games game-id])]
      (start-game (:games game-runner) game-id)
      (is (not (nil? (<!! output-ch))))
      (is (= {123 {:color :white}} (:joined-user-ids game)))
      (is (= {game-id {:output-ch output-ch}} (get-in @(:games game-runner) [:user-ids 123])))))

  (testing "Leaving game frees resources"
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch-1           (join-game (:games game-runner) {:id 1} game-id :white)
          output-ch-2           (join-game (:games game-runner) {:id 2} game-id :white)]
      (is (= {2 {:color :white} 1 {:color :white}} (get-in @(:games game-runner) [:games game-id :joined-user-ids])))
      (start-game (:games game-runner) game-id)
      (is (<!! output-ch-2))
      (is (= :timeout (alt!!
                        (timeout 100) :timeout
                        output-ch-2 :move)))
      (is (<!! output-ch-1))
      (leave-all-games (:games game-runner) {:id 1})
      (is (nil? (<!! output-ch-1)))
      (is (nil? (get-in @(:games game-runner) [:user-ids 1])))
      (is (= :move (alt!!
                     (timeout 100) :timeout
                     output-ch-2 :move)))
      (is (= {2 {:color :white}} (get-in @(:games game-runner) [:games game-id :joined-user-ids])))))

  (testing "Joining already joined game does not block"
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch-1           (join-game (:games game-runner) {:id 1} game-id :white)
          output-ch-2           (join-game (:games game-runner) {:id 1} game-id :white)]
      (start-game (:games game-runner) game-id)
      (is (= output-ch-1 output-ch-2))
      (is (not (nil? (<!! output-ch-1)))))))

(deftest play-move-test
  (testing "Playing move"
    (let [[game-runner game-id] (new-game-runner-websockets-test)
          output-ch             (join-game (:games game-runner) {:id 1} game-id :white)
          all-done-ch           (chan)]
      (start-game (:games game-runner) game-id)
      (<!! output-ch)
      (go (is (= :white (get-in (alt! 
                                   (timeout 1000) :timeout
                                   output-ch ([move] move))
                                [:game-position :board 0])))
          (close! all-done-ch))
      (player-move (:games game-runner) game-id {:id 1} (move-add :white 0))
      (<!! all-done-ch)))
  
  (testing "Moves are saved in past moves"
    (let [[game-runner game-id] (new-game-runner-test)
          output-ch             (join-game (:games game-runner) {:id 1} game-id :none)]
      (start-game (:games game-runner) game-id)
      (<!! output-ch)
      (is (= 1 (count (get-in @(:games game-runner) [:games game-id :past-game-infos]))))))

  (testing "Playing move on inexistant game"
    (let [[game-runner game-id] (new-game-runner-websockets-test)]
      (player-move (:games game-runner) "inexistant" {:id 1} (move-add :white 0))))

  (testing "Playing move with uid of wrong color does nothing"
    (let [[game-runner game-id] (new-game-runner-websockets-test)
          output-ch             (join-game (:games game-runner) {:id 1} game-id :black)]
      (start-game (:games game-runner) game-id)
      (<!! output-ch)
      (player-move (:games game-runner) game-id {:id 1} (move-add :white 0))
      (is (= :timeout (alt!!
                        (timeout 100) :timeout
                        output-ch :move))))))

(deftest channel-stats-test
  (testing "Retrieving channel stats"
    (let [[game-runner game-id] (new-game-runner-test)]
      (is (not (nil? (channel-stats game-runner)))))))
