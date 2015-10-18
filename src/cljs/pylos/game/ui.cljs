(ns pylos.game.ui
  (:require [cljs.core.async :as async :refer [put! >! <! chan close!]]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [pylos.game.board :refer [board-comp]]
            [pylos.game.state :refer [app-state]]
            [taoensso.sente :as sente :refer (cb-success?)])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defcomponent app [app-state owner]
  (render [_]
          (om/build board-comp nil)))

(defmulti handle-control :action)

(defmethod handle-control :select-cell [control]
  (do
  ; (put! ws-ch (str "Action: " control))
  (println control)))

(defn event-msg-handler [test]
  (println "got event")
  )

(defn init-server-connection []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                    {:type :auto ; e/o #{:auto :ajax :ws}
                                     :host "localhost:8080"})]
    (swap! app-state #(assoc % :chsk chsk))
    ; (swap! app-state #(assoc % :ch-chsk ch-recv))
    (swap! app-state #(assoc % :chsk-send! send-fn))
    (swap! app-state #(assoc % :chsk-state state))
    (swap! app-state #(assoc % :router (sente/start-chsk-router! ch-recv event-msg-handler)))))

(defn main []
  (init-server-connection)

  (let [control-ch (chan)]
    (swap! app-state #(assoc % :control-ch control-ch))
    (go
     (while true
       (handle-control (<! control-ch))))

    (om/root app app-state
             {:target (. js/document (getElementById "main-area"))
              :shared {:control-ch control-ch}})))

(defn stop []
  (fnil close! (:control-ch (deref app-state)))
  (println "Router " (:router (deref app-state)))

  (when-let [stop-f (:router (deref app-state))]
    (println "Closing websocket routers" stop-f)
    (stop-f))

  (when-let [chsk (:chsk (deref app-state))]
    (println "Closing websocket routers" chsk)
    (sente/chsk-destroy! chsk))


  (swap! app-state #(assoc % :chsk nil))
  ; (swap! app-state #(assoc % :ch-chsk nil))
  (swap! app-state #(assoc % :chsk-send! nil))
  (swap! app-state #(assoc % :chsk-state nil))
  (swap! app-state #(assoc % :router nil))
  (swap! app-state #(assoc % :control-ch nil)))
