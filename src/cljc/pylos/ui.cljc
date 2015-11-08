(ns pylos.ui
  (:require [pylos.board :refer [size ind square-corners]]))

(defn- all-permutations [positions]
  ; TODO order the removable positions in the move so that we don't see balls that are under other removable balls at first
  (let [position-vec (into [] positions)]
    (if (= 2 (count position-vec))
      [[(get position-vec 0) (get position-vec 1)]
       [(get position-vec 1) (get position-vec 0)]
       [(get position-vec 0)]
       [(get position-vec 1)]]
    [position-vec])))


(defn- info-map [info positions]
  (into {} (map (fn [position] [position info]) positions)))

; TODO maybe get rid of board here ?
; TODO make this nicer to read
(defn- highlight-infos [board {:keys [type position low-position original-move positions] :as move}]
  "Generates a map of {[<current-selections> highlighted-position] {<position1> <status> <position2> <status>}}"
  (case type
    :add    [{[position] {position {:addable true}}}]
    :rise   [{[low-position] {position {:addable true} low-position {:risable true}}}
             {[low-position :all] {position {:addable true}}}
             ; TODO do we display low positions when selecting high ?
             ; {[position] {:highlights {position {:addable true} low-position {:risable true}}}}
             ]
    :square (concat
              (highlight-infos board original-move)
              ; highlight in square positions
              (map (fn [position] {[(:position original-move)] {position {:in-square true}}}) (square-corners board (:square-position move)))
              (let [prefix (case (:type original-move)
                             :add  [(:position original-move)]
                             :rise [(:low-position original-move) (:position original-move)])]
                (concat
                 ; whenever original move is made, highlight all removable positions
                 [{(conj prefix :all) (info-map {:removable true} positions)}]
                 ; whenever a removable move is selected display the rest as removable
                 (map (fn [[first & rest]] {(conj prefix first :all) (info-map {:removable true} rest)}) (all-permutations positions)))))))

(defn move-infos [{:keys [type position low-position original-move positions] :as move} move-to-save playable-move current-selection]
  "Generates a map of {[current-selections] :moves ... :playable-move ...}"
  (case type
    :add    [{(into [] (cons position current-selection))                  {:moves [move-to-save] :playable-move playable-move}}]
    :rise   [{(into [] (cons low-position current-selection))              {:moves [move-to-save]}
              (into [] (concat [low-position position] current-selection)) {:moves [move-to-save] :playable-move playable-move}}]
    :square (concat
             (move-infos original-move move nil [])
             (mapcat #(move-infos original-move move move %) (all-permutations positions)))))

(defn merge-move-infos [{move-1 :moves play-1 :playable-move} {move-2 :moves play-2 :playable-move}]
  {:moves (concat move-1 move-2)
   :playable-move (if play-1 play-1 play-2)})

(defn highlight-status
  "we highlight different stuff if we are in the middle of a rise or square
  also the actual highlight position does not need to be saved, just the
  state of each balls in a map {position <state>} where <state> is
  {:addable :risable :in-square)} and position is [<current-selections> <highlighted-position>]"
  [board moves] (reduce #(apply merge-with merge %1 (highlight-infos board %2)) {} moves))

(defn move-status
  "we generate all possible path to moves"
  ([moves] (reduce #(apply merge-with merge-move-infos %1 (move-infos %2 %2 %2 [])) {} moves)))
