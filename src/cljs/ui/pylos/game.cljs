(ns ui.pylos.game
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.history :refer [game-history GameHistory]]
            [ui.pylos.board :refer [game-position GamePosition]]
            [ui.pylos.utils :as utils]
            [ui.pylos.test-data :as td]
            [ui.pylos.parser :as parser]))

(defui Game
  static om/IQuery
  (query [this]
         (let [subquery-game-position (om/get-query GamePosition)
               subquery-history (om/get-query GameHistory)]
           `[{:current-game 
              [:loading
               {:current-game-infos ~subquery-game-position}
               {:game-history ~subquery-history}]}]))
  Object
  (render [this]
          (let [{:keys [current-game]} (om/props this)]
            (if (:loading current-game)
              (dom/div "loading")
              (dom/div
               (dom/div (game-position (:current-game-infos current-game)))
               (dom/div (game-history (:game-history current-game))))))))

(def game (om/factory Game))

(defcard game-loading
  (game {:current-game {:loading true}}))

(defui RootTest
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query Game)]
           `[{:root ~subquery}]))
  Object
  (render [this]
          (let [{:keys [root]} (om/props this)]
            (dom/div 
             (dom/div
              (dom/input {:value (om/get-state this :value)
                          :on-change (fn [e]
                            (om/set-state! this {:value (.. e -target -value)}))})
              (dom/button {:on-click (fn [e] 
                                       (om/transact! this `[(game/join-game 
                                                             {:game-id ~(om/get-state this :value)
                                                              :color :black})]))} "Join game"))
             (game root)))))

(defonce normalized-state-1-atom (atom td/normalized-state-1))
(def reconciler-1 (om/reconciler {:state normalized-state-1-atom
                                  :normalize false 
                                  :parser parser/parser}))

(defcard-om-next test-card
  RootTest reconciler-1)


(defonce full-state-1-atom (atom td/state-1))
(def reconciler-2 (om/reconciler {:state full-state-1-atom
                                  :normalize false
                                  :parser parser/parser}))

(defcard-om-next test-root-card
  RootTest reconciler-2)
