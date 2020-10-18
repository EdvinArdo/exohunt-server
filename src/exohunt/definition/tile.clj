(ns exohunt.definition.tile
  (:require [exohunt.definitions :as definitions]))

(def tile-definitions
  {
   "grass-tile"
   {:type     :tile
    :walkable true}

   "stone-tile"
   {:type     :tile
    :walkable true}

   "water-tile"
   {:type     :tile
    :walkable false}
   })

(definitions/add-definitions! tile-definitions)
