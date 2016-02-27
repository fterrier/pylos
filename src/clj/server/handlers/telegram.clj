(ns server.handlers.telegram
  (:require [cheshire.core :refer [generate-string]]
            [clojure.core.async :refer [>! close! go thread]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [routes GET POST]]
            [game.game :refer [generate-moves score]]
            [org.httpkit.client :as http]
            [pylos
             [game :refer [new-pylos-game]]
             [svg :refer [print-board]]]
            [pylos.ui :refer [highlight-status]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [server.game-runner
             :refer
             [->JoinGameCommand
              ->NewGameCommand
              ->PlayerMoveCommand
              ->StartGameCommand
              ->SubscribeCommand              
              ->UnsubscribeCommand
              ->NPCCommand
              ->StopGameCommand]]
            [server.handlers.handler :refer [Handler start-event-handler]]
            [server.site :refer [convert-to-json]]
            [pylos.init :refer [visit-board]]
            [clojure.core.async :refer [>!!]]
            [clojure.core.async :refer [chan]]
            [clojure.core.async :refer [go-loop]]
            [clojure.core.async :refer [<!]]
            [pylos.ui :refer [move-status]]))

(defn create-image [board last-move highlight-status selected-positions]
  (let [png-trans (org.apache.batik.transcoder.image.PNGTranscoder.)
        reader    (java.io.StringReader. 
                   (print-board board last-move highlight-status selected-positions)) 
        input     (org.apache.batik.transcoder.TranscoderInput. reader)
        is        (java.io.PipedInputStream.)
        os        (java.io.PipedOutputStream. is)
        output    (org.apache.batik.transcoder.TranscoderOutput. os)]
    (thread (. png-trans transcode input output)
            (. os close))
    is))

(defn get-keyboard [{:keys [board selected-positions] :as game-position} 
                    highlight-status move-status]
  (let [keyboard 
        (->> (visit-board
              board
              (fn [[layer row col] position]
                (let [position-info 
                      (merge
                       (get highlight-status (conj selected-positions position))
                       (get highlight-status (conj selected-positions :all)))]
                  (if (get position-info position)
                    (str (inc position)) nil))))
             (apply concat)
             (map #(remove nil? %))
             (remove empty?))]
    (if (:playable-move (get move-status selected-positions))
      (conj keyboard ["Play!"]) keyboard)))

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

(defmethod send-to-telegram :message [bot-id {:keys [chat-id text message-id parse-mode reply-markup]}]
  (send-telegram bot-id "sendMessage" {:query-params {:chat_id chat-id :text text :reply_to_message_id message-id :parse_mode parse-mode :reply_markup (generate-string reply-markup)}}))

;; TODO remove highlight-status from here and abstract away image creation + rendering from telegram client
(defmethod send-to-telegram :photo [bot-id {:keys [chat-id game-position highlight-status last-move message-id parse-mode caption]}]
  (let [{:keys [board 
                intermediate-board 
                selected-positions 
                player
                outcome]} game-position
        score (score game-position)]
    (log/debug "Game position to send" game-position)
    (send-telegram 
     bot-id "sendPhoto"
     {:query-params 
      {:chat_id chat-id 
       :reply_to_message_id message-id 
       :parse_mode parse-mode 
       :caption (if intermediate-board "" 
                    (str "Balls left: White " (:white score) " - " (:black score) " Black"))}
      :multipart
      [{:name "photo" 
        :content (create-image 
                  (if intermediate-board intermediate-board board)
                  last-move
                  highlight-status selected-positions)
        :filename "board.png"}]})))
;; END OF TELEGRAM CLIENT

(defn- make-game-not-found-msg [client]
  {:type :message 
   :chat-id (:id client)
   :text "No running game found. Create a game using /new."})

(defn- add-or-replace-client [games client-id game-id]
  (-> games 
      (assoc-in [:clients client-id] game-id)))

(defn- remove-client [games client-id]
  (-> games 
      (update :clients dissoc client-id)))

(defn- add-game [games game-id]
  (-> games 
      (assoc-in [:games game-id] {:colors {:black #{} :white #{}}
                                  :users {}
                                  :started false})))

(defn- remove-game [games game-id]
  (-> games
      (update :games dissoc game-id)))

(defn- add-user-to-game [games game-id user-id username color]
  (-> games 
      (update-in [:games game-id :colors color] conj {:id user-id :username username})
      (update-in [:games game-id :users user-id] #(if (nil? %) #{color} (conj % color)))))

(defn- mark-game-as-started [games game-id]
  (-> games
      (assoc-in [:games game-id :started] true)))

(defn- get-game-for-client [games client-id game-id]
  (or game-id (get-in games [:clients client-id])))

(defn- has-infos-for-both-players [games game-id]
  (and (> (count (get-in games [:games game-id :colors :black])) 0)
       (> (count (get-in games [:games game-id :colors :white])) 0)))

(defn- get-missing-player [games game-id]
  (if (has-infos-for-both-players games game-id)
    nil
    (if (> (count (get-in games [:games game-id :colors :black])) 0)
      :white
      :black)))

(defn- get-user-in-game [games game-id color]
  (get-in games [:games game-id :colors color]))

;; MESSAGE PARSER FROM TELEGRAM
(defn- parse-new-game-data [args]
  [(new-pylos-game 4) :white])

(defn- parse-color-text [color-text]
  (case color-text
    "white" :white 
    "w" :white 
    "black" :black 
    "b" :black 
    nil))

(defn parse-args-keep-order [args]
  (log/debug "Parsing args from client" args)
  (map #(cond (try (number? (read-string %)) (catch Exception e false)) 
              [:number (read-string %)]
              (= "done" (->> % clojure.string/lower-case))
              [:done true]
              (not (nil? (parse-color-text %))) 
              [:color (parse-color-text %)]
              :else 
              [:game-id %]) 
       (remove nil? args)))

(defn parse-args-for-play [args]
  (let [parsed-args (parse-args-keep-order args)]
    (reduce (fn [acc [type value]]
              (case type
                :number (update acc :play conj value)
                :done (update acc :play conj :done)
                (assoc acc type value))) {:play []}  parsed-args)))

(defn parse-args [args]
  (into {} (parse-args-keep-order args)))

(defmulti parse-telegram-message (fn [[command & args] games client user message] command))

(defmethod parse-telegram-message "/new" [[_ & args] games client user message]
  (if-let [[game first-player] (parse-new-game-data args)]

    (if-not (get-in games [:clients (:id client)])
      [(add-or-replace-client games (:id client) true)
       [[:gamerunner (->NewGameCommand client game first-player)]]]
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "A game was already found in this channel. Stop it first using /stop to create a new game."
                          :message-id (:message_id message)}]]])
    ;; TODO didn't understand
    [games []]))

(defmethod parse-telegram-message "/stop" [[_ & args] games client user message]
  (let [{:keys [game-id]} (parse-args args)
        game-id           (get-game-for-client games (:id client) game-id)]
    (if (not game-id)
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "No game was found in this channel. Start a new game first using /new."
                          :message-id (:message_id message)}]]]
      [(-> games
           (remove-client (:id client))
           (remove-game game-id))
       [[:gamerunner (->UnsubscribeCommand client game-id)]
        [:gamerunner (->StopGameCommand client game-id)]
        [:telegram {:type :message
                    :chat-id (:id client)
                    :text "Game stopped. To start a new game, use /new."
                    :message-id (:message_id message)}]]])))

