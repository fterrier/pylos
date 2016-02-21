(ns ui.pylos.board
  (:require 
   [om.next :as om :refer-macros [defui ui]]
   [om.next.protocols :as p]
   [om-tools.dom :as dom :include-macros true]
   [devcards.core :as dc :refer-macros [defcard defcard-doc defcard-om-next]]))

(defui Board
  Object
  (render [this]
          (dom/div "Prout")))

(defcard test-card
  (dc/om-next-root Board)
  {})
