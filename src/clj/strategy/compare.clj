(ns strategy.compare
  (:require [game.game :refer :all]
            [io.aviso.ansi :refer :all]))


(defn format-infos [infos fun pad highlight-highest]
  (let [info-list (map fun infos)
        highest   (apply max info-list)
        lowest    (apply min info-list)]
    (reduce (fn [output info]
              (let [info-str       (fun info)
                    info-formatted (format (str "%" pad "s") info-str)
                    info-str       (if (or 
                                   (and highlight-highest (= highest info-str))
                                   (and (not highlight-highest) (= lowest info-str))) (green info-formatted) info-formatted)]
                (str output info-str " | " )))
            "" infos)))

(defn display-compare-additional-infos [infos1 infos2]
  (doseq [[depth infos] (group-by :depth (concat infos1 infos2))]
    (println "Depth" depth)
    (println "Total number of moves |" (format-infos infos #(:total-moves (:stats %)) 20 false))
    (println "  - calculated        |" (format-infos infos #(:calculated-moves (:stats %)) 20 false))
    (println "  - lookup            |" (format-infos infos #(:lookup-moves (:stats %)) 20 false))
    (println "Moves per ms          |" (format-infos infos #(:moves-per-ms %) 20 true))
    (println "Total time            |" (format-infos infos #(:time %) 20 false))
    (println )))


(def test-infos1 [{:depth 1, :negamax-values {:best-possible-score -1/2, :outcome nil}, :time 0.285324, :moves-per-ms 56.076600636469415, :stats {:calculated-moves 16, :lookup-moves 0, :total-moves 16}} {:depth 2, :negamax-values {:best-possible-score 0, :outcome nil}, :time 2.22484, :moves-per-ms 13.484115711691627, :stats {:calculated-moves 30, :lookup-moves 0, :total-moves 30}} {:depth 3, :negamax-values {:best-possible-score -1/2, :outcome nil}, :time 7.261224, :moves-per-ms 32.77684313278312, :stats {:calculated-moves 132, :lookup-moves 106, :total-moves 238}} {:depth 4, :negamax-values {:best-possible-score 0, :outcome nil}, :time 29.330743, :moves-per-ms 14.251258483291746, :stats {:calculated-moves 245, :lookup-moves 173, :total-moves 418}}])
(def test-infos2 [{:depth 1, :negamax-values {:best-possible-score 0, :outcome nil}, :time 0.225624, :moves-per-ms 66.48228911817893, :stats {:calculated-moves 15, :lookup-moves 0, :total-moves 15}} {:depth 2, :negamax-values {:best-possible-score 1/2, :outcome nil}, :time 1.95682, :moves-per-ms 14.308929794258031, :stats {:calculated-moves 28, :lookup-moves 0, :total-moves 28}} {:depth 3, :negamax-values {:best-possible-score 0, :outcome nil}, :time 5.8749, :moves-per-ms 35.40485795502902, :stats {:calculated-moves 116, :lookup-moves 92, :total-moves 208}} {:depth 4, :negamax-values {:best-possible-score 1/2, :outcome nil}, :time 64.293095, :moves-per-ms 12.7540912441686, :stats {:calculated-moves 558, :lookup-moves 262, :total-moves 820}}])