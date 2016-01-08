(ns server.handlers.websockets
  (:require [clojure.core.async :refer [>! close! go]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST routes]]
            [game.board :refer [serialize-board]]
            [pylos.game :refer [new-pylos-game]]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            [server.game-runner
             :refer
             [->JoinGameCommand
              ->NewGameCommand
              ->PlayerMoveCommand
              ->StartGameCommand
              ->UserLeaveCommand]]
            [server.handlers.handler :refer [Handler start-event-handler]]
            [taoensso.sente :refer [make-channel-socket! start-chsk-router!]]
            [taoensso.sente.server-adapters.http-kit
             :refer
             [sente-web-server-adapter]]))

(defn send-infos [{:keys [handler]} uid infos]
  (log/debug "Websockets - Sending" uid infos)
  ((:chsk-send! handler) uid infos))

(defn- get-user [uid send-fn user-ch]
  {:id uid :channel user-ch :send-message #(send-fn uid %)})

;; (defn- parse-strategy [{:keys [strategy options]}]
;;   (case strategy
;;     :channel (channel)
;;     :negamax (negamax score-middle-blocked (:depth options))))

(defn- parse-new-game-data [{:keys [first-player white black]}]
  (let [game (new-pylos-game 4)]
    [game first-player]))

; TODO maybe define a protocol to 
; 1. parse 2. validate 3. transform
; those messages
(defmulti parse-message (fn [id _ _] id))

(defmethod parse-message :server/player-move [_ user {:keys [game-id input]}]
  (->PlayerMoveCommand user game-id input))

(defmethod parse-message :server/new-game [_ user data]
  (let [[game first-player] (parse-new-game-data data)]
    (->NewGameCommand user game first-player)))

(defmethod parse-message :server/join-game [_ user {:keys [game-id color]}]
  ;; TODO here we should leave all other games of that player
  ;; or the client should check for the right game id and unsubscribe
  (->JoinGameCommand user game-id color :channel))

(defmethod parse-message :server/start-game [_ user {:keys [game-id]}]
  (->StartGameCommand game-id))

(defmethod parse-message :chsk/uidport-close [_ user data]
  (->UserLeaveCommand user))

(defmethod parse-message :default [_ _ _])

(defn- serialize-game-infos [game-infos]
  (-> game-infos 
      (assoc :board (serialize-board (:board game-infos)))
      (dissoc :intermediate-board)))

(defn- format-message-for-client [{:keys [type] :as message}]
  (let [data (dissoc message :type :user)]
    [type (case type
            :msg/game-infos 
            (update data :game-infos serialize-game-infos)
            :msg/past-game-infos 
            (update data :past-game-infos #(map serialize-game-infos %))
            data)]))

(defn- event-msg-handler* [gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [id uid ?data event send-fn]}]
    (let [message (parse-message id (get-user uid send-fn user-ch) ?data)] 
      (when message (go (>! gamerunner-ch message))))))

(defn- gamerunner-msg-handle [{:keys [user type] :as message}]
  ((:send-message user) (format-message-for-client message)))

(defn- app-routes [ring-ajax-post ring-ajax-get-or-ws-handshake]
  (-> (routes
       (GET  "/chsk" request
             (try (ring-ajax-get-or-ws-handshake request)
                                        ; do nothing in exception case
                  (catch Exception e (log/debug e))))
       (POST "/chsk" request (ring-ajax-post request)))
      wrap-keyword-params
      wrap-params))

(defrecord WebsocketsHandler [options]
  Handler
  (start-handler [handler gamerunner-ch]
    (let [user-ch           (start-event-handler gamerunner-msg-handle)
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
