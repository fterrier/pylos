(ns system.repl
  (:require [game.output :refer [output-with-fn]]

            [server.handlers.websockets :refer [send-infos]]))

;; (defn output-to-websockets [play websockets]
;;   (output-with-fn play
;;                   (fn [game-infos]
;;                     (println game-infos)
;;                     (send-infos websockets :sente/all-users-without-uid {:game-infos (get-game-infos game-infos)}))))
