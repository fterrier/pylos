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
; [this] "Get a way to communicate to this user"))

(defn make-game-infos-msg [client game-id game-infos]
  {:type :msg/game-infos :client client :game-id game-id :game-infos (get-game-infos game-infos)})

(defn make-past-game-infos-msg [client game-id past-game-infos]
  {:type :msg/past-game-infos :client client :game-id game-id :past-game-infos (into [] (map get-game-infos past-game-infos))})

(defn make-notify-new-game-msg [client game-id]
  {:type :msg/new-game :client client :game-id game-id})

(defn make-notify-end-game-msg [client game-id]
  {:type :msg/end-game :client client :game-id game-id})
 
(defn register-for-game-output [client game-id output-ch]
  (log/debug "Game output - Registering for game output" client)
  (go-loop []
    (let [result (<! output-ch)]
      (log/debug "Game output - Got result" client result)
      (if (nil? result)
        (log/debug "Game output - Game is over" client)
        (do
          (go (>! (:channel client) (make-game-infos-msg client game-id result)))
          (recur))))))

(defn- random-string [length]
  (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(defn add-game [games game-id infos]
  (update-in games [:games game-id] merge (assoc infos :joined-user-ids {})))

(defn remove-game [games game-id]
  (-> games
      (update-in [:games] dissoc game-id)))

(defn add-user-to-game [games user-id game-id color channel-key]
  (-> games
      (update-in [:users user-id :games] #(if (nil? %) #{game-id} (conj % game-id)))
      (update-in [:games game-id :joined-user-ids] assoc user-id {:color color :channel channel-key})))

; TODO remove if not there any more
(defn remove-user-from-game [games user-id game-id]
  (-> games
      (update-in [:users user-id :games] disj game-id)
      (update :games #(->> %
                           (map (fn [[game-id game-map]] 
                                  [game-id (update game-map :joined-user-ids dissoc user-id)]))
                           (into {})))
      (update :users #(if (empty? (get-in [user-id :games] %))
                        (dissoc % user-id) %))))

(defn save-move-to-game [games game-id game-infos]
  (-> games
      (update-in [:games game-id :past-game-infos] conj game-infos)))

(defn add-output-to-game [games client-id game-id output-ch]
  (-> games
      (assoc-in [:channels client-id game-id] output-ch)))

(defn remove-output-from-game [games client-id game-id]
  (-> games
      (update-in [:channels client-id] dissoc game-id)
      (update :channels #(if (empty? (get client-id %))
                        (dissoc % client-id) %))))

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
            :started false}) game-id))

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
      ; TODO handleclient not found - retrieve game from persistence layer ?
      (log/debug "Game or player not found" game-id user)
      (let [multi   (get-in game [:strategies (:color player)])]
        (if (nil? multi)
          (log/debug "No game input client found for this player" game-id player)
          (let [strategy (get-strategy multi (:channel player))]
            (if (nil? strategy)
              (log/debug "No strategy found for client" player)
              (let [game-ch  (get-input-channel strategy)]                
                (log/debug "Sending move to game client" input)
                (go (>! game-ch input))))))))))

(defn subscribe-to-game [games client game-id]
  "Subscribe to game output, returns the output client."
  (let [output-ch (get-in @games [:channels (:id client) game-id])]
    (if output-ch output-ch
        (when-let [game (get-in @games [:games game-id])]
          (let [output-ch (chan)]
            (tap (:result-mult-ch game) output-ch)
            (log/debug "Subscribing to game" game-id)
            (swap! games add-output-to-game (:id client) game-id output-ch)
            output-ch)))))

; game-id can be null
(defn unsubscribe-from-game [games client game-id]
  "Frees resources associated to that game and player and stops notifying that
  player of moves for that game."
  (log/debug "Unsubscribing client" client)
  (let [game-ids-to-unsubscribe (if game-id [game-id] (keys (get-in @games [:channels (:id client)])))]
    (log/debug "Unsubscribing from games" game-ids-to-unsubscribe)
    (doseq [game-id game-ids-to-unsubscribe]
      (let [result-mult-ch (get-in @games [:games game-id :result-mult-ch])
            output-ch      (get-in @games [:channels (:id client) game-id])]
        (when (and result-mult-ch output-ch)
          (untap result-mult-ch output-ch)
          (close! output-ch)
          (swap! games remove-output-from-game (:id client) game-id))))))

(defn join-game [games user game-id color channel-key]
  "Joins the game, allowing that player to play the given color, both if :both is given (TODO NOT SUPPORTED YET), or none for all other values"
  (let [game (get-in @games [:games game-id])]
    (when (and game (not (contains? (:joined-user-ids game) (:id user))))
      (log/debug "Joining game" game-id)
      (swap! games add-user-to-game (:id user) game-id color channel-key))))

