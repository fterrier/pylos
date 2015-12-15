(ns system.game
  (:require [clojure.core.async
             :as
             async
             :refer
             [<! >! chan close! go go-loop mult tap untap]]
            [clojure.walk :refer [postwalk]]
            [com.stuartsierra.component :as component]
            [game
             [board :refer [serialize-board]]
             [game :refer [other-color]]
             [play :refer [play]]
             [strategy :refer [get-input-channel]]]
            [pylos
             [game :refer [new-pylos-game]]
             [score :refer [score-middle-blocked]]]
            [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]
            [system.websockets :refer [send-infos]]))

; private output stuff
(defn get-game-infos [{{:keys [board player outcome]} :game-position, move :last-move, additional-infos :additional-infos, time :time}]
  [:pylos/game-infos
   {:board            (serialize-board board)
    :next-player      player
    :move             move
    :time             time
    :additional-infos additional-infos}])

; game output API
(defn register-for-game-output [{:keys [websockets games]} output-ch uid]
  (println "GamePosition output - Registering for game output" uid)
  (go-loop []
    (let [result (<! output-ch)]
      (println "GamePosition output - Got result" uid result)
      (if (nil? result)
        (println "GamePosition output - GamePosition is over" uid)
        (do
          (send-infos websockets uid (get-game-infos result))
          (recur))))))

(defn notify-new-game [{:keys [websockets]} game-id uid]
  (println "GamePosition output - Notifying new game is created")
  (send-infos websockets uid [:pylos/new-game {:game-id game-id}]))

(defn notify-end-game [{:keys [websockets]} game-id uid]
  (println "GamePosition output - Notifying game is ended")
  (send-infos websockets uid [:pylos/end-game {:game-id game-id}]))


(defrecord GamePositionOutput [websockets])

(defn new-game-output []
  (map->GamePositionOutput {}))

