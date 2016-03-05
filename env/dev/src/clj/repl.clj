(ns repl
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [pylos.game :refer [new-pylos-game]]
            [server.game-runner
             :refer
             [->NewGameCommand ->NPCCommand ->StartGameCommand]]
            [user :refer [system]]
            [server.game-runner :refer [->SubscribeCommand]]
            [clojure.core.async :refer [go-loop]]
            [clojure.core.async :refer [<!]]))

;; (defn output-to-websockets [play websockets]
;;   (output-with-fn play
;;                   (fn [game-infos]
;;                     (println game-infos)
;;                     (send-infos websockets :sente/all-users-without-uid {:game-infos (get-game-infos game-infos)}))))

(def repl-user {:id "repl-user"})

(defn new-game []
  (let [repl-client   {:id "repl" :channel (chan)}
        gamerunner-ch (:gamerunner-ch system)
        response-ch   (:channel repl-client)]
    (>!! gamerunner-ch (->NewGameCommand repl-client (new-pylos-game 4) :white))
    [repl-client (:game-id (<!! response-ch))]))

(defn start-game [[repl-client game-id]]
  (let [gamerunner-ch (:gamerunner-ch system)]
    (>!! gamerunner-ch (->StartGameCommand repl-client game-id))
    [repl-client game-id]))

(defn add-negamax [[repl-client game-id] depth color]
  (let [gamerunner-ch (:gamerunner-ch system)]
    (>!! gamerunner-ch (->NPCCommand repl-client repl-user game-id color {:type :negamax :options {:depth depth}}))
    [repl-client game-id]))

(defn output-game [[repl-client game-id] function]
  (let [gamerunner-ch (:gamerunner-ch system)
        response-ch (:channel repl-client)]
    (>!! gamerunner-ch (->SubscribeCommand repl-client game-id))
    (go-loop []
      (when-let [move (<! response-ch)]
        (when (= (:type move) :msg/game-infos)
          (function (:game-infos move)))
        (recur)))
    [repl-client game-id]))
