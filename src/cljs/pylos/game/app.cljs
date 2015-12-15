(ns pylos.game.app
  (:require [cljs.core.async :as async :refer [<! chan close! put! pub]]
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
  (when-let [control-ch (:control-ch @app-channels)]
    (put! control-ch {:action :join-game :game-id game-id})))

(defn navigate-to-game [game-id]
  (println "Navigating to game" game-id)
  (.setToken (Html5History.) (str "/game/" game-id))
  (put! (:control-ch @app-channels) {:action :join-game :game-id game-id}))

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


;; (defmulti handle-event-msg (fn [_ v] (get v 0)))

;; (defmethod handle-event-msg :pylos/game-infos [app [id game-infos]]
;;   (append-game-infos app game-infos))

;; (defmethod handle-event-msg :pylos/new-game [app [id {:keys [game-id]}]]
;;   (navigate-to-game game-id))


;; (defmulti event-msg-handler (fn [_ ev-msg] (:id ev-msg)))

;; (defmethod event-msg-handler :default ; Fallback
;;   [app {:as ev-msg :keys [event]}]
;;   (println "Unhandled event:" event))

;; (defmethod event-msg-handler :chsk/state [app {:as ev-msg :keys [?data]}]
;;   (when (:first-open? ?data)
;;     (hook-browser-navigation!)))

;; (defmethod event-msg-handler :chsk/recv [app {:as ev-msg :keys [?data]}]
;;   ; (println "Push event from server:" ?data)
;;   (handle-event-msg app ?data))

;; (defn event-msg-handler* [app]
;;   (fn [{:as ev-msg :keys [id ?data event]}]
;;     (println "Event:" id ?data)
;;     (event-msg-handler app ev-msg)))


(defcomponent app [app owner]
  (render [_]
          (dom/div 
           (dom/button {:on-click 
                        (fn [e] (put! (:control-ch (om/get-shared owner)) {:action :new-game 
                                                                           :first-player :white 
                                                                           :negamax-depth 4 
                                                                           :websockets-color :white}) 
                          (. e preventDefault))} "new game")
           (om/build game-comp nil))))


(defn main []
    (println (. js/document (getElementById "main-area")))
    (om/root app app-state
             ; TODO listen on all state changes to fill up history
             {:target (. js/document (getElementById "main-area"))
              :shared {:notif-sub-ch (:notif-sub-ch @app-channels)
                       :notif-ch (:notif-ch @app-channels)}
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
 ;   (stop-server-connection app-channels)
    (catch js/Object e (println "Error caught" e))))

(defn init []
    (try
  ;    (init-server-connection app-channels)
      (catch js/Object e (println "Error caught" e)))


    (let [control-ch   (chan)
          notif-ch     (chan)
          notif-sub-ch (pub notif-ch :topic)]
      (swap! app-channels assoc :control-ch control-ch)
      (swap! app-channels assoc :notif-ch notif-ch)
      (swap! app-channels assoc :notif-sub-ch notif-sub-ch)
      ;; (go-loop []
      ;;   (handle-control (om/root-cursor app-state) comm-ch (<! control-ch))
      ;;   (recur))
      ))
