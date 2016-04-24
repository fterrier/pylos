(ns ui.pylos.parser
  (:require [cljs.core.async :refer [chan put! close!]]
            [om.next :as om :refer-macros [defui]]
            [ui.comm :as comm]
            [game.serializer :refer [deserialize-game-position]]
            [pylos.serializer :refer [new-pylos-serializer]]
            [pylos.ui :refer [game-infos-with-meta]]
            [pylos.board :as board])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [utils.macros :refer [t]]))

;; =======
;; Reconciler

(def pylos-game-serializer (new-pylos-serializer))

;; TODO do this when getting the infos
(defn get-merged-past-game-infos [past-game-infos]
  "Gets the game infos that have to be displayed. Merges game-infos with intermediate boards."
  (->> (reduce (fn [result {:keys [game-position] :as game-infos}]
                 (if (:intermediate-board game-position)
                   (conj (into [] (butlast result)) 
                         (-> (last result) 
                             (assoc-in [:game-position :intermediate-board] 
                                       (:intermediate-board game-position))
                             (assoc-in [:game-position :selected-positions]
                                       (:selected-positions game-position))))
                   (conj result game-infos))) [] past-game-infos)
       (map-indexed (fn [index item] (assoc item :index index)))
       (into [])))

(defn get-current-game-infos [past-game-infos index]
  "Gets the current game info, sets the right :display-board depending on which index we are looking at."
  (let [{:keys [game-position] :as current-game-infos}
        (if (= :current index)
          (last past-game-infos)
          (get past-game-infos index))
        current-game-infos (-> current-game-infos
                               (assoc-in [:game-position :display-board]
                                         (if (and (= :current index) (:intermediate-board game-position))
                                           (:intermediate-board game-position)
                                           (:board game-position))))]
    (if (= :current index) (game-infos-with-meta current-game-infos) current-game-infos)))

(defn get-game-history [merged-past-game-infos index]
  "Returns the history in the form {:index :last-player :outcome}, where
   - :index represents the index to the merged-past-game-infos vector
   - :last-player the player who last played before the game position represented by the index (not the same as :player, since :player is the player whose turn it is)
   - :outcome the winner if there is one"
  {:history/merged-game-infos
   (->> merged-past-game-infos
        (map (fn [{:keys [index game-position]}] 
               {:index (if (= index (dec (count merged-past-game-infos)))
                         :current
                         (inc index))
                :last-player (:player game-position) 
                :outcome (:outcome game-position)}))
        (cons {:index 0}))
   :history/selected-index index})

(defn get-tracker-infos [{:keys [game-position]} index]
  (let [{:keys [board player outcome]} game-position]
    {:tracker/balls-remaining {:black (board/balls-remaining board :black)
                               :white (board/balls-remaining board :white)}
     :tracker/player (when (= :current index) player)
     :tracker/outcome outcome}))

(defn get-current-game [state current-game]
  (let [game                   (get-in state (:app/game current-game))
        merged-past-game-infos (get-merged-past-game-infos (:game/past-game-infos game))
        current-game-infos     (get-current-game-infos merged-past-game-infos (:app/selected-index current-game))
        game-history           (get-game-history merged-past-game-infos (:app/selected-index current-game))
        tracker-infos          (get-tracker-infos current-game-infos (:app/selected-index current-game))]
    (-> game
        (assoc :game/game-history game-history)
        (assoc :game/display-game-infos current-game-infos)
        (assoc :game/tracker-infos tracker-infos))))

(defmulti read om/dispatch)

;; (defmethod read :root [{:keys [state parser query target] :as env} key _]
;;   (println "Reading state for target" target " and query " query)
;;   {:value (parser env query)})

(defmethod read :app/current-game [{:keys [state parser query target] :as env} key _]
  (println "Reading current game for target" target " and query " query key)
  (let [st @state
        current-game (get-current-game st (get-in st [:app/current-game]))]
    {:value (om/db->tree query current-game st)}))

(defn get-send-ch [state]
  (:send-ch (:comm state)))

(defn handle-server-messages [receive-ch reconciler]
  (go-loop []
    (when-let [message (<! receive-ch)]
      (println "Got message from server:" message)
      (om/transact! reconciler `[(server/server-message ~message)])
      (recur))))

(defn new-current-game [game-id]
  {:app/game [:games/by-id game-id]
   :app/selected-index :current})

(defn deserialize-game-infos [{:keys [game-position] :as game-infos}]
  (assoc game-infos 
         :game-position (deserialize-game-position pylos-game-serializer game-position)))

(defmulti mutate om/dispatch)

(defmethod mutate 'game/select-cell
  [{:keys [state]} _ {:keys [position]}]
  {:action (fn []
             (let [send-ch (get-send-ch @state)
                   game-id (get-in @state [:app/current-game :app/game 1])]
               ;; TODO do this in the remote ?
               ;; TODO only play if the 
               (put! send-ch {:action :server/player-move :message {:game-id game-id :color :black :input position}})))})

(defmethod mutate 'game/select-history
  [{:keys [state]} _ {:keys [index]}]
  {:action (fn [] (swap! state assoc-in [:app/current-game :app/selected-index] index))})

(defmethod mutate 'game/join-game
  [{:keys [state]} _ {:keys [game-id color]}]
  ;; TODO check that connection exists
  {:action (fn []
             (let [send-ch (get-send-ch @state)]
               ;; TODO do this in the remote ?
               (put! send-ch 
                     {:action :server/join-game :message {:game-id game-id :color color}}))
             (swap! state assoc-in [:app/current-game] (new-current-game game-id)))})

(defmethod mutate 'server/server-message
  [{:keys [state] :as env} _ {:keys [action message]}]
  (cond 
    (= action :msg/past-game-infos)
    (let [{:keys [game-id past-game-infos]} message]
      {:action (fn []
                 (swap! state assoc-in [:games/by-id game-id :game/past-game-infos] 
                        (into [] (map deserialize-game-infos past-game-infos))))})
    (= action :msg/game-infos)
    (let [{:keys [game-id game-infos]} message]
      {:action (fn []
                 (swap! state update-in [:games/by-id game-id :game/past-game-infos]
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
