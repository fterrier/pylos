(ns ui.pylos.board
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [pylos.board :as board]
            [pylos.move :as move]
            [pylos.ui :refer [game-infos-with-meta]]
            [ui.pylos.circle :refer [circle]]
            [ui.pylos.test-data :as td]))

(defn- position-info [highlight-status selected-positions highlighted-position]
  (merge
   ;; all positions and the highlighted ones
   (get highlight-status (conj selected-positions :all))
   (get highlight-status (conj selected-positions highlighted-position))))

(defn- is-selected [selected-positions position]
  (contains? (into #{} selected-positions) position))

(defn- state-from-highlight-status [position-info highlight-status selected-positions 
                                    highlighted-position position]
  (let [position-info (get (position-info highlight-status selected-positions highlighted-position) position)
        {:keys [risable addable removable in-square]} position-info
        hover         (if (or risable addable removable) :red :none)
        highlight     (cond (or in-square) :red
                            (is-selected selected-positions position) :green
                            :else :none)]
    [hover highlight]))

(defn- state-from-last-move [board {:keys [type position positions low-position square-position]} 
                             current-position]
  (cond
    (= position current-position) 
    [:none :red]
    (= current-position low-position) 
    [:none :green]
    (and (= :square type) (contains? (into #{} positions) current-position)) 
    [:none :green]
    (and (= :square type) (contains? (into #{} (board/square-corners board square-position)) current-position))
    [:none :red]
    :else [:none :none]))

(defn- cell-comp [[board on-click on-mouse-over on-mouse-out last-move
                  highlight-status highlighted-position selected-positions] position]
  (let [[hover highlight] (if highlight-status 
                            (state-from-highlight-status position-info 
                                                         highlight-status 
                                                         selected-positions 
                                                         highlighted-position 
                                                         position) 
                            (state-from-last-move board last-move position))]
    (circle {:color (board/cell board position)
             :on-click (fn [e] (on-click position))
             :on-mouse-over (fn [e] (on-mouse-over position))
             :on-mouse-out (fn [e] (on-mouse-out position))
             :hover hover 
             :highlight highlight})))

(defn- row-comp [board-state positions]
  (dom/div {:class (str "pylos-row")}
           (map #(cell-comp board-state %) positions)))

(defn- layer-comp [board-state layers level]
  (when-not (empty? layers)
    (let [layer (first layers)]
      (dom/div {:class (str "pylos-layer layer-" level)}
               (map #(row-comp board-state %) layer)
               (layer-comp board-state (rest layers) (+ level 1))))))

(defn- board-comp [[board :as board-state]]
  (dom/div {:class "pylos-board"}
           (let [layered-board (board/visit-board board (fn [_ position] position))]
             (layer-comp board-state layered-board 0))))

(defui GamePosition
  static om/IQuery
  (query [this]
         '[{:game-position [:selected-positions :display-board]} :highlight-status :last-move])
  Object
  (render [this]
          (let [{:keys [game-position highlight-status last-move]} (om/props this)
                {:keys [display-board player outcome]} game-position
                {:keys [highlighted-position]}         (om/get-state this)
                on-select (fn [position] 
                            (om/transact! this `[(game/select-cell ~{:position position})]))
                on-mouse-over (fn [position] (om/set-state! this {:highlighted-position position}))
                on-mouse-out (fn [position] (om/set-state! this {:highlighted-position nil}))]
            (dom/div
             (board-comp [display-board on-select on-mouse-over on-mouse-out last-move
                          highlight-status highlighted-position (:selected-positions game-position)])
             ;(html-edn game-position)
             ))))

(def game-position (om/factory GamePosition))

(defn prep-game-infos [game-infos selected-positions]
  (-> game-infos 
      (assoc-in [:game-position :selected-positions] selected-positions)
      (assoc-in [:game-position :display-board] (get-in game-infos [:game-position :board]))))

(defcard test-card-1
  (game-position (game-infos-with-meta (prep-game-infos td/game-infos-1 nil))))

(defcard test-card-2
  (game-position (game-infos-with-meta (prep-game-infos td/game-infos-2-black [14]))))

(defcard test-card-3
  (game-position (game-infos-with-meta (prep-game-infos td/game-infos-2-white []))))

(defcard test-card-3-last-move
  (game-position (assoc (prep-game-infos td/game-infos-2-white []) :last-move (move/move-square
                                                                               (move/move-add :white 10)
                                                                               #{10 13}
                                                                               5))))
