(ns server.handlers.telegram
  (:require [com.stuartsierra.component :as component]
            [game.game :refer [other-color]]
            [org.httpkit.client :as http]
            [pylos
             [game :refer [new-pylos-game]]
             [score :refer [score-middle-blocked]]
             [svg :refer [print-board]]]
            [strategy.negamax :refer [negamax]]
            [strategy.channel :refer [channel]]
            [clojure.tools.logging :as log]
            [server.game-runner :refer [->NewGameCommand ->JoinGameCommand]]
            [clojure.core.async :refer [go-loop chan close! <! >! go thread]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.json :refer [wrap-json-body]]
            [compojure.core :refer [POST]]
            [server.handlers.handler :refer [Handler]]
            [server.handlers.handler :refer [start-event-handler]]
            [server.handlers.handler :refer [parse-new-game-data]]))

(defn create-image [board]
  (let [png-trans (org.apache.batik.transcoder.image.PNGTranscoder.)
        reader    (java.io.StringReader. (print-board board nil))
        input (org.apache.batik.transcoder.TranscoderInput. reader)
        is (java.io.PipedInputStream.)
        os (java.io.PipedOutputStream. is)
        output (org.apache.batik.transcoder.TranscoderOutput. os)]
    (thread (. png-trans transcode input output)
            (. os close))
    is))

(defn- send-to-telegram [bot-id command options]
  (let [url (str "https://api.telegram.org/bot" bot-id "/" command)]
    (println "Telegram, sending")
    (log/debug "Telegram - Sending message" url options)
    (http/get url options 
              (fn [{:keys [status headers body error]}]
                (if error
                  (log/error "Telegram - Failed, exception is " error)
                  (log/debug "Telegram - Async HTTP GET: " status body))))))

(defn- send-message-to-telegram [bot-id chat-id text message-id]
  (send-to-telegram bot-id "sendMessage" {:query-params {:chat_id chat-id :text text :reply_to_message_id message-id}}))

(defn send-board-to-telegram [bot-id chat-id board message-id]
  (send-to-telegram bot-id "sendPhoto"
                    {:query-params 
                     {:chat_id chat-id :reply_to_message_id message-id} 
                     :multipart 
                     [{:name "photo" 
                       :content (create-image board) 
                       :filename "board.png"}]}))

(defn send-message [bot-id uid message-id]
  (fn [[type message]]
    (send-to-telegram bot-id uid (str message) nil)))

(defmulti parse-message (fn [[command & args] user data] command))

(defmethod parse-message "/new" [_ user data]
  (let [[game strategies first-player] (parse-new-game-data data)]
    (->NewGameCommand user game strategies first-player)))

(defmethod parse-message "/join" [[_ game-id & args] user data]
  (->JoinGameCommand user game-id :none))

(defmethod parse-message :default [_ user data]
  (log/debug "unrecognised message" data))

(defn- get-user [chat-id user-ch]
  {:id chat-id :channel user-ch})

; TODO maybe write a handler protocol so that we can 
; reuse this particular method and retrieve-message
(defn- event-msg-handler* [bot-id gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [body]}]
    (log/debug "Got message from telegram client" body)
    (let [{{:keys [text chat message_id]} :message} body
          chat-id (:id chat)
          message (parse-message (remove clojure.string/blank? (clojure.string/split text #" ")) (get-user chat-id user-ch) nil)] 
      (if message (go (>! gamerunner-ch message))
          (send-to-telegram bot-id chat-id "sorry did not get that" message_id)))))

(defmulti forward-message (fn [{:keys [type]} _] type))

(defmethod forward-message :msg/game-infos [{:keys [type user game-id game-infos]} bot-id]
  ; TODO this create-board should not be here
  (send-board-to-telegram bot-id (:id user) (pylos.init/create-board (:board game-infos)) nil))

(defmethod forward-message :default [message _]
  (log/debug "unrecognized message to forward" message))

(defn- gamerunner-msg-handler* [bot-id]
  (fn [message]
    (forward-message message bot-id)))

(defn- app-routes [event-msg-handler]
  (-> (POST "/telegram" request (event-msg-handler request) {:body "ok"})
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-json-response))

(defrecord TelegramHandler [bot-id]
  Handler
  (start-handler [handler gamerunner-ch]
    (let [user-ch           (start-event-handler (gamerunner-msg-handler* bot-id))
          event-msg-handler (event-msg-handler* bot-id gamerunner-ch user-ch)
          routes            (app-routes event-msg-handler)]
      (assoc handler :routes routes :user-ch user-ch)))
  (stop-handler [handler]
    (if-let [user-ch (:user-ch handler)]
      (close! user-ch)))
  (get-routes [handler]
    (:routes handler)))

(defn telegram-handler [bot-id]
  (map->TelegramHandler {:bot-id bot-id}))
