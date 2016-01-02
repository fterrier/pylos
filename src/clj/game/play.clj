(ns game.play
  (:require [clojure.core.async :refer [<! >! alt! chan close! go-loop]]
            [clojure.tools.logging :as log]
            [game
             [game :refer [initial-game-position make-move move-allowed?]]
             [strategy :refer [choose-next-move get-input-channel]]]))

(defn- wait-for-valid-move [game-position strategy result-ch color]
  {:pre [(not (or (nil? game-position) 
                  (nil? strategy) 
                  (nil? result-ch) 
                  (nil? color)))]}
  (let [start-time  (System/nanoTime)]
    (go-loop []
      (log/debug "Looping for valid move on position" game-position)
      (let [; TODO add a timeout here
            game-result (alt! 
                          result-ch :closed
                          (choose-next-move strategy game-position)
                          ([value _] value))
            end-time    (System/nanoTime)]
        (log/debug "Got game result" game-result)
        (if (= :closed game-result) :closed
            (if (or (:next-game-position game-result)
                    (and (:next-move game-result)
                         (= (:color (:next-move game-result)) color)
                         (try (move-allowed? game-position (:next-move game-result))
                              (catch Exception e 
                                (log/error "Exception on move allowed" e) 
                                false))))
              (let [next-game-position
                    (or (:next-game-position game-result)
                        (make-move game-position (:next-move game-result)))]
                [next-game-position 
                 (- end-time start-time) 
                 (:additional-infos game-result) 
                 (:next-move game-result)])
              (do
                (log/debug "Did not get right move, recurring" game-result)
                (recur))))))))

(defn- end-game [result-ch strategies]
  (close! result-ch)
  (doseq [[_ strategy] strategies]
    (when-let [channel (get-input-channel strategy)]
      (log/debug "Closing input channel for strategy" strategy channel)
      (close! channel))))

(defn play-game
  ([game strategies]
    "Returns a channel where the consumer can ask for the next move"
    (let [result-ch (chan)]
        (play-game game strategies result-ch)
        result-ch))
  ([{:keys [game-position] :as game} strategies result-ch]
   (go-loop [[game-position time additional-infos last-move] 
             [game-position 0 nil nil]]
     (if (nil? game-position) 
       (do 
         (log/error "Received nil game-position, exiting game")
         (end-game result-ch strategies))
       (do
         (log/debug "Waiting to send move" last-move game-position)
         ;; TODO put in a timeout here
         ;; TODO it could be that it blocks here if result-ch is closed
         ;; after trying to put
         (when (>! result-ch
                   {:game-position game-position
                    :last-move last-move
                    :additional-infos additional-infos
                    :time time})
           (log/debug "Sent move" last-move game-position)
           (let [player    (:player game-position)
                 strategy  (get strategies player)]
             (if (:outcome game-position)
               (end-game result-ch strategies)
               (let [game-result (<! (wait-for-valid-move game-position 
                                                          strategy
                                                          result-ch
                                                          player))]
                (when-not (= :closed game-result)
                   (recur game-result)))))))))))

(defn play
  ([game {:keys [white black] :as strategies} first-player result-ch]
   (play-game {:game-position (initial-game-position game first-player)} strategies result-ch))
  ([game {:keys [white black] :as strategies} first-player]
    (play-game {:game-position (initial-game-position game first-player)} strategies)))
