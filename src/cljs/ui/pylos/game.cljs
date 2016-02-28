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
  static om/IQueryParams
  (params [this]
          {:index 1})
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query GamePosition)]
           `[:id ({:past-game-infos ~subquery} {:index ~'?index})]))
  Object
  (render [this]
          (let [{:keys [past-game-infos]} (om/props this)
                {:keys [index]}           (om/get-params this)]
            (dom/div (game-position (assoc (get past-game-infos 0) :current-selections []))))))

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

(defmethod read :past-game-infos [{:keys [state query game] :as env} key params]
  {:value (if (nil? (:index params))
            [(last (get game key))]
            [(get (get game key) (:index params))])})

(defmethod read :id [{:keys [state game] :as env} key params]
  {:value (get game key)})

(defmethod read :current-game [{:keys [state parser query] :as env} key _]
  (let [st @state]
    {:value (parser (assoc env :game (get-in st (get st key))) query)}))

(def parser (om.next/parser {:read read}))

(def test-reconciler (om/reconciler {:state (atom td/state-1)
                                     :normalize false
                                     :parser parser}))

(defcard-om-next test-card
  Root
  test-reconciler)
