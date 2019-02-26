(ns dashboard.events.mqtt
  (:require
    [dashboard.mqtt :as mqtt]
    [re-frame.core :as re-frame]
    [dashboard.db :refer [id-map switch]]))

(re-frame/reg-event-db
  ::connect
  (fn [db [_ device topics]]
    (assoc db :mqtt-connection (mqtt/connect device topics {:success           ::established
                                                            :failure           ::failed
                                                            :message-arrived   ::arrived
                                                            :message-delivered ::delivered
                                                            :lost              ::lost}))))

(re-frame/reg-event-fx
  ::established
  (fn [_ [_ topics]]
    {:dispatch-n (vec (map
                        (fn [[topic handle-success]] [::subscribe topic {:success handle-success
                                                                         :failure ::subscribe-failed}])
                        topics))}))

(re-frame/reg-event-db
  ::failed
  (fn [db _]
    db))

(re-frame/reg-event-db
  ::arrived
  (fn [db [_ message]]
    (println "message-arrived: " message)
    (let [pin-type {:light 13 :water 16}
          pin-value {:light (get (:result message) (keyword (str "ipin" (:light pin-type))))
                     :water (get (:result message) (keyword (str "ipin" (:water pin-type))))}]
      (cond
        ; Config.Get
        ;(:gpio_switch (:result message)) (let [pins (:vals (js->clj
        ;                                                     (.parse js/JSON (str "{\"vals\":" (:pins (:gpio_switch (:result message))) "}"))
        ;                                                     :keywordize-keys true))
        ;                                       pin-ids (map (fn [pin] (:pin pin)) pins)]
        ;                                   (assoc db :funcs (id-map
        ;                                                      (vec
        ;                                                        (map
        ;                                                          (fn [id]
        ;                                                            (condp = id
        ;                                                              (:light pin-type) (switch :light id "Свет" false)
        ;                                                              (:water pin-type) (switch :water id "Полив" false))) pin-ids)))))
        ; Planter.GetStats
        (some int? (vals pin-value)) (assoc db :funcs (id-map
                                                        (vec (map
                                                               (fn [[k v]] (condp = k
                                                                             :light (switch :light (:light pin-type) "Свет" (= 1 v))
                                                                             :water (switch :water (:water pin-type) "Полив" (= 1 v))))
                                                               pin-value))))
        (string? (:result message)) (let [result (js->clj (.parse js/JSON (:result message)) :keywordize-keys true)]
                                      (cond
                                        (= "schedule" (:name result)) (assoc db :schedule (first (:schedule result)))
                                        :else db))
        :else db))))

(re-frame/reg-event-db
  ::delivered
  (fn [db [_ message]]
    (println "message-delivered: " message)
    db))

(re-frame/reg-event-db
  ::lost
  (fn [db [_ response]]
    (println "MQTT lost" response)
    (assoc db :mqtt-connection nil)))

(re-frame/reg-event-db
  ::request
  (fn [db [_ payload]]
    (let [device (get (:devices db) (:active-device db))
          connection (:mqtt-connection db)]
      (mqtt/request connection device payload))
    db))

(re-frame/reg-event-db
  ::subscribe
  (fn [db [_ topic handle]]
    (let [connection (:mqtt-connection db)]
      (mqtt/subscribe connection topic handle))
    db))

(re-frame/reg-event-db
  ::subscribe-failed
  (fn [db [_ topic]]
    (println "Subscription on" topic "failed")
    db))
