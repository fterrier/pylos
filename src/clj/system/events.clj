(ns system.events
  (:require [clojure.core.async :refer [<! >! put! close! go chan]]
            [com.stuartsierra.component :as component]))

; handlers
(defmulti handle-event-msg (fn [id game-id ?data game-channels] id))

(defmethod handle-event-msg :pylos/new-game [id uid {:keys [first-player negamax-depth websockets-color]} websockets-ch]
  (println "Websockets got new game message" uid)
  (go (>! websockets-ch {:type :new-game :game-id "test" :first-player first-player :negamax-depth negamax-depth :websockets-color websockets-color})))

(defmethod handle-event-msg :pylos/player-move [id uid {:keys [game-infos]} websockets-ch]
  (println "Websockets got player move, sending to game runner" uid)
  (go (>! websockets-ch {:type :player-move :game-id uid :game-infos game-infos})))

(defmethod handle-event-msg :default ; Fallback
  [id uid event game-channels]
  (println "Unhandled event:" id uid)
  )

(defn event-msg-handler* [websockets-ch]
  (fn [{:as ev-msg :keys [id uid ?data event]}]
    (handle-event-msg id uid ?data websockets-ch)))


; event handler create-board
; TODO get rid of this ?
(defrecord EventHandler [websockets-ch]
  component/Lifecycle
  (start [component]
    (assoc component :event-msg-handler (event-msg-handler* websockets-ch)))
  (stop [component] component))

(defn new-event-handler []
  (map->EventHandler {}))
