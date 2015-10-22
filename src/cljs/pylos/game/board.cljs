(ns pylos.game.board
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [goog.string :as gstring]
            [pylos.game.state :refer [game-infos current-index]]
            [pylos.game.util :refer [circle]]))

(defn indexed-vector [m attrs]
  "[a b c] []    -> [[[0]     a] [[1]     b] [[2]     c]]
   [a b c] [1 2] -> [[[1 2 0] a] [[1 2 1] b] [[1 2 2] c]]"
  (map-indexed (fn [idx item] [(conj attrs idx) item]) m))

(defcomponent cell-comp [[position cell] owner]
  (render [_]
          (dom/td {:class (str "board-cell board-cell-" (name cell))
                   :on-click #(put! (:control-ch (om/get-shared owner)) {:action :select-cell :position position})}
                  (dom/div {:class (str "board-cell-content board-cell-content-" (name cell))}))))

(defcomponent row-comp [[[layer row :as position] cells] owner]
  (render [_]
          (dom/tr (om/build-all cell-comp (indexed-vector cells position)))))


(defcomponent layer-comp [[[layer :as position] rows] owner]
  (render [_]
          (dom/table {:class (str "board-layer layer-" layer)} (om/build-all row-comp (indexed-vector rows position)))))

(defcomponent hurmpf-cell-comp [[[layer row col :as position] cell] owner]
  (render [_]
          (dom/div {:class (str "circle col-" col " circle-" (name cell) " position-" layer "-" row "-" col)
                    :data-position (str layer " " row " " col)}
                   (circle))))

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

(defcomponent balls-remaining-comp [[color remaining-balls next-player] owner]
  (render [_]
          (dom/div {:class (str "pylos-remaining-balls-color pylos-remaining-balls-" (name color))}
                   (dom/span {:class (str "pylos-remaining-balls-label " (name next-player))} (if (= next-player color)  "PLAY" "WAIT"))
           (for [i (range 0 remaining-balls)]
             (dom/div {:class (str "circle circle-" (name color))}
                      (dom/div {:class "circle-content"} (inc i))
                      (circle))))))

(defcomponent additional-infos-iteration-comp [iteration owner]
  (render [_]
          (dom/div {:class "infos-move-stats-iteration"}
            (dom/table
              (dom/tr
               (dom/td {:class "infos-move-stats-left"}
                (dom/div "Depth " (:depth iteration))
                (dom/table {:class "infos-move-stats"}
                          (dom/tr (dom/td " - lookup: ") (dom/td (:lookup-moves (:stats iteration))))
                          (dom/tr (dom/td " - calculated: ") (dom/td (:calculated-moves (:stats iteration))))
                          (dom/tr (dom/td " - total: ") (dom/td (:total-moves (:stats iteration))))))
               (dom/td
                (dom/div "Time at depth: " (gstring/format "%.2fms" (:time iteration)))
                (dom/div "Moves per ms: " (gstring/format "%.1f" (:moves-per-ms iteration)))
                (dom/div "Best score: " (gstring/format "%.4f" (:best-possible-score (:negamax-values iteration))))
                (when (:outcome (:negamax-values iteration)) (dom/div "Winner: " (name (:outcome (:negamax-values iteration)))))))))))

(defcomponent additional-infos-comp [additional-infos owner]
  (render [_]
          (dom/div
           (om/build-all additional-infos-iteration-comp additional-infos))))

(defcomponent board-comp [_ owner]
  (render [_]
          (let [all-game-infos  (om/observe owner (game-infos))
                current-index   (om/observe owner (current-index))
                game-infos      (if (empty? current-index) (last all-game-infos) (get all-game-infos (current-index 0)) )
                next-player     (or (:next-player game-infos) :white)
                board           (:board game-infos)
                balls-remaining (:balls-remaining game-infos)]
            (println "Rendering board" board)
            (dom/div {:class "main"}
                     (dom/div {:class "pylos clearfix"}
                              (dom/div {:class "pylos-board"}
                                       (om/build hurmpf-layer-comp [board 0]))
                              (dom/div {:class "pylos-remaining-balls clearfix"}
                                       (om/build balls-remaining-comp [:white (:white balls-remaining) next-player])
                                       (om/build balls-remaining-comp [:black (:black balls-remaining) next-player])))
                     (dom/pre {:class "infos clearfix"}
                              (dom/div "Last move: " (name (:color (:move game-infos))))
                              (dom/div (str "Time: " (gstring/format "%.2fs" (/ (:time game-infos) 1000000))))
                              (om/build additional-infos-comp (:additional-infos game-infos)))
                     (dom/div {:class "board"}
                              (om/build-all layer-comp (indexed-vector board [])))))))
