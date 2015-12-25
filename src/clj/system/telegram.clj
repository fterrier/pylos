(ns system.telegram
  (:require [clojure.core.async :refer [>! go]]
            [com.stuartsierra.component :as component]
            [game.game :refer [other-color]]
            [org.httpkit.client :as http]
            [pylos
             [game :refer [new-pylos-game]]
             [score :refer [score-middle-blocked]]]
            [strategy.negamax :refer [negamax]]
            [system.strategy.websockets :refer [websockets]]))

(defn create-image []
  (let [png-trans (org.apache.batik.transcoder.image.PNGTranscoder.)
        reader    (java.io.StringReader. (pylos.svg/print-board  (pylos.init/create-board {:board [:black :black :open :open :white :white :white :open :open :open :open :open :open :open :open :open :open :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc] :size 4}) nil))
        input (org.apache.batik.transcoder.TranscoderInput. reader)
        fileos (java.io.FileOutputStream. "/Users/fterrier/projects/pylos/testr.png")
        bos (java.io.ByteArrayOutputStream.) 
        output (org.apache.batik.transcoder.TranscoderOutput. bos)]
    (. png-trans transcode input output)
    (. bos writeTo fileos)
    (. bos close)
    (. fileos close)))


(defn send-to-telegram [bot-id chat-id text message-id]
  (let [url (str "https://api.telegram.org/bot" bot-id "/sendPhoto")]    
    (println "Telegram - Sending message" url text)
    (http/get url {:query-params {:chat_id chat-id :text text :reply_to_message_id message-id}
                   :multipart [{:name "photo" :content (clojure.java.io/file "/Users/fterrier/projects/pylos/kiwi.jpeg") :filename "kiwi.jpeg"}]} 
              (fn [{:keys [status headers body error]}]
                (if error
                  (println "Telegram - Failed, exception is " error)
                  (println "Telegram - Async HTTP GET: " status body))))))

(defn send-message [bot-id uid message-id]
  (fn [[type message]]
    (send-to-telegram bot-id uid (str message) nil)))

(defmulti parse-message (fn [id data] id))

; TODO write a util for this
(defmethod parse-message "/new" [id data]
  (let [websockets-color :white
        negamax-depth    5
        game             (new-pylos-game 4)
        strategies       {websockets-color (websockets)
                          (other-color websockets-color)
                          (negamax score-middle-blocked negamax-depth)}]
    {:type :new-game :message {:game game :strategies strategies :first-player :white}}))

(defmethod parse-message :default [id data]
  (println "unrecognised message" data))

(defn- retrieve-message [id user data]
  (->  (parse-message id data)
       (assoc :user user)))

(defn- get-user [bot-id uid message-id]
  {:id uid :send-message (send-message bot-id uid message-id)})

; TODO maybe write a handler protocol so that we can 
; reuse this particular method and retrieve-message
(defn event-msg-handler* [bot-id gamerunner-ch]
  (fn [{:as ev-msg :keys [body]}]
    (let [{{:keys [text chat message_id]} :message} body
          uid     (:id chat)
          message (retrieve-message text (get-user bot-id uid message_id) nil)] 
      (if (and message (:type message)) (go (>! gamerunner-ch message))
          (send-to-telegram bot-id uid "sorry did not get that" message_id)))))

; event handler create-board
; TODO get rid of this ?
(defrecord Telegram [gamerunner-ch bot-id]
  component/Lifecycle
  (start [component]
         (assoc component :event-msg-handler (event-msg-handler* bot-id gamerunner-ch)))
  (stop [component] component))

(defn new-telegram [bot-id]
  (map->Telegram {:bot-id bot-id}))
