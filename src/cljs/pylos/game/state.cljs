(ns pylos.game.state
  (:require [om.core :as om]
            [pylos.game :refer [generate-all-moves]]
            [pylos.board :refer [starting-board ind balls-remaining board-indexes initialize-board-meta]]))


; (defn transform-move [{:keys [type position low-position o]}]
;   (case ))

(defn merge-and-group-by [acc {:keys [type position low-position] :as move}]
  (let [result (merge-with concat acc {position [move]})]
    (if (= :rise type) (merge-with concat result {low-position [move]}) result)))

(defn group-original-moves-by-position [moves]
  (let [moves-without-squares (remove #(= (:type %) :square) moves)
        moves-by-position     (reduce merge-and-group-by {} moves)]
    moves-by-position))

(defn group-square-moves-by-original-move [moves]
  (group-by :original-move (filter #(= (:type %) :square) moves)))

(defn group-moves [moves]
  "Returns a map of {:original-moves :square-moves}.
  :original-moves is grouped by position and indicates all possible moves
  :square-moves is grouped by original moves and indicates all possible square moves"
  {:original-moves (group-original-moves-by-position moves) :square-moves (group-square-moves-by-original-move moves)})

(defn game-infos-with-meta [game-infos]
  (let [board           (:board game-infos)
        board-with-meta (initialize-board-meta board 4)
        ; TODO another way here would be for the server to send a list of all valid moves
        next-player     (:next-player game-infos)
        possible-moves  (group-moves (generate-all-moves {:board board-with-meta :player next-player}))
        layered-board   (board-indexes board-with-meta)
        balls-remaining {:white (balls-remaining board-with-meta :white)
                         :black (balls-remaining board-with-meta :black)}
        game-infos      (assoc game-infos :board board-with-meta :layered-board layered-board :possible-moves possible-moves :balls-remaining balls-remaining)]
    game-infos))

(defn change-cell [board position new-cell]
  (assoc-in board position new-cell))

(def initial-state {:game-infos [(game-infos-with-meta
                                  {:board (starting-board 4)
                                   :next-player :white})]
                    :current-index []
                    :move-info {}})

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

(defn move-info []
  (om/ref-cursor (:move-info (om/root-cursor app-state))))

(defn current-game-infos [game-infos current-index]
  (if (empty? current-index) (last game-infos) (get game-infos (current-index 0))))

; change state
(defn append-game-infos [app game-infos]
  (let [game-infos-with-meta (game-infos-with-meta game-infos)]
    (om/transact! app :game-infos #(conj % game-infos-with-meta))))

(defn change-current-index [app current-index]
  (if (nil? current-index)
    (om/update! app :current-index [])
    (om/update! app :current-index [current-index])))


(defn show-move-info [app position]
  (let [current-game-infos (current-game-infos (game-infos) (current-index))
        move-info          {:position position :moves (get (:original-moves (:possible-moves current-game-infos)) position)}]
    (om/update! app :move-info move-info)))

;
(defn reset []
  (reset! app-state initial-state))

; mutate state
(defn app-change-cell [position new-cell]
  (swap! app-state (fn [state] (update-in state [:game-infos 0 :board] #(change-cell % position new-cell)))))
