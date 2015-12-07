(ns system.game
  (:require
    [clojure.core.async :as async :refer [pub chan close! go-loop go >! <! mult tap untap]]
    [system.strategy.websockets :refer [websockets]]
    [system.websockets :refer [send-infos]]
    [strategy.negamax :refer [negamax]]
    [pylos.score :refer [score-middle-blocked]]
    [pylos.game :refer [new-pylos-game]]
    [game.game :refer [other-color]]
    [game.play :refer [play]]
    [game.board :refer [serialize-board]]
    [com.stuartsierra.component :as component]))


; private output stuff
(defn get-game-infos [{{:keys [board player outcome]} :game-position, move :last-move, additional-infos :additional-infos, time :time}]
  [:pylos/game-infos
     {:board (serialize-board board)
      :next-player player
      :move move
      :time time
      :additional-infos additional-infos}])

; game output API
(defn register-for-game-output [{:keys [websockets games]} output-ch uid]
  (println "GamePosition output - Registering for game output" uid)
  (go-loop []
    ; TODO make a multicast channel
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

; TODO make pure function ?
(defn end-game [{:keys [games game-output]} game-id]
  "This ends the game and frees all associated resources."
  (let [game (get-in @games [:games game-id])
        uids (:joined-uids game)]
    (doseq [uid uids]
      (notify-end-game game-output game-id uid)
      (close! (:output-ch (get-in @games [:uids uid game-id]))))
    ; TODO end the game properly, this will cause the game to
    ; throw an exception
    (when (:result-ch game) (close! (:result-ch game)))
    (when (:game-ch game) (close! (:game-ch game))))
  (swap! games (fn [games] (-> games
                               (update :games dissoc game-id)
                               (update :uids #(->> %
                                                   (map (fn [[uid game-map]] [uid (dissoc game-map game-id)]))
                                                    (into {})))))))


; game runner API
; TODO make pure function ?
; TODO make generic with strategies
; TODO make game and score configurable
(defn new-game [{:keys [games]} size websockets-color first-player negamax-depth]
  "This creates a new game."
  (let [; TODO where to close game-ch and result-ch channel if the game crashes
        ; TODO where to close game-ch and result-ch channel if the client disconnects ?
        ; TODO what to do when the game is over ?
        game-id          (random-string 8)
        result-ch        (chan)
        result-mult-ch   (mult result-ch)
        game-ch          (chan)
        negamax-strategy (negamax score-middle-blocked negamax-depth)]
      (play (new-pylos-game 4)
            {websockets-color (websockets game-ch nil)
            (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
            first-player result-ch)
    (swap! games update-in [:games game-id]
           assoc :result-ch result-ch
                 :result-mult-ch result-mult-ch
                 :game-ch game-ch
                 :joined-uids #{})
    game-id))

(defn handle-player-move [{:keys [games]} uid {:keys [game-id game-infos]}]
  (let [game-ch (:game-ch (get-in @games [:games game-id]))]
    (if (nil? game-ch)
      ; TODO handle game channel not found - retrieve game from persistence layer ?
      (println "GamePosition runner - GamePosition channel not found")
      (do
        (println "GamePosition runner - Sending game infos to game channel" game-infos)
        ; TODO validate player move
        (go (>! game-ch {:game-infos game-infos}))))))

(defn handle-leave-game [{:keys [games]} uid]
  "Frees resources associated to that game and player and stops notifying that
  player of moves for that game."
  (println "GamePosition runner - Handling leaving client" uid)
  (let [games-for-uid  (get-in @games [:uids uid])]
    (doseq [[game-id channels] games-for-uid]
      (when-let [result-mult-ch (get-in @games [:games game-id :result-mult-ch])]
        (println result-mult-ch)
        (untap result-mult-ch (:output-ch channels)))
      (when (:output-ch channels) (close! (:output-ch channels)))))
  (swap! games (fn [games] (-> games
                               (update :uids dissoc uid)
                               (update :games #(->> %
                                                    (map (fn [[game-id game-map]] [game-id (update game-map :joined-uids disj uid)]))
                                                    (into {})))))))

(defn handle-join-game [{:keys [games game-output]} uid {:keys [game-id]}]
  "Start notifying the given uid of the moves for the given game-id."
  ; TODO what happens if the game is joined en-route ? we should send the old moves
  (let [game (get-in @games [:games game-id])]
    (when (and game (not (contains? (:joined-uids game) uid)))
      (let [output-ch  (chan)
            output-tap (tap (:result-mult-ch game) output-ch)]
        (println "GamePosition runner - Joining game" game-id)
        (swap! games #(-> %
                          (assoc-in [:uids uid game-id] {:output-ch output-ch :output-tap output-tap})
                          (update-in [:games game-id :joined-uids] conj uid)))
        (register-for-game-output game-output output-ch uid)))))

(defn handle-new-game [{:keys [game-output] :as game-runner} uid {:keys [websockets-color first-player negamax-depth]}]
  "Returns a new game id "
  (let [game-id (new-game game-runner 4 websockets-color first-player negamax-depth)]
    (println "GamePosition runner - Handling new game with id" game-id)
    (notify-new-game game-output game-id uid)))

; TODO pipe this through ? transform to message with protocol ?
(defmulti handle-websockets-msg (fn [_ {:keys [type]}] type))
(defmethod handle-websockets-msg :pylos/player-move [game-runner {:keys [uid message]}]
  (handle-player-move game-runner uid message))
(defmethod handle-websockets-msg :pylos/new-game [game-runner {:keys [uid message]}]
  (handle-new-game game-runner uid message))
(defmethod handle-websockets-msg :pylos/join-game [game-runner {:keys [uid message]}]
  (handle-join-game game-runner uid message))
(defmethod handle-websockets-msg :chsk/uidport-close [game-runner {:keys [uid]}]
  (handle-leave-game game-runner uid))

(defmethod handle-websockets-msg :default [game-runner data]
  ; nothing
  )

(defn start-game-runner [{:keys [websockets-ch games] :as game-runner}]
  (go-loop []
    ; TODO exception handling
    (when-let [websockets-msg (<! websockets-ch)]
      (println "GamePosition runner - Got message from websocket" websockets-msg)
      (handle-websockets-msg game-runner websockets-msg)
      (recur)))
  game-runner)

(defn stop-game-runner [{:keys [games]}]
  ; (doseq [[_ game-ch] (deref channels)]
    ; (close! game-ch))
  )

; TODO use walk
(defn channel-stats [{:keys [games]}]
  {:games
  (into {} (map (fn [[game-id game]] [game-id
                                      (assoc game
                                             :result-ch (some? (:result-ch game))
                                             :result-mult-ch (some? (:result-mult-ch game))
                                             :game-ch (some? (:game-ch game))
                                             )]) (:games @games)))
   :uids
   (into {} (map (fn [[uid games]] [uid (into {} (map (fn [[game-id infos]] [game-id (assoc infos
          :output-ch (some? (:output-ch infos))
          :output-tap (some? (:output-tap infos)))]) games))]) (:uids @games)))})

(defrecord GamePositionRunner [websockets-ch games game-output]
  component/Lifecycle
  (start [component]
    (start-game-runner component))
  (stop [component]
    ; TODO this should not be here
    (close! websockets-ch)
    (stop-game-runner component)
    component))

(defn new-game-runner []
  (map->GamePositionRunner {:games (atom {:games {} :uids {}})}))
