(ns pylos.game.app
  (:require [cljs.core.async :as async :refer [<! chan close! put! pub sub]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [om.core :as om]
            [pylos.game.board :refer [game-comp]]
            [pylos.game.comm :refer [init-server-connection stop-server-connection]]
            [pylos.game.state
             :refer
             [app-channels
              app-state]]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:import [goog.history EventType Html5History]))


(secretary/set-config! :prefix "#")

(defroute game "/game/*" {game-id :*}
  (println "Got link with a game" game-id)
  (when-let [control-ch (:notif-ch @app-channels)]
    (put! control-ch {:topic :general :action :join-game :game-id game-id :color :white})))

(defn navigate-to-game [game-id]
  (println "Navigating to game" game-id)
  (.setToken (Html5History.) (str "/game/" game-id))
  (put! (:notif-ch @app-channels) {:topic :general :action :join-game :game-id game-id :color :white}))

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

(defmulti handle-notif (fn [_ control] (:action control)))

(defmethod handle-notif :join-game [comm-ch {:keys [game-id color]}]
  (put! comm-ch {:action :server/join-game :message {:game-id game-id :color color}}))

(defmethod handle-notif :msg/new-game [comm-ch {:keys [message]}]
  (navigate-to-game (:game-id message)))

(defmethod handle-notif :chsk/state [comm-ch message]
  (when (:first-open? message)
    (hook-browser-navigation!)))

(defmethod handle-notif :default [_ control]
  (println "Unhandled" control))

(defcomponent app [app owner]
  (init-state [_]
              {:control-ch (chan)})
  (will-mount [_]
              (let [control-ch   (om/get-state owner :control-ch)
                    notif-sub-ch (om/get-shared owner :notif-sub-ch)
                    _ (sub notif-sub-ch :general control-ch)
                    _ (sub notif-sub-ch :server control-ch)]
                (go-loop []
                  (let [control (<! control-ch)]
                    (println "App loop got message" control)
                    (when control
                      (try  (handle-notif (om/get-shared owner :comm-ch) control)
                            (catch js/Error e (println "Unhandled exception" e)))
                      (recur))))))
  (will-unmount [_]
                (close! (om/get-state owner :control-ch)))
  (render-state [_ _]
          (dom/div 
           (dom/button {:on-click 
                        (fn [e] (put! (:comm-ch (om/get-shared owner)) {:action :server/new-game 
                                                                        :message {:first-player :white
                                                                                  :white {:strategy :websockets}
                                                                                  :back  {:strategy :negamax
                                                                                          :options {:depth 4}}}}) 
                          (. e preventDefault))} "new game")
           (om/build game-comp nil))))


(defn main []
    (println (. js/document (getElementById "main-area")))
    (om/root app app-state
             ; TODO listen on all state changes to fill up history
             {:target (. js/document (getElementById "main-area"))
              :shared {:notif-sub-ch (:notif-sub-ch @app-channels)
                       :notif-ch (:notif-ch @app-channels)
                       :comm-ch (:comm-ch @app-channels)}
              :tx-listen (fn [{:keys [path old-value new-value old-state new-state tag]} root]
                           ;(println "state changed")
                           ;(swap! app-state #(update % :history conj (:game-infos new-state)))
                           )}))

(defn start [])

(defn stop []
  (fnil close! (:control-ch @app-channels))
  (fnil close! (:notif-ch @app-channels))
  (fnil close! (:notif-sub-ch @app-channels))
  (try
    (stop-server-connection app-channels)
    (catch js/Object e (println "Error caught" e))))

(defn init []
    (let [control-ch   (chan)
          notif-ch     (chan)
          notif-sub-ch (pub notif-ch :topic)]
      (try
        (init-server-connection app-channels notif-ch)
        (catch js/Object e (println "Error caught" e)))
      (swap! app-channels assoc :control-ch control-ch)
      (swap! app-channels assoc :notif-ch notif-ch)
      (swap! app-channels assoc :notif-sub-ch notif-sub-ch)))
