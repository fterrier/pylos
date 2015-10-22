(ns pylos.game.history
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.state :refer [game-infos]]
            [pylos.game.util :refer [circle]]))


(defcomponent history-comp [_ owner]
  (render [_]
          (let [all-game-infos (om/observe owner (game-infos))]
            (dom/div {:class "history"}
              (dom/div {:class "history-list clearfix"}
               (for [i (range 1 (count all-game-infos))]
                 (let [game-infos  (get all-game-infos i)
                       player      (or (:color (:move game-infos)) :white)]
                   (dom/div {:class (str "circle circle-" (name player) " history-move-" i)
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (put! (:control-ch (om/get-shared owner))
                                                    {:action :select-current-index :current-index i}))}
                            (dom/div {:class "circle-content"} i)
                            (circle 10)))))
              (dom/a {:href "#"
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (put! (:control-ch (om/get-shared owner))
                                            {:action :select-current-index :current-index nil}))} "follow game")))))
