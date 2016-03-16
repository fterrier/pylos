(ns ui.pylos.history
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [ui.pylos.circle :refer [circle]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]))

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
            (dom/div 
             (dom/div {:class "history-controls"}
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
                                  "continue game"))
             (dom/ul {:class "history-list"}
                      (map #(dom/li
                             {:class (str "history-item " (when (= (:index %) selected-index) "history-selected"))}
                             
                             (dom/figure {:class (str "circle circle-" (name (:player %)))
                                          :on-click (fn [e]
                                                      (om/transact! this `[(game/select-history ~{:index (:index %)}) :app/current-game]))}
                                         (:index %))) merged-game-infos))))))

(def game-history (om/factory GameHistory))

(defcard test-game-history
  (game-history {:app/selected-index 10
                 :app/merged-game-infos [{:index 0 :player :white} 
                                         {:index 1 :player :black} 
                                         {:index 2 :player :white}
                                         {:index 3 :player :black} 
                                         {:index 4 :player :white}
                                         {:index 5 :player :black} 
                                         {:index 6 :player :white}
                                         {:index 7 :player :black} 
                                         {:index 8 :player :white}
                                         {:index 9 :player :black} 
                                         {:index 10 :player :white}
                                         {:index 11 :player :black} 
                                         {:index 12 :player :white}
                                         {:index 13 :player :black} 
                                         {:index 14 :player :white}
                                         {:index 15 :player :black} 
                                         {:index 16 :player :white}
                                         {:index 17 :player :black} 
                                         {:index 18 :player :white}
                                         {:index 19 :player :black} 
                                         {:index 20 :player :white}
                                         {:index 21 :player :black} 
                                         {:index 22 :player :white}
                                        ]}))
