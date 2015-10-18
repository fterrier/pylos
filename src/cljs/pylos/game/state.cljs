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

; state
(defonce app-state
  (atom {:board (initial-board 4)}))

; get state
(defn board []
  (om/ref-cursor (:board (om/root-cursor app-state))))

; mutate state
(defn app-change-cell [position new-cell]
  (swap! app-state (fn [state] (update state :board #(change-cell % position new-cell)))))
