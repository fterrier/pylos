(ns server.handlers.websockets
  (:require [clojure.core.async :refer [>! close! go]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST routes]]
            [pylos.game :refer [new-pylos-game]]
            [game.serializer :refer [serialize-game-position]]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            [server.game-runner
             :refer
             [->JoinGameCommand
              ->LeaveGameCommand
              ->NewGameCommand
              ->PlayerMoveCommand
              ->StartGameCommand
              ->SubscribeCommand
              ->UnsubscribeCommand]]
            [server.handlers.handler :refer [Handler start-event-handler]]
            [taoensso.sente :refer [make-channel-socket! start-chsk-router!]]
            [taoensso.sente.server-adapters.http-kit
             :refer
             [sente-web-server-adapter]]))

;; TODO put this centraly somewhere
(def pylos-game (new-pylos-game 4))

(defn send-infos [{:keys [handler]} uid infos]
  (log/debug "Websockets - Sending" uid infos)
  ((:chsk-send! handler) uid infos))

(defn- get-client [uid send-fn user-ch]
  {:id uid :channel user-ch :send-message #(send-fn uid %)})

;; (defn- parse-strategy [{:keys [strategy options]}]
;;   (case strategy
;;     :client (client)
;;     :negamax (negamax score-middle-blocked (:depth options))))

(defn- parse-new-game-data [{:keys [first-player white black]}]
  [pylos-game first-player])

; TODO maybe define a protocol to 
; 1. parse 2. validate 3. transform
; those messages
(defmulti parse-message (fn [id _ _ _] id))

(defmethod parse-message :server/player-move [_ client user {:keys [game-id input]}]
  [(->PlayerMoveCommand client user game-id input)])

(defmethod parse-message :server/new-game [_ client user data]
  (let [[game first-player] (parse-new-game-data data)]
    [(->NewGameCommand client game first-player)]))

(defmethod parse-message :server/join-game [_ client user {:keys [game-id color]}]
  ;; TODO here we should leave all other games of that player
  ;; or the client should check for the right game id and unsubscribe
  [(->SubscribeCommand client game-id)
   (->JoinGameCommand client user game-id color :channel)])

(defmethod parse-message :server/start-game [_ client user {:keys [game-id]}]
  [(->StartGameCommand client game-id)])

(defmethod parse-message :chsk/uidport-close [_ client user data]
  [(->LeaveGameCommand client user nil)
   (->UnsubscribeCommand client nil)])

(defmethod parse-message :default [_ _ _ _])

(defn- serialize-game-infos [game-infos]
  (-> game-infos 
      (assoc :game-position (serialize-game-position pylos-game (:game-position game-infos)))))

(defn- format-message-for-client [{:keys [type] :as message}]
  (let [data (dissoc message :type :client)]
    [type (case type
            :msg/game-infos 
            (update data :game-infos serialize-game-infos)
            :msg/past-game-infos 
            (update data :past-game-infos #(map serialize-game-infos %))
            data)]))

(defn- event-msg-handler* [gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [id uid ?data event send-fn]}]
    (let [messages (parse-message id (get-client uid send-fn user-ch) {:id uid} ?data)]
      (doseq [message messages]
        (go (>! gamerunner-ch message))))))

(defn- gamerunner-msg-handle [{:keys [client type] :as message}]
  ((:send-message client) (format-message-for-client message)))

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