(defn start-game-if-necessary [messages games client game-id]
  ;; TODO game is already started
  (if (get-in games [:games game-id :started])
    [games messages]
    (if (has-infos-for-both-players games game-id)
      [(-> games 
           (mark-game-as-started game-id))
       (conj messages 
             [:gamerunner (->StartGameCommand client game-id)]
             [:telegram {:type :message
                         :chat-id (:id client)
                         :text "Both players have joined, your game is starting! To play, use the keyboard below or \"/play <position>\"."}])]
      [games (conj messages
                   [:telegram {:type :message
                               :chat-id (:id client)
                               :text (str "The game still needs a " (name (get-missing-player games game-id)) " player, it will start as soon as one have joined.")}])])))

;; TODO disallow 2 human players on same color
(defn- handle-join [games client user message args]
  (let [{:keys [game-id color]} (parse-args args)
        game-id                 (get-game-for-client games (:id client) game-id)
        existing-colors         (get-in games [:games game-id :users (:id user)])]
    (cond
      (not game-id)
      [games [[:telegram (make-game-not-found-msg client)]]]
      (> (count existing-colors) 0)
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "You have already joined this game."
                          :message-id (:message_id message)}]]]
      (not color)
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "Pick a color below."
                          :message-id (:message_id message)
                          :reply-markup {:keyboard [["White", "Black"]]
                                         :one_time_keyboard true
                                         :selective true}}]]]
      :else
      (let [new-games (add-user-to-game games game-id (:id user) (:username user) color)]
        (let [messages
              [[:gamerunner (->SubscribeCommand client game-id)]
               [:gamerunner (->JoinGameCommand client user 
                                               game-id color :encoded)]
               ;; TODO wait for game runner ACK
               [:telegram {:type :message
                           :chat-id (:id client)
                           :text (str "You joined the game as " (name color) ".")
                           :message-id (:message_id message)}]]]
          (start-game-if-necessary messages new-games client game-id))))))

