(ns ^:figwheel-no-load pylos.core
  (:require [pylos.game.app :as app]))

(set-print-fn! #())

(app/init)
(app/main)
