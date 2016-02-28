(ns ui.pylos.board
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.cell :refer [cell]]
            [ui.pylos.test-data :as td]
            [pylos.board :as board]
            [pylos.ui :refer [game-infos-with-meta]]))

(defn- position-info [highlight-status current-selections position]
  (merge
   ;; all positions and the highlighted ones
   (get highlight-status (conj current-selections :all))
   (get highlight-status (conj current-selections position))))

(defn- is-selected [current-selections position]
  (contains? (into #{} current-selections) position))

(defn- cell-comp [[board on-select on-mouse-over on-mouse-out 
                  highlight-status highlighted-position current-selections] position]
  (let [position-info (get (position-info highlight-status current-selections highlighted-position) position)
        {:keys [risable addable removable in-square]} position-info
        ;; TODO put in the right states here
        hover         (if (or risable addable removable) :red :none)
        highlight     (cond (or in-square) :red
                            (is-selected current-selections position) :green
                            :else :none)]
    (cell {:color (board/cell board position)
           :position position 
           :on-select on-select
           :on-mouse-over on-mouse-over
           :on-mouse-out on-mouse-out
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
         '[:game-position :highlight-status :current-selections])
  Object
  (render [this]
          (let [{:keys [game-position highlight-status current-selections]} (om/props this)
                {:keys [board player outcome]} game-position
                {:keys [highlighted-position]} (om/get-state this)
                on-select (fn [position] 
                            (om/transact! this `[(select-cell ~{:position position})]))
                on-mouse-over (fn [position] (om/set-state! this {:highlighted-position position}))
                on-mouse-out (fn [position] (om/set-state! this {:highlighted-position nil}))]
            (board-comp [board on-select on-mouse-over on-mouse-out 
                         highlight-status highlighted-position current-selections]))))

(def game-position (om/factory GamePosition))

(defcard test-card-1
  (game-position td/game-infos-1))

(defcard test-card-2
  (game-position (assoc td/game-infos-2-black :current-selections [14])))

(defcard test-card-3
  (game-position (assoc td/game-infos-2-white :current-selections [])))
