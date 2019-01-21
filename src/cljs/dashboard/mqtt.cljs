(ns dashboard.mqtt
  (:require [cljsjs.paho]
            [clojure.string :refer [join]]
            [cljs.reader :as reader]
            [cljs.core :refer [random-uuid]]
            [re-frame.core :as re-frame]))

(declare client)
(def broker "mqtt.agrofarm.city")

(defn connect [device handle]
  (let [mqtt (Paho.MQTT.Client. (str "ws://" broker ":15675" "/ws") (str (random-uuid)))
        options (js/Object.)]
    (set! (.-onConnectionLost mqtt) #(re-frame/dispatch [(:lost handle) %]))
    (set! (.-onMessageArrived mqtt) #(re-frame/dispatch [(:message-arrived handle)
                                                         (js->clj (.parse js/JSON (.-payloadString %)) :keywordize-keys true)]))
    (set! (.-onMessageDelivered mqtt) #(re-frame/dispatch [(:message-delivered handle)
                                                           (js->clj (.parse js/JSON (.-payloadString %)) :keywordize-keys true)]))
    (set! (.-onSuccess options) #(re-frame/dispatch [(:success handle)]))
    (set! (.-onFailure options) #(re-frame/dispatch [(:failure handle)]))
    (set! (.-mqttVersion options) 4)
    (set! (.-userName options) (:id device))
    (set! (.-password options) (:pass device))
    (.connect mqtt options)
    mqtt))

(defn request [connection device payload]
  (let [topic (str (:id device) "/rpc")
        src (str (:id device) "/response")
        payload (.stringify js/JSON (clj->js (assoc payload :src src)))]
    (println "Sending: " "t:" topic "p:" payload)
    (.send connection topic payload)))

(defn subscribe [connection topic handle]
  (let [options (js/Object.)]
    (set! (.-onSuccess options) #(re-frame/dispatch [(:success handle)]))
    (set! (.-onFailure options) #(re-frame/dispatch [(:failure handle)]))
    (.subscribe connection topic options)))
