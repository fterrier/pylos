(ns server.game-runner-test
  (:require [clojure.core.async :refer [<!! >!! alt! alt!! chan close! go tap timeout]]
            [clojure.test :refer [deftest is testing]]
            [pylos
             [game :refer [new-pylos-game]]
             [move :refer [move-add]]
             [score :refer [score-middle-blocked]]]
            [server.game-runner
             :refer
             [game-runner
              start-game-runner
              ->NewGameCommand
              ->StartGameCommand
              ->PlayerMoveCommand
              ->LeaveGameCommand
              ->StopGameCommand
              ->JoinGameCommand
              ->SubscribeCommand
              ->UnsubscribeCommand]]
            [strategy
             [channel :refer [channel]]
             [negamax :refer [negamax]]]))

(defn new-game-command [client game first-player]
  (->NewGameCommand client game first-player))

(defn start-game-command [client game-id]
  (->StartGameCommand client game-id))

(defn stop-game-command [client game-id]
  (->StopGameCommand client game-id))

(defn join-game-command [client user game-id color channel-key]
  (->JoinGameCommand client user game-id color channel-key))

(defn leave-game-command [client user game-id]
  (->LeaveGameCommand client user game-id))

(defn subscribe-game-command [client game-id]
  (->SubscribeCommand client game-id))

(defn unsubscribe-game-command [client game-id]
  (->UnsubscribeCommand client game-id))

(defn player-move-command [client user game-id color input]
  (->PlayerMoveCommand client user game-id color input))

(defn new-client [id]
  (let [output-ch         (chan)
        client            {:id id :channel output-ch}]
    [output-ch client]))

(defn new-user [id]
  {:id id})

(defn new-game [gamerunner-ch client]
  (>!! gamerunner-ch (new-game-command client (new-pylos-game 4) :white))
  (<!! (:channel client)))

(defn subscribe-to-game [gamerunner-ch client game-id]
  (>!! gamerunner-ch (subscribe-game-command client game-id)))

(defn unsubscribe-from-game [gamerunner-ch client game-id]
  (>!! gamerunner-ch (unsubscribe-game-command client game-id)))

(defn start-game [gamerunner-ch client game-id]
  (>!! gamerunner-ch (start-game-command client game-id)))

(defn stop-game [gamerunner-ch client game-id]
  (>!! gamerunner-ch (stop-game-command client game-id)))

(defn join-game [gamerunner-ch client user game-id color channel-key]
  (>!! gamerunner-ch (join-game-command client user game-id color channel-key)))

(defn leave-game [gamerunner-ch client user game-id]
  (>!! gamerunner-ch (leave-game-command client user game-id)))

(defn play-move [gamerunner-ch client user game-id color input]
  (>!! gamerunner-ch (player-move-command client user game-id color input)))

(defn new-game-runner-test []
  (let [gamerunner-ch (chan)
        game-runner   (game-runner gamerunner-ch)]
    (start-game-runner game-runner)
    [game-runner gamerunner-ch]))
    
    ;; game-id          (new-game gamerunner-ch (new-pylos-game 4)
    ;;                                 {:white negamax-strategy :black negamax-strategy} :white)
    ;; [game-runner game-id]


(defn- get-game [game-runner game-id]
  (get-in @(:games game-runner) [:games game-id]))

;; (deftest new-game-test
;;   (testing "New game adds playable game"
;;     (let [game (get-game game-runner game-id)]
;;       (is (not (nil? game)))
;;       (is (false? (:started game))))))

(deftest new-game-test
  (testing "New game adds playable game"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          {:keys [game-id]} (new-game gamerunner-ch client)
          game              (get-game game-runner game-id)]
      (is (not (nil? game)))
      (is (false? (:started game)))
      (start-game gamerunner-ch client game-id)
      ;; we leave some time for the game runner to process that
      (<!! (timeout 10))
      (is (:started (get-game game-runner game-id)))))
  (testing "New game times out after a while"
                                        ; TODO
    ))

(deftest stop-game-test
  (testing "New game can be stopped and will free resources"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (stop-game gamerunner-ch client game-id)
      ;; we leave some time for the game runner to process that
      (<!! (timeout 100))
      (is (nil? (get-game game-runner game-id)))))

  (testing "New game can be stopped and data will be removed"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (join-game gamerunner-ch client user game-id :white :encoded)
      (stop-game gamerunner-ch client game-id)
      ;; we leave some time for the game runner to process that
      (<!! (timeout 10))
      (is (nil? (get-in @(:games game-runner) [:users 123]))))))

