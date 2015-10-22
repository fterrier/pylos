(ns pylos.game.util
  (:require [om-tools.dom :as dom :include-macros true]))

(defn circle
  ([radius] (dom/svg {:width (* 2 radius) :height (* 2 radius)}
           (dom/circle {:cx radius :cy radius :r (* radius (/ 47 50))})))
  ([] (circle 50)))
