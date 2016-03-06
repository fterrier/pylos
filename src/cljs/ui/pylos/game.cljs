(ns ui.pylos.game
  (:require [cljs.core.async :refer [chan put! close!]]
            [devcards.core :as dc :refer-macros [defcard defcard-om-next]]
            [om-tools.dom :as dom :include-macros true]
            [om.next :as om :refer-macros [defui]]
            [ui.comm :as comm]
            [ui.pylos.history :refer [game-history GameHistory]]
            [ui.pylos.board :refer [game-position GamePosition]]
            [ui.pylos.utils :as utils]
            [ui.pylos.test-data :as td]
            [game.serializer :refer [deserialize-game-position]]
            [pylos.serializer :refer [new-pylos-serializer]]
            [pylos.ui :refer [game-infos-with-meta]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defui Game
  static om/IQuery
  (query [this]
         (let [subquery-game-position (om/get-query GamePosition)
               subquery-history (om/get-query GameHistory)]
           `[{:current-game 
              [{:current-game-infos ~subquery-game-position}
               {:game-history ~subquery-history}]}]))
  Object
  (render [this]
          (let [{:keys [current-game]} (om/props this)]
            (dom/div
             (dom/div (game-position (:current-game-infos current-game)))
             (dom/div (game-history (:game-history current-game)))))))

(defmulti read om/dispatch)


(defui RootTest
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query Game)]
           subquery)))

;; TODO create a component with an input field for the game id to join
;; TODO put this in a different file
;; =======
;; Reconciler

(def pylos-game-serializer (new-pylos-serializer))

(defn get-merged-past-game-infos [past-game-infos]
  "Gets the game infos that have to be displayed. Merges game-infos with intermediate boards."
  (->> (reduce (fn [result {:keys [game-position] :as game-infos}]
                 (if (:intermediate-board game-position)
                   (conj (into [] (butlast result)) (assoc-in (last result) 
                                                              [:game-position :intermediate-board] 
                                                       (:intermediate-board game-position)))
                   (conj result game-infos))) [] past-game-infos)
       (map-indexed (fn [index item] (assoc item :index index)))
       (into [])))

(defn get-current-game-infos [past-game-infos index]
  "Gets the current game info, sets the right :display-board depending on which index we are looking at."
  (let [{:keys [game-position] :as current-game-infos}
        (if (nil? index)
          (last past-game-infos)
          (get past-game-infos index))]
    (assoc-in current-game-infos [:game-position :display-board]
              (if (and (nil? index) (:intermediate-board game-position))
                (:intermediate-board game-position)
                (:board game-position)))))

(defn get-game [state current-game]
  (let [game                   (get-in state (:game current-game))
        merged-past-game-infos (get-merged-past-game-infos (:past-game-infos game))
        current-game-infos     (get-current-game-infos merged-past-game-infos (:selected-index current-game))]
    (-> game
        (assoc-in
         [:game-history :past-game-infos] merged-past-game-infos)
        (assoc
         :current-game-infos current-game-infos))))

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
   :selected-index nil})

(defn deserialize-game-infos [{:keys [game-position] :as game-infos}]
  (game-infos-with-meta 
   (assoc game-infos :game-position (deserialize-game-position pylos-game-serializer game-position))))

(defmulti mutate om/dispatch)

(defmethod mutate 'game/select-cell
  [{:keys [state]} _ {:keys [position]}]
  {:action (fn []
             (let [send-ch (get-send-ch @state)
                   game-id (get-in @state [:current-game :game 1])]
               ;; TODO do this in the remote ?
               (put! send-ch {:action :server/player-move :message {:game-id game-id :color :black :input position}})))})

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
                 (swap! state assoc-in [:games game-id :past-game-infos] 
                        (into [] (map deserialize-game-infos past-game-infos))))})
    (= action :msg/game-infos)
    (let [{:keys [game-id game-infos]} message]
      {:action (fn []
                 (swap! state update-in [:games game-id :past-game-infos]
                        conj (deserialize-game-infos game-infos)))})
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