(defmethod parse-telegram-message "black" [[_ & args] games client user message]
  (handle-join games client user message ["black"]))

(defmethod parse-telegram-message "white" [[_ & args] games client user message]
  (handle-join games client user message ["white"]))

(defmethod parse-telegram-message "/join" [[_ & args] games client user message]
  (handle-join games client user message args))

(defn- handle-bot [games client user message args]
  (let [{:keys [game-id color number]} (parse-args args)
        user-id                 "bot"
        game-id                 (get-game-for-client games (:id client) game-id)
        existing-colors         (get-in games [:games game-id :users user-id])]
    (cond
      (not game-id)
      [games [[:telegram (make-game-not-found-msg client)]]]
      (not color)
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :message-id (:message_id message)
                          :text "Pick a color below for the bot."
                          :reply-markup {:keyboard [["White-Bot", "Black-Bot"]]
                                         :one_time_keyboard true
                                         :selective true}}]]]
      (contains? existing-colors color)
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "There is already a bot of this color on this game."
                          :message-id (:message_id message)}]]]
      :else
      (let [new-games (add-user-to-game games game-id user-id nil color)]
        (let [strategy-options {:type :negamax 
                                :options {:depth (if (number? number) number 4)}}]
          (let [messages
                [[:gamerunner (->SubscribeCommand client game-id)]
                 [:gamerunner 
                  (->NPCCommand client user game-id color strategy-options)]
                 ;; TODO wait for game runner ACK
                 [:telegram {:type :message
                             :chat-id (:id client)
                             :text (str "The bot has joined the game as " (name color) ".")
                             :message-id (:message_id message)}]]]
            (start-game-if-necessary messages new-games client game-id)))))))

(defmethod parse-telegram-message "white-bot" [[_ & args] games client user message]
  (handle-bot games client user message ["white"]))

(defmethod parse-telegram-message "black-bot" [[_ & args] games client user message]
  (handle-bot games client user message ["black"]))

(defmethod parse-telegram-message "/bot" [[_ & args] games client user message]
  (handle-bot games client user message args))

