(ns system.game
  (:require [clojure.core.async
             :as
             async
             :refer
             [<! >! chan close! go go-loop mult tap untap]]
            [clojure.walk :refer [postwalk]]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [game
             [board :refer [serialize-board]]
             [play :refer [play]]
             [strategy :refer [get-input-channel]]]))

; private output stuff
(defn get-game-infos [{{:keys [board player outcome]} :game-position, move :last-move, additional-infos :additional-infos, time :time}]
  {:board            (serialize-board board)
   :next-player      player
   :move             move
   :time             time
   :additional-infos additional-infos})

; TODO summarize these 4 in 1 send-message method
; TODO abstract the output message
(defn send-game-infos [user game-id game-infos]
  (log/debug "Game output - Sending game infos")
  ((:send-message user) [:msg/game-infos {:game-id game-id :game-infos (get-game-infos game-infos)}]))

(defn send-past-game-infos [user game-id past-game-infos]
  (log/debug "Game output - Sending past game infos")
  ((:send-message user) [:msg/past-game-infos {:game-id game-id :past-game-infos (into [] (map get-game-infos past-game-infos))}]))

(defn notify-new-game [user game-id]
  (log/info "Game output - Notifying new game is created")
  ((:send-message user) [:msg/new-game {:game-id game-id}]))

(defn notify-end-game [user game-id]
  (log/debug "Game output - Notifying game is ended")
  ((:send-message user) [:msg/end-game {:game-id game-id}]))

; game output API
; TODO replace this by a pipe to the output channel of that user
; then we probably don't need to keep the :user-ids map any more in the state atom
(defn register-for-game-output [user game-id output-ch]
  (log/debug "Game output - Registering for game output" user)
  (go-loop []
    (let [result (<! output-ch)]
      (log/debug "Game output - Got result" user result)
      (if (nil? result)
        (log/debug "Game output - Game is over" user)
        (do
          (send-game-infos user game-id result)
          (recur))))))

