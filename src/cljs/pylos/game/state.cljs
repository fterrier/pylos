(ns pylos.game.state
  (:require [om.core :as om]
            [pylos.game.game-state :refer [initial-state-game]]
            [pylos.ui :refer [game-infos-with-meta]]))

(def initial-state {:game initial-state-game})



; state
(defonce app-state
  (atom initial-state))

(defonce app-channels
  (atom {}))

; get state
(defn game []
  (om/ref-cursor (:game (om/root-cursor app-state))))

; convenience stuff for development
(defn unselect-move []
  (swap! app-state (fn [state] (assoc state :current-move {:selections [] :playable-move nil :must-play-move false}))))


(defn regen-game-infos-state []
  (swap! app-state (fn [state] (update state :game-infos #(into [] (map game-infos-with-meta %))))))

(defn reset []
  (swap! app-state (fn [state] (merge state initial-state))))
