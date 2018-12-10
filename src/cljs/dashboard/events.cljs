(ns dashboard.events
  (:require
   [re-frame.core :as re-frame]
   [dashboard.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::toggle-switch
 (fn [db [_ id]]
   (assoc db :funcs (update-in (:funcs db) [id :state] not))))

(re-frame/reg-event-db
 ::set-active-func
 (fn [db [_ id]]
   (assoc db :active-func id)))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ value]]
   (assoc db :active-panel value)))
