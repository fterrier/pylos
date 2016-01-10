(ns server.handlers.handler
  (:require [clojure.core.async :refer [<! chan go-loop]]
            [clojure.tools.logging :as log]))

(defprotocol Handler
  (start-handler [this gamerunner-ch])
  (stop-handler [this])
  (get-routes [this]))

(defn start-event-handler [handling-fn]
  (let [user-ch (chan)]
    (go-loop []
      (when-let [{:keys [channel] :as message} (<! user-ch)]
        (log/debug "Got message from game runner" message channel)
        (try
          (handling-fn message)
          (catch Exception e (log/error e)))
        (recur)))
    user-ch))

