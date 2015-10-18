(ns pylos.game.board
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.state :refer [board]]))

(defn get-class [cell]
  (name cell))

(defn indexed-vector [m attrs]
  (map-indexed (fn [idx item] [(conj attrs idx) item]) m))

(defcomponent cell-comp [[position cell] owner]
  (render [_]
          (dom/td {:class (str "board-cell board-cell-" (get-class cell))
                   :on-click #(put! (:control-ch (om/get-shared owner)) {:action :select-cell :position position})}
                  (dom/div {:class (str "board-cell-content board-cell-content-" (get-class cell))}))))

(defcomponent row-comp [[position cells] owner]
  (render [_]
          (dom/tr (om/build-all cell-comp (indexed-vector cells position)))))

(defcomponent layer-comp [[position rows] owner]
  (render [_]
          (dom/table {:class "board-layer"} (om/build-all row-comp (indexed-vector rows position)))))

(defcomponent board-comp [_ owner]
  (render [_]
          (let [board (om/observe owner (board))]
            (println "Rendering board" board)
            (dom/div {:class "board"}
                     (om/build-all layer-comp (indexed-vector board []))))))
