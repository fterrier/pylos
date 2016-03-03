(ns ui.comm
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
        :chsk/state (>! pub-ch {:action :chsk/state :message ?data})
        :chsk/recv (>! pub-ch {:action (get ?data 0) :message (get ?data 1)})
        (println "Unhandled event" id)))))

(defn stop-server-connection [connection-infos]
  (println "Stopping server connection")
  (when-let [stop-f (:router connection-infos)]
    ; (println "Closing websocket routers" stop-f)
    (stop-f))

  (when-let [chsk (:chsk connection-infos)]
    ; (println "Closing websocket routers" chsk)
    (sente/chsk-destroy! chsk))
  
  ;; (fnil close! (:comm-ch connection-infos))
  )

(defn start-server-connection [send-ch receive-ch]
  (println "Starting server connection")
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "/chsk") ; Note the same path as before
                                    {:type :auto})] ; e/o #{:auto :ajax :ws}
                                        ;:host "localhost:8080"
    (go-loop []
      (handle-comm send-fn (<! send-ch))
      (recur))
    {:chsk chsk
     :chsk-state state
     ;; :ch-chsk ch-recv
     :router (sente/start-chsk-router! ch-recv (event-msg-handler* receive-ch))}))

