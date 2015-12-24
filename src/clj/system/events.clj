(ns system.events
  (:require [clojure.core.async :refer [>! go]]
            [com.stuartsierra.component :as component]
            [game.game :refer [other-color]]
            [pylos
             [game :refer [new-pylos-game]]
             [score :refer [score-middle-blocked]]]
            [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]))

(defn- get-user [uid send-fn]
  {:id uid :send-message #(send-fn uid %)})

; TODO maybe define a protocol to 
; 1. parse 2. validate 3. transform
; those messages
(defmulti parse-message (fn [id _] id))

(defmethod parse-message :server/player-move [id data]
  {:type :player-move :message data})

; TODO parse this and unhardcode
(defmethod parse-message :server/new-game [id _]
  (let [websockets-color :white
        negamax-depth    5
        game             (new-pylos-game 4)
        strategies       {websockets-color (websockets)
                          (other-color websockets-color) (negamax score-middle-blocked negamax-depth)}]
    {:type :new-game :message {:game game :strategies strategies :first-player :white}}))

(defmethod parse-message :server/join-game [id data]
  {:type :join-game :message data})

(defmethod parse-message :chsk/uidport-close [id data]
  {:type :user-leave})

(defmethod parse-message :default [_ _])

(defn- retrieve-message [id user data]
  (->  (parse-message id data)
       (assoc :user user)))

(defn event-msg-handler* [gamerunner-ch]
  (fn [{:as ev-msg :keys [id uid ?data event send-fn]}]
    (let [message (retrieve-message id (get-user uid send-fn) ?data)] 
      (when message (go (>! gamerunner-ch message))))))

; event handler create-board
; TODO get rid of this ?
(defrecord EventHandler [gamerunner-ch]
  component/Lifecycle
  (start [component]
    (assoc component :event-msg-handler (event-msg-handler* gamerunner-ch)))
  (stop [component] component))

(defn new-event-handler []
  (map->EventHandler {}))
