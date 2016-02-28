(ns ui.pylos.game
  (:require [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.pylos.board :refer [game-position GamePosition]]
            [ui.pylos.test-data :as td]))

(defui Game
  ;; "Game component, will render the game position, stats, remaining balls, etc..."
  static om/Ident
  (ident [this {:keys [id]}]
         [:games id])
  static om/IQuery
  (query [this]
         ;; TODO subquery and parameter for which position to display
         '[:id :past-game-infos])
  Object
  (render [this]
          (let [{:keys [past-game-infos]} (om/props this)
                game-infos                (last past-game-infos)]
            (println (last past-game-infos))
            (dom/div (game-position (assoc game-infos :current-selections []))))))

(def game (om/factory Game))

(defui Root
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query Game)]
           `[{:current-game ~subquery}]))
  Object
  (render [this]
          (let [{:keys [current-game]} (om/props this)]
            (dom/div (game current-game)))))

(defmulti read om/dispatch)

(defmethod read :current-game [{:keys [state] :as env} key params]
  (let [st @state]
    {:value  (get-in st (get st key))}))

(def parser (om.next/parser {:read read}))

(def test-reconciler (om/reconciler {:state (atom td/state-1)
                                     :normalize false
                                     :parser parser}))

(defcard-om-next test-card
  Root
  test-reconciler)
