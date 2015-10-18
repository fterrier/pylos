(ns pylos.game.board
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.state :refer [board]]))

(defn get-class [cell]
  (name cell))

(defn indexed-vector [m attrs]
  "[a b c] []    -> [[[0]     a] [[1]     b] [[2]     c]]
   [a b c] [1 2] -> [[[1 2 0] a] [[1 2 1] b] [[1 2 2] c]]"
  (map-indexed (fn [idx item] [(conj attrs idx) item]) m))

(defcomponent cell-comp [[position cell] owner]
  (render [_]
          (dom/td {:class (str "board-cell board-cell-" (get-class cell))
                   :on-click #(put! (:control-ch (om/get-shared owner)) {:action :select-cell :position position})}
                  (dom/div {:class (str "board-cell-content board-cell-content-" (get-class cell))}))))

(defcomponent row-comp [[[layer row :as position] cells] owner]
  (render [_]
          (dom/tr (om/build-all cell-comp (indexed-vector cells position)))))


(defcomponent layer-comp [[[layer :as position] rows] owner]
  (render [_]
          (dom/table {:class (str "board-layer layer-" layer)} (om/build-all row-comp (indexed-vector rows position)))))

(defcomponent hurmpf-cell-comp [[[layer row col :as position] cell] owner]
  (render [_]
          (dom/div {:class (str "pylos-cell col-" col " pylos-cell-" (get-class cell) " position-" layer "-" row "-" col)}
                   (dom/svg {:width 100 :height 100}
                            (dom/circle {:cx 50 :cy 50 :r 47 :data-position (str layer " " row " " col)})))))

(defcomponent hurmpf-row-comp [[[layer row :as position] cells] owner]
  (render [_]
          (dom/div {:class (str "pylos-row row-" row)}
                   (om/build-all hurmpf-cell-comp (indexed-vector cells position)))))


(defcomponent hurmpf-layer-comp [[board level] owner]
  (render [_]
          (when-not (empty? board)
            (let [layer (first board)]
              (dom/div {:class (str "pylos-layer layer-" level)}
                       (om/build-all hurmpf-row-comp (indexed-vector layer [level]))
                       (om/build hurmpf-layer-comp [(rest board) (+ level 1)]))))))



(defcomponent board-comp [_ owner]
  (render [_]
          (let [board (om/observe owner (board))]
            (println "Rendering board" board)
            (dom/div {:class "main"}
                     (dom/div {:class "pylos clearfix"}
                              (dom/div {:class "pylos-board"}
                                       (om/build hurmpf-layer-comp [board 0])))
                     (dom/div {:class "board"}
                              (om/build-all layer-comp (indexed-vector board [])))))))
