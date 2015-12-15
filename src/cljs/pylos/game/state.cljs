(ns pylos.game.state
  (:require [cljs.core.async :as async :refer [put! >! <! chan close!]]
            [om.core :as om]
            [game.board :refer [serialize-board]]
            [pylos.move :refer [generate-all-moves]]
            [pylos.ui :refer [highlight-status move-status game-infos-with-meta]]
            [pylos.board :refer [board-size ind balls-remaining]]
            [pylos.init :refer [create-board starting-board board-indexes initialize-board-meta]]))


(def initial-state {:game {:game-infos [(game-infos-with-meta
                                         {:size 4
                                          :board (serialize-board (starting-board 4))
                                          :next-player :white})]
                           :highlighted-position {:position nil} ; this contains the highlighted position
                           :current-move {:selections [] :can-play-move false} ; TODO change for playable-moves
                           :current-index []}})

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
  (swap! app-state (fn [state] (assoc state :current-move {:selections [] :can-play-move false}))))

(defn regen-game-infos-state []
  (swap! app-state (fn [state] (update state :game-infos #(into [] (map game-infos-with-meta %))))))

(defn reset []
  (swap! app-state (fn [state] (merge state initial-state))))
