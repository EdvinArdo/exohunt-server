(ns exohunt.definitions
  (:require [ysera.test :refer [is= error?]]
            [ysera.error :refer [error]]))

; Here is where the definitions are stored
(defonce definitions-atom (atom {}))

(defn add-definitions!
  "Adds the given definitions to the game."
  [definitions]
  (swap! definitions-atom merge definitions))

(defn get-definitions
  "Returns all definitions in the game."
  []
  (vals (deref definitions-atom)))

(defn get-definition
  "Gets the definition identified by the id."
  {:test (fn []
           (is= (-> (get-definition "grass-tile")
                    :walkable)
                true)
           (error? (get-definition "Something that does not exist")))}
  [id]
  {:pre [(string? id)]}
  (let [definitions (deref definitions-atom)
        definition (get definitions id)]
    (when (nil? definition)
      (error (str "The id " id " does not exist. Are the definitions loaded?")))
    definition))
