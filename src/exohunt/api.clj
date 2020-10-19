(ns exohunt.api
  (:require [exohunt.core :refer [move-char]]
            [exohunt.getters :refer [can-move?]]))

(defn handle-move
  [state char-id data]
  (let [direction (clojure.core/keyword (:direction data))]
    (case direction
      (:left :right :up :down) (if (can-move? state char-id direction)
                                 (move-char state char-id direction)
                                 state)
      state)))

(defn handle-message
  [state char-id message]
  (let [event (clojure.core/keyword (:event message))
        data (:data message)]
    (case event
      :move (handle-move state char-id data)
      state)))

(defn handle-message-queue
  [state char-id message-queue]
  (reduce (fn [state message] (handle-message state char-id message))
          state
          message-queue))

(defn handle-message-queues
  [state message-queues]
  (reduce-kv (fn [state char-id message-queue] (handle-message-queue state char-id message-queue))
             state
             message-queues))