(defn- random-string [length]
  (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(defn add-game [games game-id infos]
  (update-in games [:games game-id] merge (assoc infos :joined-user-ids {})))

(defn remove-game [games game-id]
  (-> games
      (update-in [:games] dissoc game-id)))

(defn add-user-to-game [games user-id game-id color {:keys [output-ch]}]
  (-> games
      (assoc-in [:user-ids user-id game-id] {:output-ch output-ch})
      (update-in [:games game-id :joined-user-ids] assoc user-id {:color color})))

(defn remove-user-from-game [games user-id game-id]
  (-> games
      (update-in [:user-ids user-id] dissoc user-id)
      (update :games #(->> %
                           (map (fn [[game-id game-map]] 
                                  [game-id (update game-map :joined-user-ids dissoc user-id)]))
                           (into {})))))

(defn save-move-to-game [games game-id game-infos]
  (-> games
      (update-in [:games game-id :past-game-infos] conj game-infos)))

(defn remove-user [games user-id]
  (-> games
      (update :user-ids dissoc user-id)))

; precondition of this is that all user-ids have left
(defn stop-game [games game-id]
  (let [game (get-in @games [:games game-id])]
    (when-not (nil? game)
      (log/debug "Game Runner - Stopping game")
      (close! (:result-ch game))
      (swap! games remove-game game-id))))

; game runner API
; TODO timeout in play.clj
(defn new-game [games game {:keys [white black] :as strategies} first-player]
  "This creates a new game."
  (let [game-id          (random-string 8)
        result-ch        (chan 1 (map (fn [game-infos] 
                                        (swap! games save-move-to-game game-id game-infos)
                                        game-infos)))
        result-mult-ch   (mult result-ch)]
    (log/debug "Game Runner - Creating new game" game-id strategies game)
    (swap! games add-game game-id 
           {:result-ch result-ch
            :result-mult-ch result-mult-ch
            :strategies strategies
            :timestamp (System/currentTimeMillis)
            :first-player first-player
            :game game
            :past-game-infos []
            :started false})
    game-id))

(defn start-game [games game-id]
  (let [game (get-in @games [:games game-id])]
    (when (and (not (nil? game))
               (not (:started game)))
      (log/debug "Game Runner - Starting game" game-id)
      (swap! games assoc-in [:games game-id :started] true)
      (play (:game game) (:strategies game) (:first-player game) (:result-ch game)))))

(defn player-move [games game-id user move]
  (log/debug "Game Runner - Handling player move" game-id user move)
  (let [game   (get-in @games [:games game-id])
        player (get-in game [:joined-user-ids (:id user) :color])]
    (if (or (nil? game) (nil? player))
      ; TODO handle game channel not found - retrieve game from persistence layer ?
      (log/debug "Game runner - Game or player not found" game-id user)
      (let [strategy (get-in game [:strategies player])
            game-ch  (get-input-channel strategy)]
        (log/debug "Game Runner - " game-ch strategy)
        (if (nil? game-ch)
          (log/debug "Game Runner - No game input channel found for this player" game-id player)
          (do 
            (log/debug "Game runner - Sending move to game channel" move)
            (go (>! game-ch {:next-move move}))))))))

(defn leave-all-games [games user]
  "Frees resources associated to that game and player and stops notifying that
  player of moves for that game."
  (log/debug "Game Runner - Handling leaving client" user)
  (let [games-for-user (get-in @games [:user-ids (:id user)])]
    (doseq [[game-id channels] games-for-user]
      (when-let [result-mult-ch (get-in @games [:games game-id :result-mult-ch])]
        (untap result-mult-ch (:output-ch channels)))
      (when (:output-ch channels) (close! (:output-ch channels)))
      (swap! games remove-user-from-game (:id user) game-id))
    (swap! games remove-user (:id user))))

(defn join-game [games user game-id color]
  "Joins the game, subscribing to all game output, and also allows that
  player to play the given color, both if :both is given (TODO NOT SUPPORTED YET), or none for all other values"
  (let [game (get-in @games [:games game-id])]
    (if (and game (not (contains? (:joined-user-ids game) (:id user))))
      (let [output-ch (chan)]
        (tap (:result-mult-ch game) output-ch)
        (log/debug "Game Runner - Joining game" game-id)
        (swap! games add-user-to-game (:id user) game-id color {:output-ch output-ch})
        output-ch)
      (get-in @games [:user-ids (:id user) game-id :output-ch]))))

; the handle-* methods parse the game message, do something
; then answer with a message
(defn handle-join-game [{:keys [games]} user {:keys [game-id color]}]
  "Start notifying the given user of the moves for the given game-id.
   Sends the past moves so the game is updated and starts the game if not already started."
  (let [output-ch (join-game games user game-id color)]
    (when output-ch (register-for-game-output user game-id output-ch))
    (send-past-game-infos user game-id (get-in @games [:games game-id :past-game-infos]))
    ; we auto-start the game here
    (start-game games game-id)))

(defn handle-new-game [{:keys [games] :as game-runner} user {game :game strategies :strategies first-player :first-player}]
  "Returns a new game id "
  (let [game-id (new-game games game strategies first-player)]
    (log/debug "Game runner - Handling new game with id" game-id)
    (notify-new-game user game-id)))

(defn handle-player-move [{:keys [games]} user {:keys [game-id game-infos]}]
  (player-move games game-id user (:move game-infos)))

; no need to validate those messages as they should have been validated by the 
; readers, we could have them create an object which we can just call a method on
;; (defprotocol GameRunnerMessage
;;   (handle-message [this]))
;; (defrecord PlayerMoveMessage)

(defmulti handle-message (fn [_ {:keys [type]}] type))

(defmethod handle-message :player-move [game-runner {:keys [user message]}]
  (handle-player-move game-runner user message))

(defmethod handle-message :new-game [game-runner {:keys [user message]}]
  (handle-new-game game-runner user message))

(defmethod handle-message :join-game [game-runner {:keys [user message]}]
  (handle-join-game game-runner user message))

(defmethod handle-message :user-leave [game-runner {:keys [user message]}]
  (leave-all-games (:games game-runner) user))

(defmethod handle-message :default [game-runner data]
  ; nothing
  )

(defn start-game-runner [{:keys [gamerunner-ch games] :as game-runner}]
  (go-loop []
    ; TODO exception handling and scalability here we can let a lot of workers on this
    (when-let [message (<! gamerunner-ch)]
      (log/debug "Game runner - Got message from websocket" message)
      ; TODO this is not allowed to fail
      (try
        (handle-message game-runner message)
        (catch Exception e (log/debug e)))
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

(defrecord GameRunner [gamerunner-ch games]
  component/Lifecycle
  (start [component]
    (start-game-runner component))
  (stop [component]
    ; TODO this should not be here
    (close! gamerunner-ch)
    (stop-game-runner component)
    component))

(defn new-game-runner []
  (map->GameRunner {:games (atom {:games {} :user-ids {}})}))
