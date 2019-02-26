(ns dashboard.events.rpc
  (:require
    [re-frame.core :as re-frame]
    [dashboard.events.mqtt :as mqtt]))

(re-frame/reg-event-fx
  ::get-config
  (fn []
    {:dispatch [::mqtt/request {:method "Config.Get"
                                :args   {:key "planter"}}]}))

(re-frame/reg-event-fx
  ::planter-gpio-set
  (fn [_ [_ pin value]]
    {:dispatch [::mqtt/request {:method "Planter.GPIOSwitch.Set"
                                :args   {:key   (str "/ctrl/gpio-switch/" pin)
                                         :value value}}]}))

(re-frame/reg-event-fx
  ::planter-schedule-get
  (fn []
    {:dispatch [::mqtt/request {:method "Planter.Schedule.Get"
                                :args   {}}]}))

(re-frame/reg-event-fx
  ::planter-get-stats
  (fn []
    {:dispatch [::mqtt/request {:method "Planter.GetStats"
                                :args   {}}]}))

(re-frame/reg-event-fx
  ::get-stats-and-schedule
  (fn []
    {:dispatch-n [[::planter-get-stats]
                  [::planter-schedule-get]]}))