(ns server.game-runner
  (:require [clojure.core.async
             :as
             async
             :refer
             [<! >! chan close! go go-loop mult tap untap]]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [prewalk]]
            [compojure.core :refer [GET]]
            [game
             [play :refer [play]]
             [strategy :refer [get-input-channel]]]
            [pylos.strategy.encoded :refer [encoded]]
            [ring.middleware.json :refer [wrap-json-response]]
            [strategy
             [channel :refer [channel]]
             [multi :refer [add-strategies get-strategy multi-channel]]]))

; private output stuff
(defn get-game-infos [{{:keys [board player outcome intermediate-board]} :game-position, move :last-move, additional-infos :additional-infos, time :time}]
  {:board              board
   :intermediate-board intermediate-board
   :player             player
   :move               move
   :time               time
   :additional-infos   additional-infos})

;; (defprotocol User
;;   (id [this] "Get this user's uid")
;;   (channel [this] "Get a way to communicate to this user"))

(defn make-game-infos-msg [user game-id game-infos]
  {:type :msg/game-infos :user user :game-id game-id :game-infos (get-game-infos game-infos)})

(defn make-past-game-infos-msg [user game-id past-game-infos]
  {:type :msg/past-game-infos :user user :game-id game-id :past-game-infos (into [] (map get-game-infos past-game-infos))})

(defn make-notify-new-game-msg [user game-id]
  {:type :msg/new-game :user user :game-id game-id})

(defn make-notify-end-game-msg [user game-id]
  {:type :msg/end-game :user user :game-id game-id})
 
(defn register-for-game-output [user game-id output-ch]
  (log/debug "Game output - Registering for game output" user)
  (go-loop []
    (let [result (<! output-ch)]
      (log/debug "Game output - Got result" user result)
      (if (nil? result)
        (log/debug "Game output - Game is over" user)
        (do
          (go (>! (:channel user) (make-game-infos-msg user game-id result)))
          (recur))))))

(defn- random-string [length]
  (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(defn add-game [games game-id infos]
  (update-in games [:games game-id] merge (assoc infos :joined-user-ids {})))

(defn remove-game [games game-id]
  (-> games
      (update-in [:games] dissoc game-id)))

(defn add-user-to-game [games user-id game-id color channel-key {:keys [output-ch]}]
  (-> games
      (assoc-in [:user-ids user-id game-id] {:output-ch output-ch})
      (update-in [:games game-id :joined-user-ids] assoc user-id {:color color :channel channel-key})))

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
(defn new-game [games game strategies first-player]
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
      (log/debug "Starting game" game-id)
      (swap! games assoc-in [:games game-id :started] true)
      (play (:game game) (:strategies game) (:first-player game) (:result-ch game)))))

(defn player-move [games game-id user input]
  (log/debug "Handling player move" game-id user input)
  (let [game   (get-in @games [:games game-id])
        player (get-in game [:joined-user-ids (:id user)])]
    (if (or (nil? game) (nil? player))
      ; TODO handle game channel not found - retrieve game from persistence layer ?
      (log/debug "Game or player not found" game-id user)
      (let [multi   (get-in game [:strategies (:color player)])]
        (if (nil? multi)
          (log/debug "No game input channel found for this player" game-id player)
          (let [strategy (get-strategy multi (:channel player))]
            (if (nil? strategy)
              (log/debug "No strategy found for channel" player)
              (let [game-ch  (get-input-channel strategy)]                
                (log/debug "Sending move to game channel" input)
                (go (>! game-ch input))))))))))

(defn leave-all-games [games user]
  "Frees resources associated to that game and player and stops notifying that
  player of moves for that game."
  (log/debug "Handling leaving client" user)
  (let [games-for-user (get-in @games [:user-ids (:id user)])]
    (doseq [[game-id channels] games-for-user]
      (when-let [result-mult-ch (get-in @games [:games game-id :result-mult-ch])]
        (untap result-mult-ch (:output-ch channels)))
      (when (:output-ch channels) (close! (:output-ch channels)))
      (swap! games remove-user-from-game (:id user) game-id))
    (swap! games remove-user (:id user))))

