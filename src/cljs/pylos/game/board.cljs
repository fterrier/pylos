(ns pylos.game.board
  (:require [cljs.core.async :as async :refer [chan close! put! sub]]
            [goog.string :as gstring]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [om.core :as om]
            [pylos.board :refer [cell]]
            [pylos.game.game-state
             :refer
             [append-game-infos
              append-past-game-infos
              change-current-index
              change-highlighted-position
              current-game-infos
              current-index
              current-move
              game-infos
              highlighted-position
              join-game
              moves-info
              position-info
              select-current-position]]
            [pylos.game.history :refer [history-comp]]
            [pylos.game.state :refer [game] :as state]
;            [pylos.game.util :refer [circle]]
            [pylos.board :refer [board-size]]
;            [pylos.init :refer [visit-board]]
            )
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn indexed-vector [m attrs]
  "[a b c] []    -> [[[0]     a] [[1]     b] [[2]     c]]
   [a b c] [1 2] -> [[[1 2 0] a] [[1 2 1] b] [[1 2 2] c]]"
  (map-indexed (fn [idx item] [(conj attrs idx) item]) m))

                                        ; TODO remove game-infos
(defcomponent hurmpf-cell-comp [[[game-infos layer row col] position] owner]
  (render [_]
          (let [current-move         (om/observe owner (current-move (game)))
                highlighted-position (om/observe owner (highlighted-position (game)))
                position-info        (position-info (game) (:selections current-move) highlighted-position)
                cell                 (cell (:board game-infos) position)
                move-low-position    (:risable (get position-info position))
                move-position        (:addable (get position-info position))
                move-removable       (:removable (get position-info position))
                move-selected        (:selected (get position-info position))
                move-square-position (:in-square (get position-info position))]
            (dom/div {:class (str "circle col-" col " circle-" (name cell) " "
                                  (when move-low-position "circle-low-position") " "
                                  (when move-position "circle-position") " "
                                  (when move-square-position "circle-square-position") " "
                                  (when move-square-position "circle-selected") " "
                                  (when move-removable "circle-removable") " ")
                      :data-position (str layer " " row " " col)
                      :on-mouse-over
                      (fn [e] (put! (:notif-ch (om/get-shared owner))
                                    {:topic :game :action :hover-cell :position position})
                        (. e preventDefault))
                      :on-mouse-out
                      (fn [e] (put! (:notif-ch (om/get-shared owner))
                                    {:topic :game :action :hover-cell :position nil})
                        (. e preventDefault))
                      :on-click
                      (fn [e] (put! (:notif-ch (om/get-shared owner))
                                    {:topic :game :action :select-cell :position position})
                        (. e preventDefault))}
                     #_(circle)))))


(defcomponent hurmpf-row-comp [[[game-infos layer row] positions] owner]
  (render [_]
          (dom/div {:class (str "pylos-row row-" row)}
                   (om/build-all hurmpf-cell-comp (indexed-vector positions [game-infos layer row])))))

(defcomponent hurmpf-layer-comp [[game-infos layers level] owner]
  (render [_]
          (when-not (empty? layers)
            (let [layer (first layers)]
              (dom/div {:class (str "pylos-layer layer-" level)}
                       (om/build-all hurmpf-row-comp (indexed-vector layer [game-infos level]))
                       (om/build hurmpf-layer-comp [game-infos (rest layers) (+ level 1)]))))))

(defcomponent balls-remaining-comp [[color remaining-balls next-player] owner]
  (render [_]
          (dom/div {:class (str "pylos-remaining-balls-color pylos-remaining-balls-" (name color))}
                   (dom/span {:class (str "pylos-remaining-balls-label " (name next-player))}
                             (if (= next-player color)  "PLAY" "WAIT"))
                   (for [i (range 0 remaining-balls)]
                     (dom/div {:class (str "circle circle-" (name color))}
                              (dom/div {:class "circle-content"} (inc i))
                              #_(circle))))))


(defcomponent additional-infos-iteration-comp [iteration owner]
  (render [_]
          (dom/div {:class "infos-move-stats-iteration"}
                   (dom/table
                    (dom/tbody
                     (dom/tr
                      (dom/td {:class "infos-move-stats-left"}
                              (dom/div "Depth " (:depth iteration))
                              (dom/table {:class "infos-move-stats"}
                                         (dom/tr (dom/td " - lookup: ")
                                                 (dom/td (:lookup-moves (:stats iteration))))
                                         (dom/tr (dom/td " - calculated: ")
                                                 (dom/td (:calculated-moves (:stats iteration))))
                                         (dom/tr (dom/td " - total: ")
                                                 (dom/td (:total-moves (:stats iteration))))))
                      (dom/td
                       (dom/div "Time at depth: " (gstring/format "%.2fms" (:time iteration)))
                       (dom/div "Moves per ms: " (gstring/format "%.1f" (:moves-per-ms iteration)))
                       (dom/div "Best score: " (gstring/format "%.4f" (:best-possible-score (:negamax-values iteration))))
                       (when (:outcome (:negamax-values iteration))
                         (dom/div "Winner: " (name (:outcome (:negamax-values iteration))))))))))))

(defcomponent additional-infos-comp [current-game-infos owner]
  (render [_]
          (dom/div
           (om/build-all additional-infos-iteration-comp (:additional-infos current-game-infos)))))

(defcomponent move-info-comp [game-infos owner]
  (render [_]
          (let [current-move         (om/observe owner (current-move (game)))
                current-selections   (:selections current-move)
                can-play-move        (:playable-move current-move)
                highlighted-position (om/observe owner (highlighted-position (game)))
                position-info        (position-info (game) current-selections highlighted-position)
                moves-info           (moves-info (game) current-selections highlighted-position)]
            (println "Rendering highlighted move" position-info moves-info)
            (dom/div
             (when can-play-move
               (dom/button {:on-click (fn [e] (put! (:notif-ch (om/get-shared owner))
                                                    {:topic :game :action :play-current-move}) (. e preventDefault))} "Play this move!"))))))

(defcomponent board-comp [_ owner]
  (render [_]
          (let [all-game-infos  (om/observe owner (game-infos (game)))
                current-index   (om/observe owner (current-index (game)))
                game-infos      (current-game-infos (game) current-index)
                next-player     (:player game-infos)
                balls-remaining (:balls-remaining game-infos)
                board           (:board game-infos)
                layered-board   nil
                ;(visit-board board (fn [_ position] position))
                ]
            (println "Rendering board" layered-board)
            (if-not (empty? game-infos)
              (dom/div {:class "main"}
                       (dom/div {:class "pylos clearfix"}
                                (dom/div {:class "pylos-board"}
                                         (om/build hurmpf-layer-comp [game-infos layered-board 0]))
                                (dom/div {:class "pylos-remaining-balls clearfix"}
                                         (om/build balls-remaining-comp
                                                   [:white (:white balls-remaining) next-player])
                                         (om/build balls-remaining-comp
                                                   [:black (:black balls-remaining) next-player]))
                                (dom/div {:class "pylos-move-info"}
                                         (om/build move-info-comp game-infos)))
                       (dom/pre {:class "infos clearfix"}
                                (when (:move game-infos)
                                  (dom/div "Last move: " (name (pr-str (:move game-infos)))))
                                (dom/div (str "Time: " (gstring/format "%.2fs" (/ (:time game-infos) 1000000))))
                                (om/build additional-infos-comp  game-infos)))))))


(defmulti handle-notif (fn [_ _ control] (:action control)))

(defn- play-move [game-id board move comm-ch]
  ; TODO check if we have to send the board size here as well
  ; TODO send player color as well here
  (put! comm-ch {:action :server/player-move :message {:game-id game-id :input {:next-move move}}}))

(defn- play-move-when-must-play [{:keys [current-move] :as game} comm-ch]
  (let [{:keys [must-play-move playable-move]} current-move
        game-infos (current-game-infos game (current-index game))]
    (when must-play-move
      (play-move (:game-id game) (:board game-infos) playable-move comm-ch))))

(defmethod handle-notif :select-cell [game comm-ch control]
  (om/transact! game #(select-current-position % (:position control)))
  (play-move-when-must-play (state/game) comm-ch))

(defmethod handle-notif :hover-cell [game comm-ch control]
  (om/transact! game #(change-highlighted-position % (:position control))))

(defmethod handle-notif :select-current-index [game comm-ch control]
  (om/transact! game #(change-current-index % (:current-index control))))

(defmethod handle-notif :play-current-move [game comm-ch control]
  (play-move-when-must-play game comm-ch))

(defmethod handle-notif :join-game [game comm-ch control]
  (om/transact! game #(join-game % (:game-id control))))

(defmethod handle-notif :msg/game-infos [game comm-ch {:keys [message]}]
  ; TODO validate game id
  (om/transact! game #(append-game-infos % (:game-infos message))))

(defmethod handle-notif :msg/past-game-infos [game comm-ch {:keys [message]}]
  ; TODO validate game id
  (om/transact! game #(append-past-game-infos % (:past-game-infos message))))

(defmethod handle-notif :default [_ _ _])


(defcomponent game-comp [_ owner]
  (init-state [_]
              {:control-ch (chan)})
  (will-mount [_]
              (let [control-ch   (om/get-state owner :control-ch)
                    notif-sub-ch (om/get-shared owner :notif-sub-ch)
                    _ (sub notif-sub-ch :game control-ch)
                    _ (sub notif-sub-ch :general control-ch)
                    _ (sub notif-sub-ch :server control-ch)]
                  (go-loop []
                    (let [control (<! control-ch)]
                      (when control
                        (try (handle-notif (game) (om/get-shared owner :comm-ch) control)
                             (catch js/Error e (println "Exception in loop" e)))
                        (recur))))))
  (will-unmount [_]
                (close! (om/get-state owner :control-ch)))
  (render-state [_ state]
                (dom/div
                 (om/build history-comp nil)
                 (om/build board-comp nil))))
