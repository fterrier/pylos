(ns ui.pylos.game
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.history :as history]
            [ui.pylos.board :refer [game-position GamePosition]]
            [ui.pylos.tracker :refer [game-tracker GameTracker]]
            [ui.pylos.test-data :as td]
            [ui.pylos.parser :as parser]
            [goog.log :as glog])
  (:import goog.debug.Console))

(defui Game
  static om/IQuery
  (query [this]
         (let [subquery-game-position (om/get-query GamePosition)
               subquery-tracker-infos (om/get-query GameTracker)
               subquery-history (om/get-query history/GameHistory)]
           `[:game/id
             :game/loading
             {:game/display-game-infos ~subquery-game-position}
             {:game/tracker-infos ~subquery-tracker-infos}
             {:game/game-history ~subquery-history}]))
  static om/Ident
  (ident [this props]
         [:games/by-id (:game/id props)])
  Object
  (render [this]
          (let [{:keys [game/loading game/display-game-infos 
                        game/game-history game/tracker-infos]} (om/props this)]
            (println tracker-infos)
            (if loading
              (dom/div "loading")
              (dom/div {:class "pylos-game"}
                       (dom/div (game-position display-game-infos))
                       (dom/div (game-tracker tracker-infos))
                       (dom/div (history/game-history game-history)))))))

(def game (om/factory Game))

(defcard game-loading
  (game {:game/loading true}))

(defui RootTest
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query Game)]
           `[{:app/current-game ~subquery}]))
  Object
  (render [this]
          (let [{:keys [app/current-game]} (om/props this)]
            (dom/div 
             (dom/div
              (dom/input {:value (om/get-state this :value)
                          :on-change (fn [e]
                            (om/set-state! this {:value (.. e -target -value)}))})
              (dom/button {:on-click (fn [e] 
                                       (om/transact! this `[(game/join-game 
                                                             {:game-id ~(om/get-state this :value)
                                                              :color :black})]))} "Join game"))
             (game current-game)))))

(defonce normalized-state-1-atom (atom td/normalized-state-1))
(def reconciler-1 (om/reconciler {:state normalized-state-1-atom
                                  :normalize false 
                                  :parser parser/parser}))

(defcard-om-next test-card
  RootTest reconciler-1)


(defonce full-state-1-atom (atom td/state-1))
(def reconciler-2 (om/reconciler {:state full-state-1-atom
                                  :normalize false
                                  :remotes []
                                  :parser parser/parser}))

(defcard-om-next test-root-card
  RootTest reconciler-2)

