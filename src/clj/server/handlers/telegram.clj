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
            [server.handlers.handler :refer [Handler start-event-handler]]
            [pylos.move :refer [generate-all-moves]]
            [pylos.strategy.encoded :refer [encoded]]
            [server.game-runner :refer [->PlayerMoveCommand]]))

(defn create-image [board]
  (let [png-trans (org.apache.batik.transcoder.image.PNGTranscoder.)
        reader    (java.io.StringReader. (print-board board nil))
        input     (org.apache.batik.transcoder.TranscoderInput. reader)
        is        (java.io.PipedInputStream.)
        os        (java.io.PipedOutputStream. is)
        output    (org.apache.batik.transcoder.TranscoderOutput. os)]
    (thread (. png-trans transcode input output)
            (. os close))
    is))

;; START TELEGRAM CLIENT
(defn- send-telegram [bot-id command options]
  (let [url (str "https://api.telegram.org/bot" bot-id "/" command)]
    (log/debug "Telegram - Sending message" url options)
    (http/get url options 
              (fn [{:keys [status headers body error]}]
                (if error
                  (log/error "Telegram - Failed, exception is " error)
                  (log/debug "Telegram - Async HTTP GET: " status body))))))


(defmulti send-to-telegram (fn [_ {:keys [type]}] type))

(defmethod send-to-telegram :message [bot-id {:keys [chat-id text message-id]}]
  (send-telegram bot-id "sendMessage" {:query-params {:chat_id chat-id :text text :reply_to_message_id message-id}}))

(defmethod send-to-telegram :photo [bot-id {:keys [chat-id board message-id]}]
  (send-telegram bot-id "sendPhoto"
                    {:query-params 
                     {:chat_id chat-id :reply_to_message_id message-id} 
                     :multipart 
                     [{:name "photo" 
                       :content (create-image board) 
                       :filename "board.png"}]}))
;; END OF TELEGRAM CLIENT

;; MESSAGE PARSER FROM TELEGRAM
(defn- parse-new-game-data [args]
  [(new-pylos-game 4) {:white (encoded) :black (negamax score-middle-blocked 5)} :white])

(defn- format-possible-moves [possible-moves]

)

(defmulti parse-telegram-message (fn [[command & args] user message-id] command))
(defmulti handle-gamerunner-message (fn [{:keys [type]}] type))

(defmethod parse-telegram-message "/new" [[_ & args] user message-id]
  (let [[game strategies first-player] (parse-new-game-data args)]
    [[:gamerunner (->NewGameCommand user game strategies first-player)]]))

(defmethod parse-telegram-message "/join" [[_ game-id & args] user message-id]
  [[:gamerunner (->JoinGameCommand user game-id :none)]])

(defmethod parse-telegram-message "/play" [[_ game-id position] user message-id]
  [[:gamerunner (->PlayerMoveCommand user game-id position)]])

(defmethod parse-telegram-message :default [data user message-id]
  [[:telegram {:type :message 
                :chat-id (:id user) 
                :text "Sorry, did not get that"
                :message-id message-id}]])
;; END OF MESSAGE PARSER

(defmethod handle-gamerunner-message :msg/new-game [{:keys [user game-id]}]
  [[:gamerunner (->JoinGameCommand user game-id :white)]])

(defmethod handle-gamerunner-message :msg/game-infos [{:keys [type user game-id game-infos]}]
  [[:telegram {:type :photo :chat-id (:id user) :board 
               (if (:intermediate-board game-infos)
                 (:intermediate-board game-infos)
                 (:board game-infos))}]
   ;; (let [possible-moves (generate-all-moves game-infos)]
   ;;   [:telegram {:type :message :chat-id (:id user) :text (pr-str possible-moves)}])
   ])

(defmethod handle-gamerunner-message :default [message]
  (log/debug "Unrecognized message to forward" message)
  [])

(defn- get-user [chat-id user-ch]
  {:id chat-id :channel user-ch})

(defn- forward-messages [messages gamerunner-ch bot-id] 
  (doseq [[dest message] messages]
    (case dest
      :telegram (send-to-telegram bot-id message)
      :gamerunner (go (>! gamerunner-ch message)))))

; TODO maybe write a handler protocol so that we can 
; reuse this particular method and retrieve-message
(defn- event-msg-handler* [bot-id gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [body]}]
    (log/debug "Got message from telegram client" body)
    (let [{{:keys [text chat message_id]} :message} body
          messages (parse-telegram-message (remove clojure.string/blank? 
                                         (clojure.string/split text #" ")) 
                                 (get-user (:id chat) user-ch) message_id)] 
      (forward-messages messages gamerunner-ch bot-id))))

(defn- gamerunner-msg-handler* [bot-id gamerunner-ch]
  (fn [message]
    (let [messages (handle-gamerunner-message message)]
      (forward-messages messages gamerunner-ch bot-id))))

(defn- app-routes [event-msg-handler]
  (-> (POST "/telegram" request (event-msg-handler request) {:body "ok"})
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-json-response))

(defrecord TelegramHandler [bot-id]
  Handler
  (start-handler [handler gamerunner-ch]
    (let [user-ch           (start-event-handler 
                             (gamerunner-msg-handler* bot-id gamerunner-ch))
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
