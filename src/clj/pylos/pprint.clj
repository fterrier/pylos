(ns pylos.pprint
  (:require [io.aviso.ansi :refer :all]
            [pylos.board :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))


(defn bold-positions-from-move [last-move]
  (case (:type last-move)
    :rise   [(:low-position last-move) (:position last-move)]
    :add    [(:position last-move)]
    :square [concat (:positions last-move) (bold-positions-from-move (:original-move last-move))]
    []))

(defn- print-cell [board position last-move]
  (let [cell            (cell board position)
        bold-positions  (into #{} (bold-positions-from-move last-move))
        bold?           (contains? bold-positions position)
        blocked?        (not (can-remove-ball board position))]
    (print (str (case cell
                  :black  (if bold? (inverse (red "b")) (if blocked? (red "b") (str csi 4 sgr (red "b") reset-font)))
                  :white  (if bold? (inverse (green "w")) (if blocked? (green "w") (str csi 4 sgr (green "w") reset-font)))
                  :no-acc (str "-")
                  :open   (if bold? (inverse "o") "o")
                  "") " "))))

(defn print-board
  ([board last-move]
   (println)
   (doseq [row (range 1 (+ 1 (board-size board)))]
     (doseq [layer (range 1 (+ 1 (board-size board)))]
       (doseq [col (range 1 (+ 2 (- (board-size board) layer)))]
         (let [position-coord [layer row col]
               position       (ind board position-coord)]
           (when-not (nil? position) (print-cell board position last-move))))
       (print "    "))
     (println))
   (println))
  ([board]
   (print-board board nil)))

(defn print-pylos-game [{{:keys [board player outcome]} :game-position, last-move :last-move, additional-infos :additional-infos, time :time :as play}]
  (let [time-ms (when time (/ time 1000000))]
  (println "====================")
  (if-not (nil? last-move)
    (let [last-move last-move]
      (println "Board after move of" (:color last-move))
      (println "====================")
      (println)
      (println "Move:" last-move)
      (println "Time:" (double time-ms)))
    (do
      (println "Initial board")
      (println "====================")))
  (print-board board last-move)
  (println "Balls remaining :")
  (println " - :white" (balls-remaining board :white))
  (println " - :black" (balls-remaining board :white))
  (println)
  (when additional-infos
    ; (pprint/pprint additional-infos)
    ; (println "Calculated moves per ms: " (double (/ (:calculated-moves (:stats additional-infos)) time-ms)))
    (println))
  (if outcome
    (println "We have a winner:" outcome)
    (println "Next move is for" player))
  (println))
  play)
