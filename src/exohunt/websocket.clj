(ns exohunt.websocket
  (:require [exohunt.core :refer [init-game
                                  create-char
                                  spawn-char
                                  move-char]]
            [exohunt.client :refer [get-client-state]]
            [exohunt.api :refer [handle-message]]
            [clj-json-patch.core :refer [diff]]
            [clojure.data.json :as json])
  (:use org.httpkit.server))

(defonce game (atom (-> (init-game)
                        (create-char "Test Char" {:x 25 :y 25})
                        (spawn-char 0))))
(defonce connections (atom {}))
(defonce clients (atom {}))
(defonce server (atom nil))

(defn get-available-char-id
  [state]
  (let [characters (:characters state)
        first-char-id (first (keys characters))]
    first-char-id))

(defn on-close-handler
  [channel status]
  (swap! connections dissoc channel))

(defn on-open-handler
  [channel]
  (let [char-id (get-available-char-id @game)
        client-state-atom (atom nil)]
    ; Add watcher that sends client state on change
    (do (add-watch client-state-atom :watcher (fn [key ref old new] (send! channel (json/write-str new))))
        (reset! client-state-atom (get-client-state @game char-id))
        (swap! connections assoc channel char-id)
        (swap! clients assoc char-id {:channel    channel
                                      :state-atom client-state-atom}))))

(defn on-receive-handler
  [channel message]
  ; Handle message
  (do (swap! game handle-message (@connections channel) (json/read-str message :key-fn clojure.core/keyword))
      ; Update client states
      (doseq [[char-id {channel    :channel
                        state-atom :state-atom}] @clients]
        (reset! state-atom (get-client-state @game char-id)))))

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
