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
  (atom {:game-infos {:board (initial-board 4)
                      :balls-remaining {:white 15 :black 15}
                      }}))

; get state
(defn game-infos []
  (om/ref-cursor (:game-infos (om/root-cursor app-state))))

; mutate state
(defn app-change-cell [position new-cell]
  (swap! app-state (fn [state] (update-in state [:game-infos :board] #(change-cell % position new-cell)))))
