(ns pylos.game.board
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [goog.string :as gstring]
            [pylos.board :refer [cell]]
            [pylos.game.state :refer [game-infos move-info
                                      current-index current-game-infos]]
            [pylos.game.util :refer [circle]]))

(defn indexed-vector [m attrs]
  "[a b c] []    -> [[[0]     a] [[1]     b] [[2]     c]]
   [a b c] [1 2] -> [[[1 2 0] a] [[1 2 1] b] [[1 2 2] c]]"
  (map-indexed (fn [idx item] [(conj attrs idx) item]) m))


; (defcomponent cell-comp [[position cell] owner]
;   (render [_]
;           (dom/td {:class (str "board-cell board-cell-" (name cell))
;                    :on-click #(put! (:control-ch (om/get-shared owner)) {:action :select-cell :position position})}
;                   (dom/div {:class (str "board-cell-content board-cell-content-" (name cell))}))))
;
; (defcomponent row-comp [[[layer row :as position] cells] owner]
;   (render [_]
;           (dom/tr (om/build-all cell-comp (indexed-vector cells position)))))
;
;
; (defcomponent layer-comp [[[layer :as position] rows] owner]
;   (render [_]
;           (dom/table {:class (str "board-layer layer-" layer)} (om/build-all row-comp (indexed-vector rows position)))))

(defn is-low-position [move-info position]
  (when move-info
  (some #(= (:low-position %) position) (:moves move-info))))

(defn is-position [move-info position]
  (when move-info
  (some #(= (:position %) position) (:moves move-info))))

(defcomponent hurmpf-cell-comp [[[board layer row col] position] owner]
  (render [_]
          (let [move-info       (om/observe owner (move-info))
                cell            (cell board position)
                is-low-position (is-low-position move-info position)
                is-position     (is-position move-info position)]
            (println position is-position move-info)
            (dom/div {:class (str "circle col-" col " circle-" (name cell) " " (when is-low-position "circle-low-position") " " (when is-position "circle-position"))
                      :data-position (str layer " " row " " col)
                      :on-mouse-over (fn [e] (put! (:control-ch (om/get-shared owner)) {:action :hover-cell :position position}) (. e preventDefault))}
                     (circle)))))

(defcomponent hurmpf-row-comp [[[board layer row] positions] owner]
  (render [_]
          (dom/div {:class (str "pylos-row row-" row)}
                   (om/build-all hurmpf-cell-comp (indexed-vector positions [board layer row])))))

(defcomponent hurmpf-layer-comp [[board layers level] owner]
  (render [_]
          (when-not (empty? layers)
            (let [layer (first layers)]
              (dom/div {:class (str "pylos-layer layer-" level)}
                       (om/build-all hurmpf-row-comp (indexed-vector layer [board level]))
                       (om/build hurmpf-layer-comp [board (rest layers) (+ level 1)]))))))

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

(defcomponent additional-infos-comp [current-game-infos owner]
  (render [_]
          (dom/div
           (om/build-all additional-infos-iteration-comp (:additional-infos current-game-infos)))))

(defcomponent move-info-comp [_ owner]
  (render [_]
          (let [move-info (om/observe owner (move-info))]
            (println "Rendering move info" move-info)
            (dom/div (prn-str (:moves move-info))))))

(defcomponent board-comp [_ owner]
  (render [_]
          (let [all-game-infos  (om/observe owner (game-infos))
                current-index   (om/observe owner (current-index))
                game-infos      (current-game-infos all-game-infos current-index)
                next-player     (:next-player game-infos)
                balls-remaining (:balls-remaining game-infos)
                board           (:board game-infos)
                layered-board   (:layered-board game-infos)]
            (println "Rendering board" layered-board)
            (if-not (empty? game-infos)
            (dom/div {:class "main"}
                     (dom/div {:class "pylos clearfix"}
                              (dom/div {:class "pylos-board"}
                                       (om/build hurmpf-layer-comp [board layered-board 0]))
                              (dom/div {:class "pylos-remaining-balls clearfix"}
                                       (om/build balls-remaining-comp [:white (:white balls-remaining) next-player])
                                       (om/build balls-remaining-comp [:black (:black balls-remaining) next-player]))
                              (dom/div {:class "pylos-move-info"}
                                       (om/build move-info-comp [])))
                     (dom/pre {:class "infos clearfix"}
                              (when (:move game-infos) (dom/div "Last move: " (name (:color (:move game-infos)))))
                              (dom/div (str "Time: " (gstring/format "%.2fs" (/ (:time game-infos) 1000000))))
                              (om/build additional-infos-comp current-game-infos))
                    ;  (dom/div {:class "board"}
                            ;   (om/build-all layer-comp (indexed-vector board [])))
                     )))))
