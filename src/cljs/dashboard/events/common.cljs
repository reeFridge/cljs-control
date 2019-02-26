(ns dashboard.events.common
  (:require
    [dashboard.events.rpc :as rpc]
    [re-frame.core :as re-frame]
    [dashboard.db :as db]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-fx
  ::toggle-switch
  (fn [{:keys [db]} [_ id]]
    (let [func (get (:funcs db) id)
          value (if (not (:state func)) 1 0)
          pin (:pin func)]
      {:db       (assoc db :funcs (update-in (:funcs db) [id :state] not))
       :dispatch [::rpc/planter-gpio-set pin value]
       })))

(re-frame/reg-event-db
  ::toggle-event-switch
  (fn [db [_ id]]
    (assoc db :events (update-in (:events db) [id :state] not))))

(re-frame/reg-event-db
  ::set-active-func
  (fn [db [_ id]]
    (assoc db :active-func id)))

(re-frame/reg-event-db
  ::set-active-event
  (fn [db [_ id]]
    (assoc db :active-event id)))

(re-frame/reg-event-db
  ::set-active-device
  (fn [db [_ id]]
    (assoc db :active-device id)))

(re-frame/reg-event-db
  ::set-schedule
  (fn [db [_ schedule]]
    (assoc db :schedule schedule)))

(re-frame/reg-event-db
  ::set-active-panel
  (fn [db [_ value data]]
    (assoc (assoc db :active-panel value) :panel-data data)))

(re-frame/reg-event-fx
  ::reset-device
  (fn [{:keys [db]}]
    {:db         (assoc db :funcs nil)
     :dispatch-n (list [::set-active-device nil]
                       [::set-schedule nil]
                       [::set-active-func nil])}))

(re-frame/reg-event-db
  ::set-grow-mode
  (fn [db [_ mode]]
    (assoc db :grow-mode mode)))

(re-frame/reg-event-db
  ::reset-user
  (fn [db _]
    (assoc db :user nil)))
