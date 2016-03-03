(ns ui.pylos.history
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.utils :as utils]))

(defui GameHistory
  static om/IQuery
  (query [this]
         '[{:past-game-infos [:index {:game-position [:player]}]}])
  Object
  (render [this]
          (let [{:keys [past-game-infos]} (om/props this)]
            (dom/div {:class "history-list"}
                     (map #(dom/div 
                            {:class (str "circle circle-" (name (:player (:game-position %))))}
                            (dom/div {:class "circle-content"
                                      :on-click (fn [e]
                                                  (om/transact! this `[(game/select-history ~{:index (:index %)}) :current-game]))} 
                                     (:index %))
                            (utils/circle 10)) past-game-infos)))))

(def game-history (om/factory GameHistory))

(defcard test-game-history
  (game-history {:past-game-infos [{:index 0 :game-position {:player :white}} 
                                   {:index 1 :game-position {:player :black}} 
                                   {:index 2 :game-position {:player :white}}]}))
