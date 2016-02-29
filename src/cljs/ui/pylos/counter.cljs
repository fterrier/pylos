(ns ui.pylos.counter
  (:require
   [ui.pylos.utils :as utils]
   [om.next :as om :refer-macros [defui ui]]
   [om-tools.dom :as dom :include-macros true]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))

(defui Ball
  Object
  (render [this]
          (let [{:keys [color counter]} (om/props this)]
            (dom/div {:class (str "circle circle-" (name color))}
                     (dom/div {:class "circle-content"} counter)
                     (utils/circle)))))

(defcard last-white-ball
  (dc/om-next-root Ball)
  {:color :white :counter 1} {:inspect-data true})

(defcard last-black-ball
  (dc/om-next-root Ball)
  {:color :black :counter 1} {:inspect-data true})
