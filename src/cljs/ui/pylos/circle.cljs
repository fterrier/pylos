(ns ui.pylos.circle
  (:require
   [om.next :as om :refer-macros [defui ui]]
   [om-tools.dom :as dom :include-macros true]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))

(defn circle [props]
  (let [{:keys [text color hover highlight on-click on-mouse-over on-mouse-out]} props]
    (dom/span {:class "circle-container"}
              (dom/figure {:class
                           (str "circle circle-" (name color) " "
                                (when hover
                                  (str "circle-hover-" (name hover)))
                                " "
                                (when highlight
                                  (str "circle-highlight-" (name highlight))))
                           :on-click on-click
                           :on-mouse-over on-mouse-over
                           :on-mouse-out on-mouse-out}
                          (if text text " ")))))

(defui ^private Circle
  Object
  (render [this]
          (circle (om/props this))))

(defcard cell-with-text
  (dom/div 
   "Text "
   (circle
    {:color :white :text "10"}) " circle"))

(defcard cell-without-text
  (dom/div 
   "Text "
   (circle
    {:color :white}) " circle"))

(defcard cell-list
  (dom/div 
   "Text "
   (dom/ul {:class "circle-list collapsed"}
           (map #(dom/li (circle %)) [{:color :white :text "1"}
                                      {:color :black :text "2"}
                                      {:color :white :text "3"}]))
   " circle"))

(defcard cell-list-with-highlight
  (dom/div 
   "Text "
   (dom/ul {:class "circle-list collapsed"} 
           (dom/li (circle {:color :white :text "1"}))
           (dom/li {:class "is-highlighted"} 
                   (circle {:color :black :text "2"}))
           (dom/li (circle {:color :white :text "3"})))
   " circle"))

(defcard cell-list-with-highlight-last
  (dom/div 
   "Text "
   (dom/ul {:class "circle-list collapsed"} 
           (dom/li (circle {:color :white :text "1"}))
           (dom/li (circle {:color :black :text "2"}))
           (dom/li {:class "is-highlighted"} 
                   (circle {:color :white :text "3"}))
)
   " circle"))

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
