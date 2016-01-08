(ns strategy.multi
  (:require [clojure.core :refer [compare-and-set! locking]]
            [clojure.core.async :refer [>! alts! chan close! go go-loop]]
            [clojure.tools.logging :as log]
            [game.strategy
             :refer
             [choose-next-move get-input-channel notify-end-game Strategy]]))

(defn add-strategies [strategy new-strategies]
  (locking (:ended strategy)
    (when-not @(:ended strategy)
      (swap! (:strategies strategy) merge new-strategies)
      ;; we notify the strategy that it has to re-loop on the moves
      (go (>! (:notify-ch strategy) :new)))))

(defn get-strategy [strategy key]
  (locking (:ended strategy)
    (get @(:strategies strategy) key)))

(defn- choose-next-move-from-channels [strategies game-position notify-ch]
  (go-loop [channel-map {}]
    (let [strategies-to-add (apply dissoc @strategies (keys channel-map))
          new-channel-map (merge channel-map (into {} (map (fn [[key strategy]] [key (choose-next-move strategy game-position)]) strategies-to-add)))]
      (log/debug "Listening on new channel map" new-channel-map)
      (let [[channel-response _] (alts! (conj (vals new-channel-map) notify-ch))]
        (log/debug "Got response from one channel" channel-response)
        (if (= :new channel-response)
          (recur new-channel-map)
          channel-response)))))

(defrecord MultiStrategy [strategies notify-ch ended]
  Strategy
  (choose-next-move [this game-position]
    (choose-next-move-from-channels strategies game-position notify-ch))

  (get-input-channel [this])
  (notify-end-game [this]
    (locking ended
      ;; afaik we don't need to compare-and-set! here since we have a lock, but hey
      (when (compare-and-set! ended false true)
        ;; this should take care of ending the loop on the moves
        (close! notify-ch)
        (doseq [[_ strategy] @strategies]
          (notify-end-game strategy))))))

(defn multi-channel []
  (map->MultiStrategy {:strategies (atom {}) :notify-ch (chan) :ended (atom false)}))