(defn- random-string [length]
  (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

;; (defn- end-game [{:keys [games game-output]} game-id]
;;   "This ends the game and frees all associated resources."
;;   (let [game (get-in @games [:games game-id])
;;         uids (:joined-uids game)]
;;     (doseq [uid uids]
;;       (notify-end-game game-output game-id uid)
;;       (close! (:output-ch (get-in @games [:uids uid game-id]))))
;;     ; TODO end the game properly, this will cause the game to
;;     ; throw an exception
;;     (when (:result-ch game) (close! (:result-ch game)))
;;     (when (:game-ch game) (close! (:game-ch game))))
;;   (swap! games (fn [games] (-> games
;;                                (update :games dissoc game-id)
;;                                (update :uids #(->> %
;;                                                    (map (fn [[uid game-map]] [uid (dissoc game-map game-id)]))
;;                                                    (into {})))))))


(defn add-game [games game-id infos]
  (update-in games [:games game-id] merge (assoc infos :joined-uids #{})))

(defn remove-game [games game-id]
  (-> games
      (update-in [:games] dissoc game-id)))

(defn add-uid-to-game [games uid game-id {:keys [output-ch]}]
  (-> games
      (assoc-in [:uids uid game-id] {:output-ch output-ch})
      (update-in [:games game-id :joined-uids] conj uid)))

(defn remove-uid-from-game [games uid game-id]
  (-> games
      (update-in [:uids uid] dissoc uid)
      (update :games #(->> %
                           (map (fn [[game-id game-map]] [game-id (update game-map :joined-uids disj uid)]))
                           (into {})))))

(defn remove-uid [games uid]
  (-> games
      (update :uids dissoc uid)))

; precondition of this is that all uids have left
(defn stop-game [games game-id]
  (let [game (get-in @games [:games game-id])]
    (when-not (nil? game)
      (println "Game Runner - Stopping game")
      (close! (:result-ch game))
      (swap! games remove-game game-id))))

; game runner API
; TODO timeout in play.clj
(defn new-game [games game {:keys [white black] :as strategies} first-player]
  "This creates a new game."
  (let [game-id          (random-string 8)
        result-ch        (chan)
        result-mult-ch   (mult result-ch)]
    (println "Game Runner - Creating new game" game-id strategies game)
    (swap! games add-game game-id 
           {:result-ch result-ch
            :result-mult-ch result-mult-ch
            :strategies strategies
            :timestamp (System/currentTimeMillis)
            :first-player first-player
            :game game
            :started false})
    game-id))

(defn start-game [games game-id]
  (let [game (get-in @games [:games game-id])]
    (when (and (not (nil? game))
               (not (:started game)))
      (println "Game Runner - Starting game" game-id)
      (swap! games assoc-in [:games game-id :started] true)
      (play (:game game) (:strategies game) (:first-player game) (:result-ch game)))))

(defn player-move [games game-id player move]
  (println "Game Runner - Handling player move" game-id player move)
  (let [game (get-in @games [:games game-id])]
    (if (nil? game)
      ; TODO handle game channel not found - retrieve game from persistence layer ?
      (println "Game runner - Game not found" game-id )
      (let [strategy (get-in game [:strategies player])
            game-ch  (get-input-channel strategy)]
        (println "Game Runner - " game-ch strategy)
        (if (nil? game-ch)
          (println "Game Runner - No game input channel found for this player" game-id player)
          (do 
            (println "Game runner - Sending move to game channel" move)
            (go (>! game-ch {:next-move move}))))))))

(defn leave-all-games [games uid]
  "Frees resources associated to that game and player and stops notifying that
  player of moves for that game."
  (println "Game Runner - Handling leaving client" uid)
  (let [games-for-uid (get-in @games [:uids uid])]
    (doseq [[game-id channels] games-for-uid]
      (when-let [result-mult-ch (get-in @games [:games game-id :result-mult-ch])]
        (untap result-mult-ch (:output-ch channels)))
      (when (:output-ch channels) (close! (:output-ch channels)))
      (swap! games remove-uid-from-game uid game-id))
    (swap! games remove-uid uid)))

(defn join-game [games uid game-id]
  (let [game (get-in @games [:games game-id])]
    (if (and game (not (contains? (:joined-uids game) uid)))
      (let [output-ch (chan)]
        (tap (:result-mult-ch game) output-ch)
        (println "Game Runner - Joining game" game-id)
        (swap! games add-uid-to-game uid game-id {:output-ch output-ch})
        output-ch)
      (get-in @games [:uids uid game-id :output-ch]))))

; the handle-* methods parse the game message, do something
; then answer with a message
(defn handle-join-game [{:keys [games game-output]} uid {:keys [game-id]}]
  "Start notifying the given uid of the moves for the given game-id."
  ; TODO what happens if the game is joined en-route ? we should send the old moves
  (let [output-ch (join-game games uid game-id)]
    (when output-ch (register-for-game-output game-output output-ch uid))))

(defn handle-new-game [{:keys [games game-output] :as game-runner} uid {game-name :game-name {:keys [white black]} :strategies first-player :first-player}]
  "Returns a new game id "
  (let [; TODO parse those 3 following variables from the map
        websockets-color :white
        negamax-depth    5
        game             (new-pylos-game 4)

        strategies {websockets-color               (websockets)
                    (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
        game-id (new-game games game strategies first-player)]
    (println "Game runner - Handling new game with id" game-id)
    (notify-new-game game-output game-id uid)
    (join-game games uid game-id)
    (start-game games game-id)))

(defn handle-player-move [{:keys [games]} uid {:keys [game-id player game-infos]}]
  (player-move games game-id player (:move game-infos)))

; TODO pipe this through ? transform to message with protocol ?
; TODO 1. parse 2. execute 3. answer
(defmulti handle-websockets-msg (fn [_ {:keys [type]}] type))
(defmethod handle-websockets-msg :pylos/player-move [game-runner {:keys [uid message]}]
  (handle-player-move game-runner uid message))
(defmethod handle-websockets-msg :pylos/new-game [game-runner {:keys [uid message]}]
  (handle-new-game game-runner uid message))
(defmethod handle-websockets-msg :pylos/join-game [game-runner {:keys [uid message]}]
  (handle-join-game game-runner uid message))
(defmethod handle-websockets-msg :chsk/uidport-close [game-runner {:keys [uid]}]
  (leave-all-games (:games game-runner) uid))

(defmethod handle-websockets-msg :default [game-runner data]
  ; nothing
  )

(defn start-game-runner [{:keys [websockets-ch games] :as game-runner}]
  (go-loop []
    ; TODO exception handling and scalability here we can let a lot of workers on this
    (when-let [websockets-msg (<! websockets-ch)]
      (println "GamePosition runner - Got message from websocket" websockets-msg)
      ; this is not allowed to fail
      (try
        (handle-websockets-msg game-runner websockets-msg)
        (catch Exception e (println e)))
      (recur)))
  game-runner)

(defn stop-game-runner [{:keys [games]}]
  ; (doseq [[_ game-ch] (deref channels)]
  ; (close! game-ch))
  )

(defn channel-stats [{:keys [games]}]
  (postwalk (fn [el] (cond (keyword? el) el 
                           (coll? el) el 
                           :else (str el))) @games))

(defrecord GameRunner [websockets-ch games game-output]
  component/Lifecycle
  (start [component]
    (start-game-runner component))
  (stop [component]
    ; TODO this should not be here
    (close! websockets-ch)
    (stop-game-runner component)
    component))

(defn new-game-runner []
  (map->GameRunner {:games (atom {:games {} :uids {}})}))
