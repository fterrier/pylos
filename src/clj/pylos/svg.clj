(ns pylos.svg
  (:require 
   [clojure.tools.logging :as log]
   [hiccup
    [core :refer [html]]
    [page :refer [xml-declaration]]]
   [pylos
    [pprint :refer [bold-positions-from-move]]
    [board :refer [cell visit-board]]]))

(defn print-board
  [board last-move highlight-status selected-positions]
  (html {:mode :xml}
        (xml-declaration "UTF-8")  
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"
        (into 
         [:svg {:version 1.1 :xmlns "http://www.w3.org/2000/svg" :xmlns:xlink "http://www.w3.org/1999/xlink" :x "0px" :y "0px" :width "400px" :height "400px"}]
         (cons 
          [:rect {:x 0 :y 0 :width 400 :height 400 :fill "lightgrey"}]
          (->> (visit-board
                board 
                (fn [[layer row col] position]
                  (let [cell  (cell board position)
                        x     (+ (* 100 col) (* (inc layer) 50))
                        y     (+ (* 100 row) (* (inc layer) 50))
                        border-color (if (or (contains? (into #{} selected-positions)
                                                        position)
                                             ;; TODO replace by good abstraction
                                             (contains? (into #{} (bold-positions-from-move last-move))
                                                        position))
                                       "red"
                                       (case cell
                                         :open "grey"
                                         :black "grey"
                                         :white "grey"
                                         :no-acc nil))
                        font-color   (case cell
                                       :open "black"
                                       :black "white"
                                       :white "black"
                                       :no-acc nil)
                        fill-color   (case cell
                                       :black "black"
                                       :white "white"
                                       :no-acc "none"
                                       :open "none")
                        fill-opacity (case cell
                                       :black 1.0
                                       :white 1.0
                                       :no-acc 0.0
                                       :open 0.0)
                        position-info (merge 
                                       (get highlight-status 
                                            (conj selected-positions position))
                                       (get highlight-status 
                                            (conj selected-positions :all)))
                        text           (when (get position-info position) 
                                         (inc position))]
                    [[:circle {:r 47
                               :cx x
                               :cy y
                               :stroke border-color
                               :stroke-width "2px"
                               :fill-opacity fill-opacity 
                               :fill fill-color}]
                     [:text {:x x
                             :y y
                             :text-anchor "middle"
                             :stroke font-color
                             :fill font-color
                             :stroke-width "1px"
                             :dy ".3em"
                             :font-family "Helvetica"
                             :font-size "32px"} 
                      (if text text "")]])))
               (apply concat)
               (apply concat)
               (apply concat))))))

;;(print-board (create-board {:board [:black :black :open :open :white :white :white :open :open :open :open :open :open :open :open :open :open :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc] :size 4}) nil)
