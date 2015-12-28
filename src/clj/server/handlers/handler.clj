(ns server.handlers.handler
  (:require [clojure.core.async :refer [chan go-loop <!]]
            [clojure.tools.logging :as log]
            [pylos.game :refer [new-pylos-game]]
            [strategy.channel :refer [channel]]
            [game.game :refer [other-color]]
            [strategy.negamax :refer [negamax]]
            [pylos.score :refer [score-middle-blocked]]))

(defprotocol Handler
  (start-handler [this gamerunner-ch])
  (stop-handler [this])
  (get-routes [this]))

(defn start-event-handler [handling-fn]
  (let [user-ch (chan)]
    (go-loop []
      (when-let [{:keys [user] :as message} (<! user-ch)]
        (log/debug "Got message from game runner" message user)
        (try
          (handling-fn message)
          (catch Exception e (log/error e)))
        (recur)))
    user-ch))

(defn- parse-strategy [{:keys [strategy options]}]
  (case strategy
    :channel (channel)
    :negamax (negamax score-middle-blocked (:depth options))))

; TODO validate
(defn parse-new-game-data [{:keys [first-player white black]}]
  (let [channel-color :white
        game          (new-pylos-game 4)
        strategies    {:white (parse-strategy white)
                       :black (parse-strategy black)}]
       [game strategies first-player]))
