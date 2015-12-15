(ns game.play
  (:require [clojure.core.async :refer [<! >! alt! chan close! go-loop]]
            [game
             [game :refer [initial-game-position make-move move-allowed?]]
             [strategy :refer [choose-next-move get-input-channel]]]))

(defn- wait-for-valid-move [game-position strategy result-ch color]
  (let [start-time  (System/nanoTime)]
    (go-loop []
      (let [; TODO add a timeout here
            game-result (alt! 
                          result-ch :closed
                          (choose-next-move strategy game-position) ([value _] value))
            end-time    (System/nanoTime)]
        (println "Got game result" game-result)
        (if (= :closed game-result) :closed
          (if (and (:next-move game-result)
                   (= (:color (:next-move game-result)) color)
                   (try (move-allowed? game-position (:next-move game-result))
                        (catch Exception e false)))
            (let [next-game-position (make-move game-position (:next-move game-result))]
              [next-game-position 
               (- end-time start-time) 
               (:additional-infos game-result) 
               (:next-move game-result)])
            (recur)))))))

(defn- end-game [result-ch strategies]
  (close! result-ch)
  (doseq [strategy strategies]
    (when-not (nil? (get-input-channel strategy))
      (close! get-input-channel strategy))))

(defn- play-game
  ([game strategies]
    "Returns a channel where the consumer can ask for the next move"
    (let [result-ch (chan)]
        (play-game game strategies result-ch)
        result-ch))
  ([{:keys [game-position] :as game} strategies result-ch]
   (go-loop [[game-position time additional-infos last-move] [game-position 0 nil nil]]
     (let [player    (:player game-position)
           strategy  (get strategies player)]
       (println "Waiting to send move" last-move)
       ; TODO put in a timeout here
       (>! result-ch
           {:game-position game-position
            :last-move last-move
            :additional-infos additional-infos
            :time time})
       (println "Could send move " last-move game-position strategy)
       (if (:outcome game-position)
         (end-game result-ch strategies)
         (let [game-result (<! (wait-for-valid-move game-position strategy result-ch player))]
           (when-not (= :closed game-result)
             (recur game-result))))))))

(defn play
  ([game {:keys [white black] :as strategies} first-player result-ch]
   (play-game {:game-position (initial-game-position game first-player)} strategies result-ch))
  ([game {:keys [white black] :as strategies} first-player]
    (play-game {:game-position (initial-game-position game first-player)} strategies)))
