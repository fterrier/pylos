(ns pylos.game.app
  (:require [cljs.core.async :as async :refer [put! >! <! chan close!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.board :refer [board-comp]]
            [pylos.game.history :refer [history-comp]]
            [pylos.game.state :refer [app-state app-channels append-game-infos change-current-index
                                      select-current-position change-highlighted-position
                                      play-current-move]]
            [taoensso.sente :as sente :refer (cb-success?)])
  (:import goog.History)
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(secretary/set-config! :prefix "#")
(defroute game "/game/*" {game-id :*}
  (println "Got link with a game" game-id)
  ; TODO here we join the game
  ; TODO treat like a user event -> send to control-ch
  (swap! app-state (fn [state] (assoc state :game-id game-id))))

(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
         EventType/NAVIGATE
         (fn [event]
           (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

(defmulti handle-event-msg (fn [_ v] (get v 0)))

(defmethod handle-event-msg :pylos/game-infos [app [id game-infos]]
  (append-game-infos app game-infos))

(defmulti event-msg-handler (fn [_ ev-msg] (:id ev-msg)))

(defmethod event-msg-handler :default ; Fallback
  [app {:as ev-msg :keys [event]}]
  (println "Unhandled event:" event))

(defmethod event-msg-handler :chsk/recv [app {:as ev-msg :keys [?data]}]
  ; (println "Push event from server:" ?data)
  (handle-event-msg app ?data))

(defn event-msg-handler* [app]
  (fn [{:as ev-msg :keys [id ?data event]}]
    (println "Event:" id)
    (event-msg-handler app ev-msg)))

(defn init-server-connection [app owner]
  (println "Initializing server connection")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "/chsk") ; Note the same path as before
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

(defmulti handle-control (fn [_ _ control] (:action control)))

(defmethod handle-control :select-cell [app owner control]
  (select-current-position app (:control-ch (om/get-shared owner)) (:position control)))

(defmethod handle-control :hover-cell [app owner control]
  (change-highlighted-position app (:position control)))

(defmethod handle-control :select-current-index [app owner control]
  (change-current-index app (:current-index control)))

(defmethod handle-control :play-current-move [app owner control]
  (play-current-move app (:control-ch (om/get-shared owner))))

(defmethod handle-control :send-move-to-server [app owner control]
  (println "playing move")
  (let [chsk-send (om/get-state owner :chsk-send!)]
    (chsk-send [:pylos/player-move {:game-id (:game-id app) :game-infos (:game-infos control)}])))

(defmethod handle-control :send-new-game-to-server [app owner control]
  (println "creating new game")
  (let [chsk-send (om/get-state owner :chsk-send!)]
    ; [game-id websockets-color first-player negamax-depth]
    ; TODO should receive the ID back
    (chsk-send [:pylos/new-game (dissoc control :action)])))

; TODO make all channels global again so we can use them from the REPL
(defcomponent app [app owner]
  (will-mount [_]
              ; maybe we can move this outside cause it doesn't need to unmount
              (go-loop []
                (handle-control app owner (<! (:control-ch (om/get-shared owner))))
                (recur))

              (try
                (init-server-connection app owner)
                (catch js/Object e (println "Error caught" e))))
  (will-unmount [_]
                (fnil close! (:control-ch (om/get-shared owner)))

                (try
                  (stop-server-connection owner)
                  (catch js/Object e (println "Error caught" e))))
  (render [_]
          (dom/div
            (dom/button {:on-click (fn [e] (put! (:control-ch (om/get-shared owner)) {:action :send-new-game-to-server :first-player :white :negamax-depth 4 :websockets-color :white}) (. e preventDefault))} "new game")
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

(defn init []
  (hook-browser-navigation!))

(defn stop []
  )
