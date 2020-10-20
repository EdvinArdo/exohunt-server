(ns exohunt.core
  (:require [exohunt.getters :refer [get-counter
                                     get-char
                                     get-tile
                                     get-entity
                                     is-empty?
                                     get-new-coords
                                     get-coords
                                     get-move-cooldown]]
            [exohunt.definitions :refer [get-definition]]
            [ysera.test :refer [is=]]))

(defn init-tile
  "Initializes a new tile"
  ([tile-id]
   (let [definition (get-definition tile-id)]
     {:walkable (:walkable definition)
      :id       tile-id}))
  ([tile entity]
   (-> (init-tile tile)
       (assoc :entity entity))))

(defn init-map
  "Initializes a new map"
  [width height]
  (as-> (vec (repeat height (vec (repeat width {})))) $
        (map-indexed (fn [y row]
                       (vec (map-indexed (fn [x tile] (init-tile (if (or (<= y 10)
                                                                         (>= y 40)
                                                                         (<= x 10)
                                                                         (>= x 40))
                                                                   "water-tile"
                                                                   (rand-nth ["grass-tile" "water-tile" "stone-tile"]))))
                                         row)))
                     $)
        (vec $)
        (assoc-in $ [25 25] (init-tile "grass-tile"))
        (assoc-in $ [24 25] (init-tile "grass-tile"))
        (assoc-in $ [26 25] (init-tile "grass-tile"))
        (assoc-in $ [25 24] (init-tile "grass-tile"))
        (assoc-in $ [25 26] (init-tile "grass-tile"))))

(defn init-char
  "Initializes a new character"
  [counter name coords]
  {:name      name
   :id        counter
   :coords    coords
   :cooldowns {:move 0}})

(defn init-game
  "Initializes a new game"
  []
  (-> {:map        (init-map 50 50)
       :characters {}
       :counter    0}))

(defn create-char
  "Creates a new character and adds it to the state."
  {:test (fn []
           (is= (-> (create-char (init-game) "char" {:x 25 :y 25})
                    (get-char 0)
                    :name)
                "char"))}
  [state name coords]
  (let [char (init-char (get-counter state) name coords)]
    (assoc-in state [:characters (:id char)] char)))

(defn update-tile
  "Update tile at the given coords with the given function."
  {:test (fn []
           (is= (-> (init-game)
                    (update-tile {:x 25 :y 25} (fn [tile] (assoc tile :entity "entity")))
                    (get-tile {:x 25 :y 25})
                    :entity)
                "entity"))}
  [state coords fn]
  (update-in state [:map (:y coords) (:x coords)] fn))

(defn assoc-tile
  "Assoc key to val in tile at given coords."
  {:test (fn []
           (is= (-> (init-game)
                    (assoc-tile {:x 25 :y 25} :entity "entity")
                    (get-tile {:x 25 :y 25})
                    :entity)
                "entity"))}
  [state coords key val]
  (update-tile state coords (fn [tile] (assoc tile key val))))

(defn dissoc-tile
  "Dissoc key in tile at given coords."
  {:test (fn []
           (is= (-> (init-game)
                    (assoc-tile {:x 25 :y 25} :entity "entity")
                    (dissoc-tile {:x 25 :y 25} :entity)
                    (get-tile {:x 25 :y 25})
                    :entity)
                nil))}
  [state coords key]
  (update-tile state coords (fn [tile] (dissoc tile key))))

(defn assoc-entity
  "Assoc entity to val at given coords."
  {:test (fn []
           (is= (-> (init-game)
                    (assoc-entity {:x 25 :y 25} "entity")
                    (get-entity {:x 25 :y 25}))
                "entity"))}
  [state coords val]
  (assoc-tile state coords :entity val))

(defn dissoc-entity
  "Dissoc entity at given coords."
  {:test (fn []
           (is= (-> (init-game)
                    (assoc-entity {:x 25 :y 25} "entity")
                    (dissoc-entity {:x 25 :y 25})
                    (get-entity {:x 25 :y 25}))
                nil))}
  [state coords]
  (dissoc-tile state coords :entity))

(defn spawn-char
  "Spawns the character with the given id on the map at its coordinates."
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (spawn-char 0)
                    (get-entity {:x 25 :y 25}))
                0))}
  [state char-id]
  {:pre [(is-empty? state (get-coords state char-id))]}
  (assoc-entity state (get-coords state char-id) char-id))

(defn update-char
  "Updates the character with the given id with the given function."
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (update-char 0 (fn [char] (assoc char :name "name")))
                    (get-char 0)
                    :name)
                "name"))}
  [state char-id fn]
  {:pre [(contains? (:characters state) char-id)]}
  (update-in state [:characters char-id] fn))

(defn assoc-char
  "Assoc key to val in the character with the given id."
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (assoc-char 0 :name "name")
                    (get-char 0)
                    :name)
                "name"))}
  [state char-id key val]
  {:pre [(contains? (:characters state) char-id)]}
  (update-char state char-id (fn [char] (assoc char key val))))

(defn move-char
  "Moves the character with the given id in the given direction."
  {:test (fn []
           (let [state (-> (init-game)
                           (create-char "char" {:x 25 :y 25})
                           (spawn-char 0)
                           (move-char 0 :left))]
             (is= (get-coords state 0)
                  {:x 24 :y 25})
             (is= (get-entity state {:x 24 :y 25})
                  0)
             (is= (get-entity state {:x 25 :y 25})
                  nil)))}
  [state char-id direction]
  {:pre [(is-empty? state (get-new-coords state char-id direction))
         (<= (get-move-cooldown state char-id) 0)]}
  (let [old-coords (get-coords state char-id)
        new-coords (get-new-coords state char-id direction)]
    (-> (assoc-char state char-id :coords new-coords)
        (update-char char-id (fn [char] (assoc-in char [:cooldowns :move] 20)))
        (dissoc-entity old-coords)
        (assoc-entity new-coords char-id))))

(defn decrement-cooldowns
  "Decrements the cooldowns of all characters."
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (move-char 0 :down)
                    (decrement-cooldowns)
                    (get-move-cooldown 0))
                19))}
  [state]
  (update state :characters (fn [characters]
                              (reduce-kv (fn [acc key val]
                                           (assoc acc key (update-in val [:cooldowns :move] (fn [old-cd]
                                                                                              (if (> old-cd 0)
                                                                                                (- old-cd 1)
                                                                                                0)))))
                                         {}
                                         characters))))







































