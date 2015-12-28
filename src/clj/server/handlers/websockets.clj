(ns server.handlers.websockets
(:require [clojure.core.async :refer [<! >! chan close! go go-loop]]
[clojure.tools.logging :as log]
[compojure.core :refer [GET POST routes]]
[game.game :refer [other-color]]
[pylos
[game :refer [new-pylos-game]]
[score :refer [score-middle-blocked]]]
[ring.middleware
[keyword-params :refer [wrap-keyword-params]]
[params :refer [wrap-params]]]
[server.game-runner
 :refer
 [->JoinGameCommand
  ->NewGameCommand
  ->PlayerMoveCommand
  ->UserLeaveCommand]]
[server.handlers.handler :refer [Handler]]
[strategy
[channel :refer [channel]]
[negamax :refer [negamax]]]
[taoensso.sente :refer [make-channel-socket! start-chsk-router!]]
[taoensso.sente.server-adapters.http-kit
 :refer
 [sente-web-server-adapter]]))

(defn- get-user [uid send-fn user-ch]
  {:id uid :channel user-ch :send-message #(send-fn uid %)})

; TODO maybe define a protocol to 
; 1. parse 2. validate 3. transform
; those messages
(defmulti parse-message (fn [id _ _] id))

(defmethod parse-message :server/player-move [id user {:keys [game-id game-infos]}]
  (->PlayerMoveCommand user game-id game-infos))

; TODO parse this and unhardcode
; TODO make util
(defmethod parse-message :server/new-game [id user _]
  (let [channel-color :white
        negamax-depth 5
        game          (new-pylos-game 4)
        strategies    {channel-color
                       (channel)
                       (other-color channel-color) 
                       (negamax score-middle-blocked negamax-depth)}]
    (->NewGameCommand user game strategies :white)))

(defmethod parse-message :server/join-game [id user {:keys [game-id color]}]
  (->JoinGameCommand user game-id color))

(defmethod parse-message :chsk/uidport-close [id user data]
  (->UserLeaveCommand user))

(defmethod parse-message :default [_ _ _])

(defn- event-msg-handler* [gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [id uid ?data event send-fn]}]
    (let [message (parse-message id (get-user uid send-fn user-ch) ?data)] 
      (when message (go (>! gamerunner-ch message))))))

(defn- start-event-handler []
  (let [user-ch (chan)]
    (go-loop []
      (when-let [{:keys [user type] :as message} (<! user-ch)]
        (log/debug "Websockets Handler - Got message from game runner" message user)
        (try
          ((:send-message user) [type (dissoc message :type :user)])
          (catch Exception e (log/error e)))
        (recur)))
    user-ch))

(defn- app-routes [ring-ajax-post ring-ajax-get-or-ws-handshake]
  (-> (routes
       (GET  "/chsk" request
             (try (ring-ajax-get-or-ws-handshake request)
                                        ; do nothing in exception case
                  (catch Exception e (log/debug e))))
       (POST "/chsk" request (ring-ajax-post request)))
      wrap-keyword-params
      wrap-params))

(defn send-infos [handler uid infos]
  (log/debug "Websockets - Sending" uid infos)
  ((:chsk-send! handler) uid infos))

(defrecord WebsocketsHandler [options]
  Handler
  (start-handler [handler gamerunner-ch]
    (let [user-ch           (start-event-handler)
          event-msg-handler (event-msg-handler* gamerunner-ch user-ch)
          {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
                            (make-channel-socket! 
                             sente-web-server-adapter
                             (assoc options :user-id-fn
                                    (fn [ring-req] (:client-id ring-req))))]
      (assoc handler
             :ch-chsk ch-recv
             :chsk-send! send-fn
             :connected-uids connected-uids
             :router (atom (start-chsk-router! ch-recv event-msg-handler))
             :user-ch user-ch
             :routes (app-routes ajax-post-fn ajax-get-or-ws-handshake-fn))))
  (stop-handler [handler]
    (if-let [router (:router handler)]
      (if-let [stop-f @router]
        (stop-f)))
    (if-let [user-ch (:user-ch handler)]
      (close! user-ch)))
  (get-routes [handler]
    (:routes handler)))

(defn websockets-handler []
  (->WebsocketsHandler {}))
