(ns exohunt.definition.monster
  (:require [exohunt.definitions :as definitions]))

(def monster-definitions
  {
   "rat"
   {:type          :monster
    :name          "Rat"
    :max-health        10
    :min-damage    0
    :max-damage    2
    :move-cooldown 30}
   })

(definitions/add-definitions! monster-definitions)
