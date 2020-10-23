(ns exohunt.client
  (:require
    [exohunt.getters :refer [get-char
                             get-coords]]))

(def client-width 17)
(def client-height 13)

(defn get-client-character
  "Returns the client character."
  [state char-id]
  (let [char (get-char state char-id)]
    {:name   (:name char)
     :coords (:coords char)
     :id     (:id char)}))

(defn get-client-state
  "Returns the client state for the character with the given id."
  [state char-id events]
  (let [{x :x y :y} (get-coords state char-id)
        left (- x (/ (- client-width 1) 2))
        right (+ x (/ (+ client-width 1) 2))
        top (- y (/ (- client-height 1) 2))
        bottom (+ y (/ (+ client-height 1) 2))]
    (as-> {:map       (->> (subvec (:map state) top bottom)
                           (map (fn [row] (subvec row left right))))
           :character (get-client-character state char-id)} $
          (if (empty? events)
            $
            (assoc $ :events events)))))
