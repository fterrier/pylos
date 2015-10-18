(ns system.websockets
  (:require [taoensso.sente :as sente]
            [com.stuartsierra.component :as component]))

(defrecord ChannelSockets [ring-ajax-post ring-ajax-get-or-ws-handshake ch-chsk chsk-send! connected-uids router server-adapter event-msg-handler options]
  component/Lifecycle
  (start [component]
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
          (sente/make-channel-socket! server-adapter options)]
      (assoc component
             :ring-ajax-post ajax-post-fn
             :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn
             :ch-chsk ch-recv
             :chsk-send! send-fn
             :connected-uids connected-uids
             :router (atom (sente/start-chsk-router! ch-recv event-msg-handler)))))
  (stop [component]
    (if router
      (if-let [stop-f @router]
        (assoc component :router (stop-f))
        component))
    component))

(defn new-channel-sockets
  ([event-msg-handler server-adapter]
   (new-channel-sockets event-msg-handler server-adapter {}))
  ([event-msg-handler server-adapter options]
   (map->ChannelSockets {:server-adapter server-adapter
                         :event-msg-handler event-msg-handler
                         :options options})))
