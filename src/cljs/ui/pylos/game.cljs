(ns ui.pylos.game
  (:require [cljs.core.async :refer [chan put! close!]]
            [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.comm :as comm]
            [ui.pylos.history :refer [game-history GameHistory]]
            [ui.pylos.board :refer [game-position GamePosition]]
            [ui.pylos.utils :as utils]
            [ui.pylos.test-data :as td])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defui Game
  static om/IQuery
  (query [this]
         (let [subquery-game-position (om/get-query GamePosition)
               subquery-history (om/get-query GameHistory)]
           `[{:current-game ~(apply conj [:id {:current-game-infos subquery-game-position}] subquery-history)}]))
  Object
  (render [this]
          (let [{:keys [current-game]} (om/props this)]
            (dom/div
             (dom/div (game-history current-game))
             (dom/div (game-position (:current-game-infos current-game)))))))

(defmulti read om/dispatch)

;; =======
;; Reconciler

(defn get-current-game-info [past-game-infos index]
  (case index
    nil (last past-game-infos)
    (get past-game-infos index)))

(defn get-game [state current-game]
  (let [game              (get-in state (:game current-game))
        current-game-info (get-current-game-info 
                                    (:past-game-infos game) 
                                    (:selected-index current-game))
        current-game-info (assoc current-game-info :current-selections (:current-selections current-game))]
    (-> game
        (assoc :current-game-infos current-game-info))))

(defmethod read :current-game [{:keys [state parser query] :as env} key _]
  (let [st @state]
    {:value (get-game st (get st key))}))

(defn get-send-ch [state]
  (:send-ch (:comm state)))

(defn handle-server-messages [receive-ch reconciler]
  (go-loop []
    (when-let [message (<! receive-ch)]
      (println "Got message from server:" message)
      (om/transact! reconciler `[(server/server-message ~message)])
      (recur))))

(defn new-current-game [game-id]
  {:game [:games game-id]
   :current-selections []
   :selected-index nil})

(defmulti mutate om/dispatch)

(defmethod mutate 'game/select-history
  [{:keys [state]} _ {:keys [index]}]
  {:action (fn [] (swap! state assoc-in [:current-game :selected-index] index))})

(defmethod mutate 'game/join-game
  [{:keys [state]} _ {:keys [game-id color]}]
  ;; TODO check that connection exists
  {:action (fn []
             (let [send-ch (get-send-ch @state)]
               ;; TODO do this in the remote ?
               (put! send-ch 
                     {:action :server/join-game :message {:game-id game-id :color color}}))
             (swap! state assoc :current-game (new-current-game game-id)))})

(defmethod mutate 'server/server-message
  [{:keys [state] :as env} _ {:keys [action message]}]
  (println action message)
  (cond 
    (= action :msg/past-game-infos)
    (let [{:keys [game-id past-game-infos]} message]
      {:action (fn []
                 (swap! state assoc-in [:games game-id :past-game-infos] past-game-infos))})
    :else nil))

(defmethod mutate 'comm/init-connection
  [{:keys [state reconciler] :as env} _]
  {:action (fn []
             (when-not (:comm @state)
               (let [send-ch (chan)
                     receive-ch (chan)]
                 (handle-server-messages receive-ch reconciler)
                 (swap! state assoc 
                        :comm {:send-ch send-ch 
                               :receive-ch receive-ch
                               :connection-infos (comm/start-server-connection send-ch receive-ch)}))))})

(defmethod mutate 'comm/stop-connection
  [{:keys [state ] :as env} _]
  {:action (fn []
             (when (:comm @state)
               (let [comm (:comm @state)]
                 (comm/stop-server-connection (:connection-infos comm))
                 (close! (:send-ch comm))
                 (close! (:receive-ch comm))
                 (swap! state dissoc :comm))))})

(def parser (om.next/parser {:read read :mutate mutate}))
(def reconciler (om/reconciler {:state td/state-1-atom
                                :normalize false 
                                :parser parser}))

(defcard-om-next test-card
  Game
  reconciler)
