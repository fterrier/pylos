(ns pylos.game.comm
  (:require [cljs.core.async :refer [>! chan pub close!]]
            [taoensso.sente :as sente])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn handle-comm [chsk-send {:keys [action message] :as control}]
  (chsk-send [action message]))

(defn event-msg-handler* [pub-ch]
  (fn [{:as ev-msg :keys [id ?data event]}]
    (println "Event:" id ?data)
    (go
      (case id
        :chsk/state (>! pub-ch {:topic :server :action :chsk/state :message ?data})
        :chsk/recv (>! pub-ch {:topic :server :action (get ?data 0) :message (get ?data 1)})
        (println "Unhandled event" id)))))


(defn init-server-connection [app-channels pub-ch]
  (println "Initializing server connection")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "/chsk") ; Note the same path as before
                                    {:type :auto})] ; e/o #{:auto :ajax :ws}
                                     ;:host "localhost:8080"

    (swap! app-channels assoc :chsk chsk)
    ; (swap! app-channels assoc :ch-chsk ch-recv)
    (swap! app-channels assoc :chsk-send! send-fn)
    (swap! app-channels assoc :chsk-state state)
    (swap! app-channels assoc :router (sente/start-chsk-router! ch-recv (event-msg-handler* pub-ch))))
  (let [comm-ch (chan)]
    (swap! app-channels assoc :comm-ch comm-ch)
    (go-loop []
      (handle-comm (:chsk-send! @app-channels) (<! comm-ch))
      (recur))))

(defn stop-server-connection [app-channels]
  (println "Stopping server connection")
  (when-let [stop-f (:router @app-channels)]
    ; (println "Closing websocket routers" stop-f)
    (stop-f))

  (when-let [chsk (:chsk @app-channels)]
    ; (println "Closing websocket routers" chsk)
    (sente/chsk-destroy! chsk))

  (fnil close! (:comm-ch @app-channels))

  (swap! app-channels assoc :chsk nil)
  ; (swap! app-channels assoc :ch-chsk nil)
  (swap! app-channels assoc :chsk-send! nil)
  (swap! app-channels assoc :chsk-state nil)
  (swap! app-channels assoc :router nil)
  (swap! app-channels assoc :control-ch nil))