; game-id can be null
(defn leave-game [games user game-id]
  "Leave game so the player cannot play moves on that game anymore"
  (let [game-ids-to-leave (if game-id [game-id] (get-in @games [:users (:id user) :games]))]
    (log/debug "Leaving games" game-ids-to-leave)
    (doseq [game-id game-ids-to-leave]
      (swap! games remove-user-from-game (:id user) game-id))))

(defn add-strategy [games game-id color channel-key strategy]
  "Adds the strategy to the multi client if it is not there already."
  (when-let [multi (get-in @games [:games game-id :strategies color])]
    (when-not (get-strategy multi channel-key)
      (let [new-strategy (or strategy 
                             (case channel-key
                               :encoded (encoded)
                               :channel (channel)
                               (throw (java.lang.IllegalArgumentException.))))]
        (log/debug "Adding strategy to game" game-id color channel-key strategy)
        (add-strategies multi {channel-key new-strategy})))))

(defn handle-subscribe-game [{:keys [games]} client game-id]
  "Start notifiying the given user of the moves for the given game-id.
   Sends the past moves so the game is updated."
  (let [output-ch (subscribe-to-game games client game-id)]
    ;; if there is no output ch, the game was not found
    (when output-ch 
      (register-for-game-output client game-id output-ch)
      (go (>! (:channel client) 
              (make-past-game-infos-msg 
               client game-id (get-in @games [:games game-id :past-game-infos])))))))

(defn handle-unsubscribe-game [{:keys [games]} client game-id]
  "Stops notifying the given user of the moves for the given game-id."
  (unsubscribe-from-game games client game-id)
  ;; TODO clean up if no more from this user
  )

; the handle-* methods parse the game message, do something
; then answer with a message
(defn handle-join-game [{:keys [games]} client user game-id color channel-key]
  "Allows the user to play moves on that game."
  ;; we adclient to the multi strategy if not already there
  (add-strategy games game-id color channel-key nil)
  (join-game games user game-id color channel-key))

; TODO remove user id when needed
(defn handle-leave-game [{:keys [games]} client user game-id]
  "User won't be able to play game on that game any more"
  (leave-game games user game-id)
  ;; TODO clean up if no more from this user
  )

(defn handle-new-game [{:keys [games] :as game-runner} client game first-player]
  "Returns a new game id and starts the game."
  (log/debug "New game")
  (let [game-id (new-game games game {:white (multi-channel) :black (multi-channel)} first-player)]
    (log/debug "Handling new game with id" game-id)
    (go (>! (:channel client) (make-notify-new-game-msg client game-id)))))

(defn handle-player-move [{:keys [games]} client user game-id input]
  (player-move games game-id user input))

; no need to validate those messages as they should have been validated by the readers
; TODO maybe add "answer here"
; maybe reorganise this in game-runner/runner+commands
(defprotocol CommandHandler
  (handle-command [this game-runner]))

(defrecord SubscribeCommand [client game-id]
  CommandHandler
  (handle-command [this game-runner]
    (handle-subscribe-game game-runner client game-id)))

; game-id can be null
(defrecord UnsubscribeCommand [client game-id]
  CommandHandler
  (handle-command [this game-runner]
    (handle-unsubscribe-game game-runner client game-id)))

(defrecord NewGameCommand [client game first-player]
  CommandHandler
  (handle-command [this game-runner]
    (handle-new-game game-runner client game first-player)))

(defrecord StartGameCommand [client game-id]
  CommandHandler
  (handle-command [this game-runner]
    (start-game (:games game-runner) game-id)))

(defrecord PlayerMoveCommand [client user game-id input]
  CommandHandler
  (handle-command [this game-runner]
    (handle-player-move game-runner client user game-id input)))

;; TODO probablcchannel should be on the player-move command
;; then we don't need to save it anywhere
(defrecord JoinGameCommand [client user game-id color channel-key]
  CommandHandler
  (handle-command [this game-runner]
    (handle-join-game game-runner client user game-id color channel-key)))

; game-id can be null
(defrecord LeaveGameCommand [client user game-id]
  CommandHandler
  (handle-command [this game-runner]
    (handle-leave-game game-runner client user game-id)))

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
  ; (doseq [[_ game-ch] (clients)]
  ; (close! game-ch))
  )

(defn gamerunner-stats [{:keys [games]}]
  (prewalk (fn [el] (cond (keyword? el) el 
                           (coll? el) el 
                           (= clojure.lang.Atom (type el)) @el
                           :else (str el))) @games))

(defn get-routes [game-runner]
  (-> (GET "/inspect" [request] {:body (gamerunner-stats game-runner)})
      wrap-json-response))

(defrecord GameRunner [gamerunner-ch games])

(defn game-runner [gamerunner-ch]
  (map->GameRunner {:gamerunner-ch gamerunner-ch
                    :games (atom {:games {} :users {} :channels {}})}))
