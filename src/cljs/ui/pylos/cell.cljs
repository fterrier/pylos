(ns ui.pylos.cell
  (:require
   [om.next :as om :refer-macros [defui ui]]
   [om-tools.dom :as dom :include-macros true]
   [ui.pylos.utils :as utils]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))

(defui  Cell
  Object
  (render [this]
          (let [{:keys [color hover highlight on-select on-mouse-over on-mouse-out position]} (om/props this)]
            (dom/div {:class
                      (str "circle circle-" (name color) " "
                           (when hover
                             (str "circle-hover-" (name hover)))
                           " "
                           (when highlight
                             (str "circle-highlight-" (name highlight))))
                      :on-click (fn [e] (on-select position))
                      :on-mouse-over (fn [e] (on-mouse-over position))
                      :on-mouse-out (fn [e] (on-mouse-out position))}
                     (utils/circle)))))

(def cell (om/factory Cell))

(defcard cell-white-card
  (dc/om-next-root Cell)
  {:color :white} {:inspect-data true})

(defcard cell-black-card
  (dc/om-next-root Cell)
  {:color :black} {:inspect-data true :classname "gray"})

(defcard cell-open-card
  (dc/om-next-root Cell)
  {:color :open} {:inspect-data true})

(defcard cell-open-hover-red
  "Cells with hover state also have a pointer cursor"
  (dc/om-next-root Cell)
  {:color :open :hover :red} {:inspect-data true})

(defcard cell-open-hover-green
  "Cells with hover state also have a pointer cursor"
  (dc/om-next-root Cell)
  {:color :open :hover :green} {:inspect-data true})

(defcard cell-open-highlight-red
  (dc/om-next-root Cell)
  {:color :open :highlight :red} {:inspect-data true})

(defcard cell-open-highlight-green
  (dc/om-next-root Cell)
  {:color :open :highlight :green} {:inspect-data true})

(defcard cell-open-highlight-green-hover-red
  (dc/om-next-root Cell)
  {:color :open :highlight :green :hover :red} {:inspect-data true})
