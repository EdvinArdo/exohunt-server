(ns exohunt.websocket
  (:require [exohunt.core :refer [init-game
                                  create-char
                                  spawn-char
                                  move-char
                                  decrement-cooldowns
                                  spawn-monster]]
            [exohunt.client :refer [get-client-state]]
            [exohunt.api :refer [handle-message-queues]]
            [clj-json-patch.core :refer [diff]]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<! timeout chan go]]
            [ysera.test :refer [is=]])
  (:use org.httpkit.server))

(defonce game-atom (atom (-> (init-game)
                             (create-char "Test Char" {:x 25 :y 24})
                             (create-char "Test Char" {:x 25 :y 26})
                             (spawn-char 0)
                             (spawn-char 1)
                             (spawn-monster "rat" {:x 26 :y 25}))))
(defonce connections (atom {}))
(defonce clients-atom (atom {}))
(defonce server (atom nil))

(defn get-available-char-id
  [state used-char-ids]
  (let [characters (:characters state)
        available-characters (filter (fn [char-id] (not (contains? used-char-ids char-id))) (keys characters))
        first-char-id (first available-characters)]
    first-char-id))

(defn on-close-handler
  [channel status]
  (let [char-id (@connections channel)]
    (do (swap! connections dissoc channel)
        (swap! clients-atom dissoc char-id))))

(defn on-open-handler
  [channel]
  (let [char-id (get-available-char-id @game-atom (vec (keys @clients-atom)))
        client-state-atom (atom nil)]
    ; Add watcher that sends client state on change
    (do (add-watch client-state-atom :watcher (fn [key ref old new] (when (not= old new)
                                                                      (send! channel (json/write-str new)))))
        (reset! client-state-atom (get-client-state @game-atom char-id '()))
        (swap! connections assoc channel char-id)
        (swap! clients-atom assoc char-id {:channel       channel
                                           :message-queue '()
                                           :state-atom    client-state-atom}))))

(defn on-receive-handler
  [channel message]
  ; Add message to client message queue
  (let [char-id (@connections channel)]
    (swap! clients-atom (fn [clients] (update-in clients
                                                 [char-id :message-queue]
                                                 conj
                                                 (json/read-str message :key-fn clojure.core/keyword))))))

(defn handler [request]
  (as-channel request {:on-close   on-close-handler
                       :on-open    on-open-handler
                       :on-receive on-receive-handler}))

(defn start-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil))
  (reset! server (run-server handler {:port 8080})))

(start-server!)

(defn clear-message-queues
  {:test (fn []
           (is= (-> {0 {:message-queue '("message")}}
                    (clear-message-queues)
                    (get-in [0 :message-queue]))
                '()))}
  [clients]
  (into {} (for [[key val] clients] {key (assoc val :message-queue '())})))

(defn game-loop!
  []
  (do
    ; Update cooldowns
    (swap! game-atom decrement-cooldowns)

    ; Handle message-queues
    (let [message-queues (reduce-kv (fn [acc char-id client] (assoc acc char-id (:message-queue client)))
                                    {}
                                    @clients-atom)]
      (let [{game-state :state
             events-map :events-map} (handle-message-queues @game-atom message-queues)]
        (do (reset! game-atom game-state)
            ; Clear message queues
            (swap! clients-atom clear-message-queues)

            ; Update client states
            (doseq [[char-id {channel       :channel
                              message-queue :message-queue
                              state-atom    :state-atom}] @clients-atom]
              (reset! state-atom (get-client-state @game-atom char-id (get events-map char-id)))))))))

; https://stackoverflow.com/questions/21404130/periodically-calling-a-function-in-clojure
(def game-loop-time (/ 1000 20))

(def the-condition (atom true))

(defn evaluate-condition []
  @the-condition)

(defn stop-periodic-function []
  (reset! the-condition false)
  )

(go (while (evaluate-condition)
      (<! (timeout game-loop-time))
      (game-loop!)))

;(stop-periodic-function)
