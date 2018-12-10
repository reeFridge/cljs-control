(ns dashboard.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::device-name
 (fn [db]
   (:device-name db)))

(re-frame/reg-sub
 ::funcs
 (fn [db]
   (:funcs db)))

(re-frame/reg-sub
 ::timers
 (fn [db]
   (:timers db)))

(re-frame/reg-sub
 ::active-func
 (fn [db _]
   (:active-func db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))
