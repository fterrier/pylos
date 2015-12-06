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
                                      play-current-move join-game reset]]
            [taoensso.sente :as sente :refer (cb-success?)])
  (:import [goog.history Html5History EventType])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(secretary/set-config! :prefix "#")

(defroute game "/game/*" {game-id :*}
  (println "Got link with a game" game-id)
  (when-let [control-ch (:control-ch (deref app-channels))]
    (put! control-ch {:action :join-game :game-id game-id})))

(defn navigate-to-game [game-id]
  (println "Navigating to game" game-id)
  (.setToken (Html5History.) (str "/game/" game-id))
  (put! (:control-ch (deref app-channels)) {:action :join-game :game-id game-id}))

(defn hook-browser-navigation! []
  (println "hooking browser navigation")
  (doto (Html5History.)
        (events/listen
         EventType/NAVIGATE
         (fn [event]
           (println "dispatching event" event)
           (.log js/console event)
           (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

(defmulti handle-event-msg (fn [_ v] (get v 0)))

(defmethod handle-event-msg :pylos/game-infos [app [id game-infos]]
  (append-game-infos app game-infos))

(defmethod handle-event-msg :pylos/new-game [app [id {:keys [game-id]}]]
  (navigate-to-game game-id))

(defmulti event-msg-handler (fn [_ ev-msg] (:id ev-msg)))

(defmethod event-msg-handler :default ; Fallback
  [app {:as ev-msg :keys [event]}]
  (println "Unhandled event:" event))

(defmethod event-msg-handler :chsk/state [app {:as ev-msg :keys [?data]}]
  (when (:first-open? ?data)
    (hook-browser-navigation!)))

(defmethod event-msg-handler :chsk/recv [app {:as ev-msg :keys [?data]}]
  ; (println "Push event from server:" ?data)
  (handle-event-msg app ?data))

(defn event-msg-handler* [app]
  (fn [{:as ev-msg :keys [id ?data event]}]
    (println "Event:" id ?data)
    (event-msg-handler app ev-msg)))

(defn init-server-connection [app app-channels]
  (println "Initializing server connection")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "/chsk") ; Note the same path as before
                                    {:type :auto ; e/o #{:auto :ajax :ws}
                                     ;:host "localhost:8080"
                                     })]
    (swap! app-channels assoc :chsk chsk)
    ; (swap! app-channels assoc :ch-chsk ch-recv)
    (swap! app-channels assoc :chsk-send! send-fn)
    (swap! app-channels assoc :chsk-state state)
    (swap! app-channels assoc :router (sente/start-chsk-router! ch-recv (event-msg-handler* app)))))

(defn stop-server-connection [app-channels]
  (println "Stopping server connection")
  (when-let [stop-f (:router (deref app-channels))]
    ; (println "Closing websocket routers" stop-f)
    (stop-f))

  (when-let [chsk (:chsk (deref app-channels))]
    ; (println "Closing websocket routers" chsk)
    (sente/chsk-destroy! chsk))

  (swap! app-channels assoc :chsk nil)
  ; (swap! app-channels assoc :ch-chsk nil)
  (swap! app-channels assoc :chsk-send! nil)
  (swap! app-channels assoc :chsk-state nil)
  (swap! app-channels assoc :router nil)
  (swap! app-channels assoc :control-ch nil))

(defmulti handle-control (fn [_ _ control] (:action control)))

(defmethod handle-control :select-cell [app comm-ch control]
  (select-current-position app comm-ch (:position control)))

(defmethod handle-control :hover-cell [app comm-ch control]
  (change-highlighted-position app (:position control)))

(defmethod handle-control :select-current-index [app comm-ch control]
  (change-current-index app (:current-index control)))

(defmethod handle-control :play-current-move [app comm-ch control]
  (play-current-move app comm-ch))

(defmethod handle-control :join-game [app comm-ch control]
  (join-game app comm-ch (:game-id control)))

(defmethod handle-control :new-game [app comm-ch control]
  ; TODO at the moment we just pipe it through
  (put! comm-ch (assoc control :action :server/start-new-game)))

(defmulti handle-comm (fn [_ _ comm] (:action comm)))

(defmethod handle-comm :server/join-game [app chsk-send control]
  (println "joining game" control)
  (chsk-send [:pylos/join-game {:game-id (:game-id control)}]))

(defmethod handle-comm :server/play-move [app chsk-send control]
  (println "playing move")
  (chsk-send [:pylos/player-move {:game-id (:game-id app) :game-infos (:game-infos control)}]))

(defmethod handle-comm :server/start-new-game [app chsk-send control]
  (println "creating new game")
  (chsk-send [:pylos/new-game (dissoc control :action)]))

(defcomponent app [app owner]
  (render [_]
          (dom/div
            (dom/button {:on-click (fn [e] (put! (:control-ch (om/get-shared owner)) {:action :new-game :first-player :white :negamax-depth 4 :websockets-color :white}) (. e preventDefault))} "new game")
            (om/build history-comp nil)
            (om/build board-comp nil))))

(defn main []
    (println (. js/document (getElementById "main-area")))
    (om/root app app-state
             ; TODO listen on all state changes to fill up history
             {:target (. js/document (getElementById "main-area"))
              :shared {:control-ch (:control-ch (deref app-channels))}
              :tx-listen (fn [{:keys [path old-value new-value old-state new-state tag]} root]
                           ;(println "state changed")
                           ;(swap! app-state #(update % :history conj (:game-infos new-state)))
                           )}))

(defn start [])

(defn stop []
  (fnil close! (:control-ch (deref app-channels)))
  (fnil close! (:comm-ch (deref app-channels)))
  (try
    (stop-server-connection app-channels)
    (catch js/Object e (println "Error caught" e))))

(defn init []
    (try
      (init-server-connection (om/root-cursor app-state) app-channels)
      (catch js/Object e (println "Error caught" e)))

    (let [control-ch (chan)
          comm-ch    (chan)
          chsk-send  (:chsk-send! (deref app-channels))]
      (swap! app-channels assoc :control-ch control-ch)
      (swap! app-channels assoc :comm-ch comm-ch)
      (go-loop []
        (handle-control (om/root-cursor app-state) comm-ch (<! control-ch))
        (recur))
      (go-loop []
        (handle-comm (om/root-cursor app-state) chsk-send (<! comm-ch))
        (recur))))
