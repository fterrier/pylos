(ns system.game-runner
  (:require [com.stuartsierra.component :as component]
            [server.game-runner :refer [game-runner get-routes 
                                        start-game-runner 
                                        stop-game-runner]]
            [clojure.core.async :refer [close!]]))

(defrecord GameRunnerComponent [gamerunner-ch gamerunner routes]
  component/Lifecycle
  (start [component]
    (let [gamerunner (game-runner gamerunner-ch)
          routes     (get-routes gamerunner)]
      (start-game-runner gamerunner)
      (assoc component
             :gamerunner gamerunner
             :routes routes)))
  (stop [component]
    ; TODO this should maybe not be here
    (close! gamerunner-ch)
    (when-let [gamerunner (:gamerunner component)]
      (stop-game-runner gamerunner))
    component))

(defn new-game-runner []
  (map->GameRunnerComponent {}))
