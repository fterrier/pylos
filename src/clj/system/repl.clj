(ns system.repl
  (:require [clojure.core.async :refer [<!! >!! chan]]
            [pylos.game :refer [new-pylos-game]]
            [server.game-runner :refer [->NewGameCommand]]
            [user :refer [system]]))

;; (defn output-to-websockets [play websockets]
;;   (output-with-fn play
;;                   (fn [game-infos]
;;                     (println game-infos)
;;                     (send-infos websockets :sente/all-users-without-uid {:game-infos (get-game-infos game-infos)}))))

(def repl-client {:id "repl" :channel (chan)})

(defn new-game []
  (let [gamerunner-ch (:gamerunner-ch system)
        response-ch   (:channel repl-client)]
    (>!! gamerunner-ch (->NewGameCommand repl-client (new-pylos-game 4) :white))
    (<!! response-ch)))
