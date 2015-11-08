(ns system.events
  (:require
    [clojure.core.async :as async :refer [chan close!]]
    [system.app :refer :all]
    [com.stuartsierra.component :as component]))


(defrecord EventHandler [event-ch event-msg-handler]
  component/Lifecycle
  (start [component]
    (let [event-ch (chan)]
      (assoc component
             :event-ch event-ch
             :event-msg-handler (event-msg-handler* event-ch))))
  (stop [component]
    (when event-ch (close! event-ch))
    component))

(defn new-event-handler []
  (map->EventHandler {}))
