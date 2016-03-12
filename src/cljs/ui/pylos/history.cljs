(ns ui.pylos.history
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.utils :as utils]))

(defui GameHistory
  static om/IQuery
  (query [this]
         '[:app/selected-index 
           :app/merged-game-infos])
  Object
  (render [this]
          (let [{:keys [app/merged-game-infos app/selected-index]} (om/props this)
                max-index (count merged-game-infos)
                current-index (if (nil? selected-index) max-index selected-index)]
            (dom/div {:class "history-list"}
                     (map #(dom/div 
                            {:class (str "circle circle-" (name (:player %)) " "
                                         (when (= (:index %) selected-index) "history-selected"))}
                            (dom/div {:class "circle-content"
                                      :on-click (fn [e]
                                                  (om/transact! this `[(game/select-history ~{:index (:index %)}) :app/current-game]))}
                                     (:index %))
                            (utils/circle 10)) merged-game-infos)
                     (dom/button {:disabled (= 0 current-index)
                                  :on-click (fn [e]
                                              (.preventDefault e)
                                              (om/transact! this `[(game/select-history ~{:index (dec current-index)}) :app/current-game]))}
                                 "previous")
                     (dom/button {:disabled (>= current-index (dec max-index))
                                  :on-click (fn [e]
                                              (.preventDefault e)
                                              (om/transact! this `[(game/select-history ~{:index (inc current-index)}) :app/current-game]))}
                                 "next")
                     (dom/button {:disabled (nil? selected-index)
                                  :on-click (fn [e]
                                              (.preventDefault e)
                                              (om/transact! this `[(game/select-history ~{:index nil}) :app/current-game]))}
                                 "continue game")))))

(def game-history (om/factory GameHistory))

(defcard test-game-history
  (game-history {:app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                         {:index 2 :player :white}]}))
