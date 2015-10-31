(ns pylos.game.ui
  (:require [cljs.core.async :as async :refer [put! >! <! chan close!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.board :refer [board-comp]]
            [pylos.game.history :refer [history-comp]]
            [pylos.game.state :refer [app-state app-channels append-game-infos change-current-index show-move-info]]
            [taoensso.sente :as sente :refer (cb-success?)])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defmulti handle-event-msg (fn [_ v] (get v 0)))

(defmethod handle-event-msg :pylos/game-infos [app [id {:keys [board balls-remaining next-player] :as game-infos}]]
  (append-game-infos app game-infos))

(defmulti event-msg-handler (fn [_ ev-msg] (:id ev-msg)))

(defmethod event-msg-handler :default ; Fallback
  [app {:as ev-msg :keys [event]}]
  (println "Unhandled event:" event))

(defmethod event-msg-handler :chsk/recv [app {:as ev-msg :keys [?data]}]
  ;(println "Push event from server:" ?data)
  (handle-event-msg app ?data))

(defn event-msg-handler* [app]
  (fn [{:as ev-msg :keys [id ?data event]}]
    (println "Event:" id)
    (event-msg-handler app ev-msg)))

(defn init-server-connection [app owner]
  (println "Initializing server connection")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                    {:type :auto ; e/o #{:auto :ajax :ws}
                                     ;:host "localhost:8080"
                                     })]
    (om/set-state! owner :chsk chsk)
    ; (om/set-state! owner :ch-chsk ch-recv)
    (om/set-state! owner :chsk-send! send-fn)
    (om/set-state! owner :chsk-state state)
    (om/set-state! owner :router (sente/start-chsk-router! ch-recv (event-msg-handler* app)))))

(defn stop-server-connection [owner]
  (println "Stopping server connection")
  (when-let [stop-f (om/get-state owner :router)]
    ; (println "Closing websocket routers" stop-f)
    (stop-f))

  (when-let [chsk (om/get-state owner :chsk)]
    ; (println "Closing websocket routers" chsk)
    (sente/chsk-destroy! chsk))

  (om/set-state! owner :chsk nil)
  ; (om/set-state! owner :ch-chsk nil)
  (om/set-state! owner :chsk-send! nil)
  (om/set-state! owner :chsk-state nil)
  (om/set-state! owner :router nil)
  (om/set-state! owner :control-ch nil))

(defmulti handle-control (fn [_ control] (:action control)))

(defmethod handle-control :hover-cell [app control]
   (show-move-info app (:position control)))

(defmethod handle-control :select-current-index [app control]
  (change-current-index app (:current-index control)))

(defcomponent app [app owner]
  (will-mount [_]
              (go-loop []
                (handle-control app (<! (:control-ch (om/get-shared owner))))
                (recur))

              (init-server-connection app owner))
  (will-unmount [_]
                (fnil close! (:control-ch (om/get-shared owner)))
                (stop-server-connection owner))
  (render [_]
          (dom/div
            (om/build history-comp nil)
            (om/build board-comp nil))))


(defn main []
  (let [control-ch (chan)]
    (swap! app-channels #(assoc % :control-ch control-ch))

    (om/root app app-state
             ; TODO listen on all state changes to fill up history
             {:target (. js/document (getElementById "main-area"))
              :shared {:control-ch control-ch}
              :tx-listen (fn [{:keys [path old-value new-value old-state new-state tag]} root]
                           ;(println "state changed")
                           ;(swap! app-state #(update % :history conj (:game-infos new-state)))
                           )})))

(defn stop []
  )
