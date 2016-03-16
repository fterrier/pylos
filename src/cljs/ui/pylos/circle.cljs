(ns ui.pylos.circle
  (:require
   [om.next :as om :refer-macros [defui ui]]
   [om-tools.dom :as dom :include-macros true]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))

;; TODO make just a function
(defn circle [props]
  (let [{:keys [color hover highlight on-select on-mouse-over on-mouse-out position]} props]
    (dom/div {:class "circle-container"}
             (dom/figure {:class
                          (str "circle circle-" (name color) " "
                               (when hover
                                 (str "circle-hover-" (name hover)))
                               " "
                               (when highlight
                                 (str "circle-highlight-" (name highlight))))
                          :on-click (fn [e] (on-select position))
                          :on-mouse-over (fn [e] (on-mouse-over position))
                          :on-mouse-out (fn [e] (on-mouse-out position))}))))

(defui ^private Circle
  Object
  (render [this]
          (circle (om/props this))))

(defcard cell-white-card
  (dc/om-next-root Circle)
  {:color :white} {:inspect-data true})

(defcard cell-black-card
  (dc/om-next-root Circle)
  {:color :black} {:inspect-data true :classname "gray"})

(defcard cell-open-card
  (dc/om-next-root Circle)
  {:color :open} {:inspect-data true})

(defcard cell-open-hover-red
  "Circles with hover state also have a pointer cursor"
  (dc/om-next-root Circle)
  {:color :open :hover :red} {:inspect-data true})

(defcard cell-open-hover-green
  "Circles with hover state also have a pointer cursor"
  (dc/om-next-root Circle)
  {:color :open :hover :green} {:inspect-data true})

(defcard cell-open-highlight-red
  (dc/om-next-root Circle)
  {:color :open :highlight :red} {:inspect-data true})

(defcard cell-open-highlight-green
  (dc/om-next-root Circle)
  {:color :open :highlight :green} {:inspect-data true})

(defcard cell-open-highlight-green-hover-red
  (dc/om-next-root Circle)
  {:color :open :highlight :green :hover :red} {:inspect-data true})

(defcard cell-open-highlight-green-hover-red
  (dc/om-next-root Circle)
  {:color :black :highlight :green :hover :red} {:inspect-data true})
