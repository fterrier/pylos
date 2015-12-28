(ns pylos.svg
  (:require [hiccup
             [core :refer [html]]
             [page :refer [xml-declaration]]]
            [pylos
             [board :refer [cell]]
             [init :refer [create-board visit-board]]]))

(defn print-board
  [board last-move]
  (html {:mode :xml}
        (xml-declaration "UTF-8")
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"
        (into [:svg {:version 1.1 :xmlns "http://www.w3.org/2000/svg" :xmlns:xlink "http://www.w3.org/1999/xlink" :x "0px" :y "0px" :width "400px" :height "400px"}]
              (cons [:rect {:x 0 :y 0 :width 400 :height 400 :fill "lightgrey"}]
                    (->> (visit-board board 
                                      (fn [[layer row col] position]
                                        (let [cell (cell board position)]
                                          [:circle {:r 47 
                                                    :cx (+ (* 100 col) (* (inc layer) 50))
                                                    :cy (+ (* 100 row) (* (inc layer) 50))
                                                    :stroke (if-not (= cell :no-acc) "black")
                                                    :fill-opacity (case cell
                                                                    :black 1.0
                                                                    :white 1.0
                                                                    :no-acc 0.0
                                                                    :open 0.0) 
                                                    :fill (case cell
                                                            :black "black"
                                                            :white "white"
                                                            :no-acc "none"
                                                            :open "none")}])))
                         (apply concat)
                         (apply concat))))))

(print-board (create-board {:board [:black :black :open :open :white :white :white :open :open :open :open :open :open :open :open :open :open :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc :no-acc] :size 4}) nil)

  
