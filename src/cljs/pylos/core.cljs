(ns ^:figwheel-no-load pylos.core
  (:require [pylos.game.ui :as ui]))

(enable-console-print!)

(defn fig-reload []
  (ui/main))

(ui/main)