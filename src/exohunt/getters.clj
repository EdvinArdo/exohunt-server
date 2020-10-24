(ns exohunt.getters
  (:require [ysera.test :refer [is
                                is-not
                                is=]]
            [exohunt.definitions :refer [get-definition]]))

(defn get-counter
  "Returns the counter from state."
  [state]
  (:counter state))

(defn get-and-increment-counter
  "Returns the counter from state."
  [state]
  {:counter (:counter state)
   :state   (update state :counter inc)})

(defn get-char
  "Returns the character with the given id."
  [state char-id]
  (get-in state [:characters char-id]))

(defn get-monster
  "Returns the character with the given id."
  [state monster-id]
  (get-in state [:monsters monster-id]))

(defn get-coords
  "Returns the coords of the character with the given id."
  [state char-id]
  (-> (get-char state char-id)
      :coords))

(defn get-tile
  "Returns the tile at the given coordinates."
  [state coords]
  (get-in state [:map (:y coords) (:x coords)]))

(defn get-entity
  "Returns the entity at the given coordinates."
  [state coords]
  (-> (get-tile state coords)
      :entity))

(defn is-empty?
  "Returns true if the tile at the given coordinates is empty."
  {:test (fn []
           (is (-> (get-definition "grass-tile")
                   (is-empty?)))
           (is-not (-> (get-definition "water-tile")
                       (is-empty?))))}
  ([tile]
   (and (true? (:walkable tile))
        (not (some? (:entity tile)))))
  ([state coords]
   (is-empty? (get-tile state coords))))

(defn get-coords-change
  "Returns the change in coordinates from given a direction."
  {:test (fn []
           (is= (get-coords-change :left)
                {:x -1 :y 0}))}
  [direction]
  (case direction
    :left {:x -1 :y 0}
    :right {:x 1 :y 0}
    :up {:x 0 :y -1}
    :down {:x 0 :y 1}))

(defn get-new-coords
  "Returns the new coordinates given the old coordinates and a direction."
  {:test (fn []
           (is= (get-new-coords {:x 25 :y 25} :left)
                {:x 24 :y 25}))}
  ([coords direction]
   (let [{x-change :x y-change :y} (get-coords-change direction)]
     {:x (+ (:x coords) x-change)
      :y (+ (:y coords) y-change)}))
  ([state char-id direction]
   (get-new-coords (get-coords state char-id) direction)))

(defn get-cooldowns
  "Returns the cooldowns of the character with the specified id."
  [state char-id]
  (-> (get-char state char-id)
      :cooldowns))

(defn get-move-cooldown
  "Returns the move cooldown of the character with the specified id."
  [state char-id]
  (-> (get-cooldowns state char-id)
      :move))

(defn can-move?
  "Returns true if the character with the given id can move in the given direction."
  [state char-id direction]
  (and (is-empty? state (get-new-coords state char-id direction))
       (<= (get-move-cooldown state char-id) 0)))
