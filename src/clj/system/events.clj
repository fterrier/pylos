(ns system.events
  (:require [clojure.core.async :refer [<! >! put! close! go chan]]
            [com.stuartsierra.component :as component]))

(defn event-msg-handler* [websockets-ch]
  (fn [{:as ev-msg :keys [id uid ?data event]}]
    (go (>! websockets-ch {:type id :uid uid :message ?data}))))

; event handler create-board
; TODO get rid of this ?
(defrecord EventHandler [websockets-ch]
  component/Lifecycle
  (start [component]
    (assoc component :event-msg-handler (event-msg-handler* websockets-ch)))
  (stop [component] component))

(defn new-event-handler []
  (map->EventHandler {}))
