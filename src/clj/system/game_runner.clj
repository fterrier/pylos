(ns system.game-runner
  (:require [com.stuartsierra.component :as component]
            [server.game-runner :refer [game-runner]]
            [server.game-runner :refer [start-game-runner]]
            [server.game-runner :refer [stop-game-runner]]
            [clojure.core.async :refer [close!]]))


(defrecord GameRunnerComponent [gamerunner-ch gamerunner]
  component/Lifecycle
  (start [component]
    (let [gamerunner (game-runner gamerunner-ch)]
      (start-game-runner gamerunner)
      (assoc component :gamerunner gamerunner)))
  (stop [component]
    ; TODO this should not be here
    (close! gamerunner-ch)
    (when-let [gamerunner (:gamerunner component)]
      (stop-game-runner gamerunner))
    component))

(defn new-game-runner []
  (map->GameRunnerComponent {}))
