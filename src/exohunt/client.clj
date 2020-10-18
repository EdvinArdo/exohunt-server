(ns exohunt.client
  (:require
    [exohunt.getters :refer [get-char
                             get-coords]]))

(def client-width 17)
(def client-height 13)

(defn get-client-state
  "Returns the client state for the character with the given id."
  [state char-id]
  (let [{x :x y :y} (get-coords state char-id)
        left (- x (/ (- client-width 1) 2))
        right (+ x (/ (+ client-width 1) 2))
        top (- y (/ (- client-height 1) 2))
        bottom (+ y (/ (+ client-height 1) 2))]
    {:map       (->> (subvec (:map state) top bottom)
                     (map (fn [row] (subvec row left right))))
     :character (get-char state char-id)}))