;; TODO error : it is not your turn to play
(defn- handle-play [games client user message args]
  (let [{:keys [game-id color play]} (parse-args-for-play args) 
        game-id                  (get-game-for-client games (:id client) game-id)
        joined-colors            (get-in games [:games game-id :users (:id user)])]
    (cond 
      (and (not color) (= 2 (count joined-colors)))
      [games [[:telegram {:type :message
                     :chat-id (:id client)
                     :text "You have joined both as black and white, please specify the color you want to play using \"/play white <position>\" or \"/play black <position>\""}]]]
      (and color (= 1 (count joined-colors)) (not= (first joined-colors) color))
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "You haven't joined as " (name color) "."}]]]
      (= 0 (count joined-colors))
      [games [[:telegram {:type :message
                          :chat-id (:id client)
                          :text "You haven't joined any games yet. Please create a game using /new and join it using /join."}]]]
      :else
      [games
       [[:gamerunner
          (->PlayerMoveCommand
           client user
           (get-game-for-client games (:id client) game-id)
           (or color (first joined-colors))
           (map #(if (number? %) (dec %) %) play))]]])))

(defmethod parse-telegram-message "/play" [[_ & args] games client user message]
  (handle-play games client user message args))

(defmethod parse-telegram-message "play!" [[_ & args] games client user message]
  (handle-play games client user message ["done"]))

(defn- handle-start [games client user message]
  [games [[:telegram {:type :message
                      :chat-id (:id client)
                      :parse-mode "Markdown"
                      :text (str "Hi, I am game-bot! You can play against me or other people:

 - /new to start a new game
 - /stop to stop a running game")}]]])

(defmethod parse-telegram-message "/start" [[_ & args] games client user message]
  (handle-start games client user message))

(defmethod parse-telegram-message nil [_ games client user message]
  (cond
    (:group_chat_created message) (handle-start games client user message)
    ;; TODO unhardcode
    (= "franztestbot" (get-in message [:new_chat_participant :username])) (handle-start games client user message)
    :else [games []]))

(defmethod parse-telegram-message :default [args games client user message]
  (let [parsed-args (parse-args args)]
    (if (and (= 1 (count parsed-args)) (:number parsed-args))
      (handle-play games client user message args)
      [games [[:telegram {:type :message 
                          :chat-id (:id client)
                          :text "Sorry, did not get that"
                          :message-id (:message_id message)}]]])))
;; END OF MESSAGE PARSER

(defmulti handle-gamerunner-message (fn [{:keys [type]} games] type))

(defmethod handle-gamerunner-message :msg/new-game [{:keys [client game-id]} games]
  [(-> games 
       (add-or-replace-client (:id client) game-id)
       (add-game game-id))
   [[:telegram {:type :message
                :chat-id (:id client)
                :parse-mode "Markdown"
                :text (str "Welcome to your new game! Now, people in this channel can:

 - Join this game using /join.
 - Have the bot join the game using /bot. 
 - Play online, by following [this link](http://localhost:8888/#/game/" game-id ").""

Your game will start as soon as a black and a white player have joined.")}]]])

(defmethod handle-gamerunner-message :msg/game-infos [{:keys [type client game-id game-infos]} games]
  (let [{:keys [board player] :as game-position} 
                          (:game-position game-infos)
        moves             (generate-moves game-position)
        highlight-status  (highlight-status board moves)
        move-status       (move-status board player moves)
        current-users     (get-user-in-game games game-id (:player game-position))
        current-usernames (remove nil? (map :username current-users))
        messages [[:telegram {:type :photo 
                              :chat-id (:id client) 
                              :game-position (:game-position game-infos)
                              :last-move (:last-move game-infos)
                              :highlight-status highlight-status}]]
        messages (if (empty? current-usernames) messages
                   (conj messages 
                         [:telegram {:type :message
                                     :chat-id (:id client)
                                     :text (str (clojure.string/join ", " (map #(str "@" %) current-usernames)) ": your turn!")
                                     :reply-markup {:keyboard (get-keyboard game-position highlight-status move-status)
                                                    :one_time_keyboard true
                                                    :selective true}}]))
        outcome  (get-in game-infos [:game-position :outcome])]
    (if outcome
      [(-> games
           (remove-client (:id client))
           (remove-game game-id))
       (conj messages 
             [:telegram {:type :message
                         :chat-id (:id client)
                         :text (str "We have a winner! Player " (name outcome) " wins!
Start a new game using /new.")}] 
             [:gamerunner (->UnsubscribeCommand client game-id)]
             [:gamerunner (->StopGameCommand client game-id)])]
      [games messages])))

(defmethod handle-gamerunner-message :msg/errors [{:keys [client errors]} games]
  [games (remove nil? (map (fn [[field value error]] 
                             (case [field error]
                               [:game-id :not-found]
                               [:telegram (make-game-not-found-msg client)])) errors))])

(defmethod handle-gamerunner-message :default [message games]
  (log/debug "Unrecognized message to forward" message)
  [games []])

(defn- get-client [chat-id user-ch]
  {:id chat-id :channel user-ch})

(defn- get-user [chat-id from]
  {:id (str (:id from) "-" chat-id)
   :user-id (:id from)
   :username (or (:username from) (:first_name from))})

(defn- forward-messages [messages gamerunner-ch bot-id] 
  (log/debug "Forwarding messages" messages)
  (doseq [[dest message] messages]
    (case dest
      :gamerunner 
      (>!! gamerunner-ch message)
      :telegram
      @(send-to-telegram bot-id message))))

(defn- create-or-retrieve-client-channel [games client gamerunner-ch bot-id]
  (when-not (get-in @games [:channels (:id client)])
    (let [client-ch (chan 100)]
      (swap! games assoc-in [:channels (:id client)] client-ch)
      (go-loop []
        (let [messages (<! client-ch)]
          (when messages
            (forward-messages messages gamerunner-ch bot-id)
            (recur))))))
  (get-in @games [:channels (:id client)]))

(defn- sanitize-input [text]
  (let [[first-arg & rest] (into [] (remove clojure.string/blank? 
                            (clojure.string/split text #" ")))]
    (if (clojure.string/starts-with? first-arg "/")
      (cons (clojure.string/lower-case 
             (get (clojure.string/split first-arg #"@") 0)) rest)
      (cons (clojure.string/lower-case first-arg) rest))))

;; TODO I18n write a function to transform ID to text messages

;; TODO maybe write a handler protocol so that we can 
;; reuse this particular method and retrieve-message
(defn- event-msg-handler* [bot-id games gamerunner-ch user-ch]
  (fn [{:as ev-msg :keys [body]}]
    (log/debug "Got message from telegram client" body)
    (let [{{:keys [text chat from]} :message} body
          client               (get-client (:id chat) user-ch)
          user                 (get-user (:id chat) from)
          [new-games messages] (parse-telegram-message 
                                (if text (sanitize-input text) text) 
                                @games client user (:message body))]
      (log/debug "Swapping game atom" new-games)
      (reset! games new-games)
      ;; we send this blocking to the client channel
      (>!! (create-or-retrieve-client-channel games client gamerunner-ch bot-id) 
           messages))))

(defn- gamerunner-msg-handler* [bot-id games gamerunner-ch]
  (fn [message]
    (let [[new-games messages] (handle-gamerunner-message message @games)]
      (log/debug "Swapping game atom" new-games)
      (reset! games new-games)
      (forward-messages messages gamerunner-ch bot-id))))

(defn- app-routes [event-msg-handler games]
  (routes
   (-> (GET "/inspect/telegram" [request] {:body (convert-to-json @games)})
       wrap-json-response)
   (-> (POST "/telegram" request (event-msg-handler request) {:body "ok"})
       (wrap-json-body {:keywords? true :bigdecimals? true})
       wrap-json-response)))

(defrecord TelegramHandler [bot-id games]
  Handler
  (start-handler [handler gamerunner-ch]
    (let [user-ch           (start-event-handler 
                             (gamerunner-msg-handler* bot-id games gamerunner-ch))
          event-msg-handler (event-msg-handler* bot-id games gamerunner-ch user-ch)
          routes            (app-routes event-msg-handler games)]
      (assoc handler :routes routes :user-ch user-ch)))
  (stop-handler [handler]
    (if-let [user-ch (:user-ch handler)]
      (close! user-ch)))
  (get-routes [handler]
    (:routes handler)))

(defn telegram-handler [bot-id]
  (map->TelegramHandler {:bot-id bot-id :games (atom {:clients {} :games {} :channels {}})}))
