(ns system.events
  (:require
    [clojure.core.async :as async :refer [pub chan close! go-loop go >! <!]]
    [system.app :refer :all]
    [system.strategy.websockets :refer [websockets]]
    [strategy.negamax :refer [negamax]]
    [pylos.score :refer [score-middle-blocked]]
    [pylos.core :refer [play]]
    [game.game :refer [other-color]]
    [game.board :refer [serialize-board]]
    [com.stuartsierra.component :as component]))

; channels API
(defn new-channels []
  (atom {}))

(defn stop-channels [channels]
  (doseq [[_ game-ch] (deref channels)]
    (close! game-ch)))

(defn new-ch [channels game-id]
  (println "Creating game channel for" game-id)
  (let [game-ch (chan)]
    (swap! channels assoc game-id game-ch)
    game-ch))

(defn delete-ch [channels game-ch game-id]
  (println "Deleting game channel for" game-id)
    (close! game-ch)
    (swap! channels dissoc game-id))

(defn get-ch [channels game-id]
      (get (deref channels) game-id))

; private output stuff
(defn- send-game-infos [websockets uid {{:keys [board player outcome]} :game-position, move :last-move, additional-infos :additional-infos, time :time}]
  (println "Game output - Sending infos" uid board)
  ((:chsk-send! websockets) uid
                      [:pylos/game-infos
                       {:board (serialize-board board)
                        :next-player player
                        :move move
                        :time time
                        :additional-infos additional-infos}]))

; game output API
(defn register-for-game-output [{:keys [websockets]} result-ch game-id uid]
  (go-loop []
    (let [result (<! result-ch)]
      (println "Game output - Got result" game-id )
      (if (nil? result)
        (println "Game output - Game is over")
        (do
          (send-game-infos websockets uid result)
          (recur))))))

; (defn deregister-for-game-output []
;   )


(defrecord GameOutput [websockets])

(defn new-game-output []
  (map->GameOutput {}))

; game runner API
(defn new-game [{:keys [websockets-ch game-channels]} game-id size websockets-color first-player negamax-depth]
  (println game-channels)
  (let [; TODO where to close this channel ?
        ; TODO what if the client disconnects and the game never terminates ?
        game-ch          (new-ch game-channels game-id)
        negamax-strategy (negamax score-middle-blocked negamax-depth)]
      (play size
            {websockets-color (websockets game-ch nil)
            (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}
            first-player)))

(defmulti handle-websockets-msg (fn [_ {:keys [type]}] type))

(defmethod handle-websockets-msg :player-move [{:keys [game-channels]} {:keys [game-id game-infos]}]
  (let [game-ch (get-ch game-channels game-id)]
    (if (nil? game-ch)
      ; TODO handle this
      (println "Game runner - Game channel not found")
      (do
        (println "Game runner - Sending game infos to game channel" game-infos)
        (go (>! game-ch {:game-infos game-infos}))))))

(defn start-game-runner [{:keys [websockets-ch game-channels] :as game-runner}]
  (go-loop []
    (let [websockets-msg (<! websockets-ch)]
      (println "Game runner - Got message from websocket" websockets-msg)
      (handle-websockets-msg game-runner websockets-msg))
    (recur))
  game-runner)

(defrecord GameRunner [websockets-ch game-channels game-output]
  component/Lifecycle
  (start [component]
    (start-game-runner component))
  (stop [component]
    ; TODO this should not be here
    (close! websockets-ch)
    (stop-channels game-channels)
    component))

(defn new-game-runner []
  (map->GameRunner {:game-channels (new-channels)}))


; event handler create-board
; TODO get rid of this
(defrecord EventHandler [websockets-ch]
  component/Lifecycle
  (start [component]
    (assoc component
           :event-msg-handler (event-msg-handler* websockets-ch)))
  (stop [component] component))

(defn new-event-handler []
  (map->EventHandler {}))
