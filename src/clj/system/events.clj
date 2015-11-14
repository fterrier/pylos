(ns system.events
  (:require
    [clojure.core.async :as async :refer [pub chan close!]]
    [system.app :refer :all]
    [com.stuartsierra.component :as component]))


(defrecord EventChannels [event-ch pub-ch]
  component/Lifecycle
  (start [component]
    (let [event-ch (chan)
          topic-fn :game-id]
      (assoc component
             :topic-fn topic-fn
             :event-ch event-ch
             :pub-ch (pub event-ch topic-fn))))
  (stop [component]
    (when event-ch (close! event-ch))
    component))

(defn new-event-channels []
  (map->EventChannels {}))

(defrecord EventHandler [event-channels]
  component/Lifecycle
  (start [component]
    (println event-channels)
    (assoc component
           :event-msg-handler (event-msg-handler* event-channels)))
  (stop [component] component))

(defn new-event-handler []
  (map->EventHandler {}))
