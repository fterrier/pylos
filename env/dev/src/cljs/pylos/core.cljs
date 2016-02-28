(ns ^:figwheel-no-load pylos.core
  (:require [ui.pylos.cell :as cell]
            [ui.pylos.board :as board]
            [ui.pylos.counter :as counter]
            [ui.pylos.game :as game]))

(enable-console-print!)

(def fig-reload [])

;(defn fig-reload []
;   (app/start)
;   (app/main))

;(app/init)
;(app/main)
