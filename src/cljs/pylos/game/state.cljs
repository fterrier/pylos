(ns pylos.game.state
  (:require [om.core :as om]))

; board methods
(defn initial-board [size]
  (let [board (into [] (for [x (range size 0 -1)]
                         (into [] (repeat x
                                          (into [] (repeat x (if (= size x) :open :no-acc)))))))]
    board))

(defn change-cell [board position new-cell]
  (assoc-in board position new-cell))

(def initial-state {:game-infos [{:board (initial-board 4)
                                   :balls-remaining {:white 15 :black 15}}]
                    :current-index []})

; state
(defonce app-state
  (atom initial-state))

(defonce app-channels
  (atom {}))

; get state
(defn game-infos []
  (om/ref-cursor (:game-infos (om/root-cursor app-state))))

(defn current-index []
  (om/ref-cursor (:current-index (om/root-cursor app-state))))

(defn reset []
  (reset! app-state initial-state))

; mutate state
(defn app-change-cell [position new-cell]
  (swap! app-state (fn [state] (update-in state [:game-infos 0 :board] #(change-cell % position new-cell)))))
