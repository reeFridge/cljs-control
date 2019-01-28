(ns dashboard.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::device-name
 (fn [db]
   (:name (get (:devices db) (:active-device db)))))

(re-frame/reg-sub
 ::funcs
 (fn [db]
   (:funcs db)))

(re-frame/reg-sub
  ::devices
  (fn [db]
    (:devices db)))

(re-frame/reg-sub
  ::user
  (fn [db]
    (:user db)))

(re-frame/reg-sub
 ::events
 (fn [db [_ id]]
   (filter (fn [{:keys [func-id]}] (= func-id id)) (vals (:events db)))))

(re-frame/reg-sub
 ::events-map
 (fn [db]
   (:events db)))

(re-frame/reg-sub
 ::active-event
 (fn [db]
   (:active-event db)))

(re-frame/reg-sub
 ::active-func
 (fn [db _]
   (:active-func db)))

(re-frame/reg-sub
  ::active-device
  (fn [db _]
    (:active-device db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
  ::panel-data
  (fn [db _]
    (:panel-data db)))

(re-frame/reg-sub
  ::grow-mode
  (fn [db _]
    (:grow-mode db)))

(re-frame/reg-sub
  ::loading
  (fn [db _]
    (:loading db)))
