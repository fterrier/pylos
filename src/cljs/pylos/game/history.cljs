(ns pylos.game.history
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.state :refer [game]]
            [pylos.game.game-state :refer [game-infos]]))


(defcomponent history-comp [_ owner]
  (render [_]
          (let [all-game-infos (om/observe owner (game-infos (game)))]
            (dom/div {:class "history"}
              (dom/div {:class "history-list clearfix"}
               (for [i (range 1 (count all-game-infos))]
                 (let [game-infos  (get all-game-infos i)
                       player      (or (:color (:move game-infos)) :white)]
                   (dom/div {:class (str "circle circle-" (name player) " history-move-" i)
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (put! (:notif-ch (om/get-shared owner))
                                               {:topic :game :action :select-current-index :current-index i}))}
                            (dom/div {:class "circle-content"} i)
                            #_(circle 10)))))

              (dom/a {:href "#"
                      :on-click (fn [e]
                                  (.preventDefault e)
                                  (put! (:notif-ch (om/get-shared owner))
                                        {:topic :game :action :select-current-index :current-index nil}))} "follow game")))))