(defn join-game [games user game-id color channel-key]
  "Joins the game, subscribing to all game output, and also allows that
  player to play the given color, both if :both is given (TODO NOT SUPPORTED YET), or none for all other values"
  (let [game (get-in @games [:games game-id])]
    (if (and game (not (contains? (:joined-user-ids game) (:id user))))
      (let [output-ch (chan)]
        (tap (:result-mult-ch game) output-ch)
        (log/debug "Joining game" game-id)
        (swap! games add-user-to-game (:id user) game-id color channel-key {:output-ch output-ch})
        output-ch)
      (get-in @games [:user-ids (:id user) game-id :output-ch]))))

(defn add-strategy [games game-id color channel-key strategy]
  "Adds the strategy to the multi channel if it is not there already."
  (log/debug "Adding strategy to game" game-id color channel-key strategy)
  (when-let [multi (get-in @games [:games game-id :strategies color])]
    (when-not (get-strategy multi channel-key)
      (let [new-strategy (or strategy (case channel-key
                                        :encoded (encoded)
                                        :channel (channel)
                                        (throw (java.lang.IllegalArgumentException.))))]
        (add-strategies multi {channel-key new-strategy})))))

; the handle-* methods parse the game message, do something
; then answer with a message
(defn handle-join-game [{:keys [games]} user game-id color channel-key]
  "Start notifying the given user of the moves for the given game-id.
   Sends the past moves so the game is updated."
  (let [output-ch (join-game games user game-id color channel-key)]
    ;; if there is no output ch, the game was not found
    (when output-ch (register-for-game-output user game-id output-ch)
          (go (>! (:channel user) (make-past-game-infos-msg 
                                   user game-id (get-in @games [:games game-id :past-game-infos]))))
          ;; we add the channel to the multi strategy if not already there
          (add-strategy games game-id color channel-key nil))))

(defn handle-new-game [{:keys [games] :as game-runner} user game first-player]
  "Returns a new game id and starts the game."
  (log/debug "New game")
  (let [game-id (new-game games game {:white (multi-channel) :black (multi-channel)} first-player)]
    (log/debug "Handling new game with id" game-id)
    (go (>! (:channel user) (make-notify-new-game-msg user game-id)))))

(defn handle-player-move [{:keys [games]} user game-id input]
  (player-move games game-id user input))

; no need to validate those messages as they should have been validated by the readers
; TODO maybe add "answer here"
; maybe reorganise this in game-runner/runner+commands
(defprotocol CommandHandler
  (handle-command [this game-runner]))

(defrecord PlayerMoveCommand [user game-id input]
  CommandHandler
  (handle-command [this game-runner]
    (handle-player-move game-runner user game-id input)))

(defrecord NewGameCommand [user game first-player]
  CommandHandler
  (handle-command [this game-runner]
    (handle-new-game game-runner user game first-player)))

(defrecord StartGameCommand [game-id]
  CommandHandler
  (handle-command [this game-runner]
    (start-game (:games game-runner) game-id)))

;; TODO probably the channel-key should be on the player-move command
;; then we don't need to save it anywhere
(defrecord JoinGameCommand [user game-id color channel-key]
  CommandHandler
  (handle-command [this game-runner]
    (handle-join-game game-runner user game-id color channel-key)))

(defrecord UserLeaveCommand [user]
  CommandHandler
  (handle-command [this game-runner]
    (leave-all-games (:games game-runner) user)))

(defn start-game-runner [{:keys [gamerunner-ch] :as game-runner}]
  (go-loop []
    ; TODO exception handling and scalability here we can let a lot of workers on this
    (when-let [command (<! gamerunner-ch)]
      (log/debug "Got command from websocket" command)
      ; TODO this is not allowed to fail
      (try
        (handle-command command game-runner)
        (catch Exception e (log/error e)))
      (recur)))
  game-runner)

(defn stop-game-runner [{:keys [games]}]
  ; (doseq [[_ game-ch] (deref channels)]
  ; (close! game-ch))
  )

(defn channel-stats [{:keys [games]}]
  (prewalk (fn [el] (cond (keyword? el) el 
                           (coll? el) el 
                           (= clojure.lang.Atom (type el)) @el
                           :else (str el))) @games))

(defn get-routes [game-runner]
  (-> (GET "/inspect" [request] {:body (channel-stats game-runner)})
      wrap-json-response))

(defrecord GameRunner [gamerunner-ch games])

(defn game-runner [gamerunner-ch]
  (map->GameRunner {:gamerunner-ch gamerunner-ch
                    :games (atom {:games {} :user-ids {}})}))
