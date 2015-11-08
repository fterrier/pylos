(ns ^:figwheel-no-load pylos.core
  (:require [pylos.game.app :as app]))

(enable-console-print!)

(defn fig-reload []
  (app/stop)
  (app/main))

(app/main)
