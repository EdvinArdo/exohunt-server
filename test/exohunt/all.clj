(ns exohunt.all
  (:require [clojure.test :refer [run-tests successful?]]
            [ysera.test :refer [deftest is]]
            [exohunt.core]
            [exohunt.definitions]
            [exohunt.definitions-loader]))

(deftest test-all
         "Bootstrapping with the required namespaces, finds all the exohunt.* namespaces (except this one),
         requires them, and runs all their tests."
         (let [namespaces (->> (all-ns)
                               (map str)
                               (filter (fn [x] (re-matches #"exohunt\..*" x)))
                               (remove (fn [x] (= "exohunt.all" x)))
                               (map symbol))]
           (is (successful? (time (apply run-tests namespaces))))))
