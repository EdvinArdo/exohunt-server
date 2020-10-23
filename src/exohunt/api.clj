(ns exohunt.api
  (:require [exohunt.core :refer [move-char
                                  init-game
                                  create-char
                                  spawn-char
                                  assoc-tile]]
            [exohunt.getters :refer [can-move?
                                     get-coords]]
            [ysera.test :refer [is=]]))

(defn handle-move
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (spawn-char 0)
                    (handle-move 0 {:direction "down"})
                    :state
                    (get-coords 0))
                {:x 25 :y 26})
           (is= (-> (init-game)
                    (assoc-tile {:x 25 :y 26} :walkable false)
                    (create-char "char" {:x 25 :y 25})
                    (spawn-char 0)
                    (handle-move 0 {:direction "down"})
                    :state
                    (get-coords 0))
                {:x 25 :y 25}))}
  [state char-id data]
  (let [direction (clojure.core/keyword (:direction data))]
    (let [new-state (case direction
                      (:left :right :up :down) (if (can-move? state char-id direction)
                                                 (move-char state char-id direction)
                                                 state)
                      state)]
      (if (= state new-state)
        {:state state}
        {:state new-state
         :event {:event     :move
                 :direction direction}}))))

(defn handle-message
  {:test (fn []
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (spawn-char 0)
                    (handle-message 0 {:event "move"
                                       :data  {:direction "down"}})
                    :state
                    (get-coords 0))
                {:x 25 :y 26})
           (is= (-> (init-game)
                    (create-char "char" {:x 25 :y 25})
                    (spawn-char 0)
                    (handle-message 0 {:event "move"
                                       :data  {:direction "down"}})
                    :event)
                {:event     :move
                 :direction :down}))}
  [state char-id message]
  (let [event (clojure.core/keyword (:event message))
        data (:data message)]
    (case event
      :move (handle-move state char-id data)
      state)))

(defn handle-message-queue
  {:test (fn []
           (let [test-state (-> (init-game)
                                (create-char "char" {:x 25 :y 25})
                                (spawn-char 0)
                                (handle-message-queue 0 '({:event "move"
                                                           :data  {:direction "down"}}
                                                          ; Second move event will not happen because of move cooldown
                                                          {:event "move"
                                                           :data  {:direction "down"}})))]
             (is= (-> (:state test-state)
                      (get-coords 0))
                  {:x 25 :y 26})
             (is= (-> (:events test-state))
                  '({:direction :down
                     :event     :move}))))}
  [state char-id message-queue]
  (reduce (fn [{state  :state
                events :events} message] (let [{new-state :state
                                                event     :event} (handle-message state char-id message)]
                                           {:state  new-state
                                            :events (if (some? event)
                                                      (conj events event)
                                                      events)}))
          {:state  state
           :events '()}
          message-queue))

(defn handle-message-queues
  {:test (fn []
           (let [test-state (-> (init-game)
                                (create-char "char0" {:x 25 :y 25})
                                (create-char "char1" {:x 25 :y 24})
                                (spawn-char 0)
                                (spawn-char 1)
                                (handle-message-queues {0 '({:event "move"
                                                             :data  {:direction "down"}})
                                                        1 '({:event "move"
                                                             :data  {:direction "down"}})}))]
             (is= (-> (:state test-state)
                      (get-coords 0))
                  {:x 25 :y 26})
             (is= (-> (:state test-state)
                      (get-coords 1))
                  {:x 25 :y 25})
             (is= (get-in test-state [:events-map 0])
                  '({:direction :down
                     :event     :move}))))}
  [state message-queues]
  (reduce-kv (fn [state char-id message-queue] (let [{new-state :state
                                                      events    :events} (handle-message-queue (:state state) char-id message-queue)]
                                                 {:state      new-state
                                                  :events-map (assoc (:events-map state) char-id events)}))
             {:state      state
              :events-map {}}
             message-queues))
