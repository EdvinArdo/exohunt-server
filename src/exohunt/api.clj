(ns exohunt.api
  (:require [exohunt.core :refer [move-char]]))

(defn handle-move
  [state char-id data]
  (let [direction (clojure.core/keyword (:direction data))]
    (case direction
      (:left :right :up :down) (move-char state char-id direction)
      state)))

(defn handle-message
  [state char-id message]
  (let [event (clojure.core/keyword (:event message))
        data (:data message)]
    (case event
      :move (handle-move state char-id data)
      state)))