(deftest start-game-test
  (testing "Starting game twice does nothing"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (is (false? (:started (get-game game-runner game-id))))
      (start-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (is (:started (get-in @(:games game-runner) [:games game-id])))
      (start-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (is (:started (get-in @(:games game-runner) [:games game-id]))))))

(deftest join-game-test
  (testing "Joining game adds data"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (join-game gamerunner-ch client user game-id :white :channel)
      (<!! (timeout 10))
      (is (= {:white {123 :channel}} (:joined-user-ids (get-game game-runner game-id))))
      (is (= {:games #{game-id}} (get-in @(:games game-runner) [:users 123])))))
  
  (testing "Leaving game removes data"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (join-game gamerunner-ch client user game-id :white :channel)
      (<!! (timeout 10))
      (leave-game gamerunner-ch client user game-id)
      (<!! (timeout 10))
      (is (= {:white {} :black nil} (:joined-user-ids (get-game game-runner game-id))))
      (is (nil? (get-in @(:games game-runner) [:users 123]))))))

(deftest subscribe-to-game-test 
  (testing "Subscribing to game adds data"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (subscribe-to-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (is (not (nil? (get-in @(:games game-runner) [:clients 1 game-id]))))))

  (testing "Subscribing to game sends past game infos"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (is (= :timeout (alt!!
                        (timeout 100) :timeout
                        output-ch     :move)))
      (subscribe-to-game gamerunner-ch client game-id)
      (is (= :msg/past-game-infos (:type (<!! output-ch))))))
  
  (testing "Unsubscribing from game frees resources"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch-1 client1] (new-client 1)
          [output-ch-2 client2] (new-client 2)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client1)]
      (subscribe-to-game gamerunner-ch client1 game-id)
      (subscribe-to-game gamerunner-ch client2 game-id)
      (<!! (timeout 10))
      (is (<!! output-ch-1))
      (is (<!! output-ch-2))
      (let [game-output-ch-1        (get-in @(:games game-runner) [:clients 1 game-id])
            game-output-ch-2        (get-in @(:games game-runner) [:clients 2 game-id])]
        (start-game gamerunner-ch client1 game-id)
        (<!! (timeout 10))
        (is (<!! output-ch-1))
        (is (<!! output-ch-2))
        (is (= :timeout (alt!!
                          (timeout 100) :timeout
                          output-ch-2 :move)))
        (unsubscribe-from-game gamerunner-ch client1 game-id)
        (<!! (timeout 10))
        (is (nil? (<!! game-output-ch-1)))
        (is (= :timeout (alt!!
                          (timeout 100) :timeout
                          game-output-ch-2 :move))))))

  (testing "Unsubscribing from game removes data"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (subscribe-to-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (unsubscribe-from-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (is (nil? (get-in @(:games game-runner) [:clients 1 game-id])))))
  
  (testing "Subscribing to gasme twice does not block"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (subscribe-to-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (subscribe-to-game gamerunner-ch client game-id)
      (<!! (timeout 10))
      (is (<!! output-ch)))))

(deftest play-move-test
  (testing "Playing move works and is saved"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (subscribe-to-game gamerunner-ch client game-id)
      (is (<!! output-ch))
      (start-game gamerunner-ch client game-id)
      (is (<!! output-ch))
      (join-game gamerunner-ch client user game-id :white :encoded)
      (play-move gamerunner-ch client user game-id :white 3)
      (is (= :white (get-in (<!! output-ch) [:game-infos :board 3])))
      (<!! (timeout 10))
      (is (= 2 (count (get-in @(:games game-runner) [:games game-id :past-game-infos]))))))
  
  (testing "Playing move on inexistant game"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)]
      (play-move gamerunner-ch client user "inexistant" :white 3)))
  
  (testing "Playing move when not your turn"
    (let [[game-runner gamerunner-ch] (new-game-runner-test)
          [output-ch client] (new-client 1)
          user               (new-user 123)
          {:keys [game-id]}  (new-game gamerunner-ch client)]
      (subscribe-to-game gamerunner-ch client game-id)
      (is (<!! output-ch))
      (start-game gamerunner-ch client game-id)
      (is (<!! output-ch))
      (join-game gamerunner-ch client user game-id :black :encoded)
      (play-move gamerunner-ch client user game-id :white 3)
      (is (= :timeout (alt!!
                        (timeout 100) :timeout
                        output-ch :move))))))

;; (deftest channel-stats-test
;;   (testing "Retrieving game runner stats"
;;     (let [[game-runner game-id] (new-game-runner-test)]
;;       (is (not (nil? (gamerunner-stats game-runner)))))))
