(ns dashboard.events
  (:require
    [re-frame.core :as re-frame]
    [dashboard.db :as db]
    [dashboard.mqtt :as mqtt]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [clojure.string :refer [join]]
    [dashboard.db :refer [id-map switch]]))

(def endpoint "api.agrofarm.city:9090")
(def pin-type {:light 13 :water 16})
(defn url [path]
  (str "http://" endpoint path))

(re-frame/reg-event-fx
  ::request-token
  (fn [{:keys [db]} [_ data]]
    {:db         (assoc db :loading true)
     :http-xhrio {:method          :post
                  :uri             (url "/token")
                  :body            (doto
                                     (js/FormData.)
                                     (.append "username" (:email data))
                                     (.append "password" (:password data))
                                     (.append "grant_type" "password"))
                  :timeout         5000
                  :format          (ajax/text-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::token-success]
                  :on-failure      [::token-fail]}}))

(re-frame/reg-event-fx
  ::request-register
  (fn [_ [_ data]]
    {:http-xhrio {:method          :post
                  :uri             (url "/api/v1/users")
                  :params          (clj->js {:data {:type          "users"
                                                    :id            nil
                                                    :attributes    {:email      (:email data)
                                                                    :first_name (:first-name data)
                                                                    :last_name  (:last-name data)
                                                                    :password   (:pass data)}
                                                    :relationships {:role {:data {:type "roles" :id "user"}}}}})
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::register-success]
                  :on-failure      [::register-fail]}}))

(re-frame/reg-event-db
  ::register-fail
  (fn [db _]
    db))

(re-frame/reg-event-db
  ::register-fail
  (fn [db _]
    db))

(re-frame/reg-event-fx
  ::request-devices
  (fn [{:keys [db]} _]
    (let [token (:token db)]
      {:http-xhrio {:method          :get
                    :uri             (url (str "/api/v1/users/" (:user_id token) "/relationships/devices"))
                    :timeout         5000
                    :format          (ajax/text-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::devices-success]
                    :on-failure      [::devices-fail]}})))

(re-frame/reg-event-fx
  ::request-user
  (fn [{:keys [db]} [_ token]]
    {:http-xhrio {:method          :get
                  :uri             (url (str "/api/v1/users/" (:user_id token)))
                  :headers         {"Authorization" (str "Bearer" " " (:access_token token))}
                  :timeout         5000
                  :format          (ajax/text-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::user-success]
                  :on-failure      [::user-fail]}}))

(re-frame/reg-event-db
  ::user-success
  (fn [db [_ response]]
    (assoc db :user {:id         (:id (:data response))
                     :email      (:email (:attributes (:data response)))
                     :first-name (:first_name (:attributes (:data response)))
                     :last-name  (:last_name (:attributes (:data response)))
                     :uuid       (:uuid (:attributes (:data response)))})))

(re-frame/reg-event-db
  ::user-fail
  (fn [db _]
    db))

(re-frame/reg-event-fx
  ::remove-device
  (fn [{:keys [db]} [_ id]]
    (let [token (:token db)]
      {:http-xhrio {:method          :delete
                    :uri             (url (str "/api/v1/devices/" id))
                    :timeout         5000
                    :headers         {"Authorization" (str "Bearer" " " (:access_token token))}
                    :format          (ajax/text-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::remove-success id]
                    :on-failure      [::remove-fail]}})))

(re-frame/reg-event-db
  ::remove-success
  (fn [db [_ id]]
    (assoc db :devices (dissoc (:devices db) id))))

(re-frame/reg-event-db
  ::remove-fail
  (fn [db _]
    db))

(re-frame/reg-event-fx
  ::add-device
  (fn [{:keys [db]} [_ data]]
    (let [token (:token db)]
      {:http-xhrio {:method          (if (:id data) :patch :post)
                    :uri             (url (str "/api/v1/devices" (when (:id data) (str "/" (:id data)))))
                    :timeout         5000
                    :headers         {"Authorization" (str "Bearer" " " (:access_token token))}
                    :params          (clj->js {:data {:type       "devices"
                                                      :id         (:id data)
                                                      :attributes {:name        (:name data)
                                                                   :description (:description data)}}})
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::add-success]
                    :on-failure      [::add-fail]}})))

(re-frame/reg-event-db
  ::add-success
  (fn [db _]
    (re-frame/dispatch [::request-devices])
    db))

(re-frame/reg-event-db
  ::add-fail
  (fn [db _]
    db))

(re-frame/reg-event-db
  ::devices-success
  (fn [db [_ {:keys [data]}]]
    (assoc (assoc db :loading false) :devices
                                     (id-map (map (fn [{:keys [id attributes]}]
                                                    {:id   id
                                                     :desc (:description attributes)
                                                     :name (:name attributes)
                                                     :pass (:pass attributes)})
                                                  data)))))

(re-frame/reg-event-db
  ::mqtt-connect
  (fn [db [_ device]]
    (assoc db :mqtt-connection (mqtt/connect device {:success           ::mqtt-established
                                                     :failure           ::mqtt-failed
                                                     :message-arrived   ::mqtt-arrived
                                                     :message-delivered ::mqtt-delivered
                                                     :lost              ::mqtt-lost}))))

(re-frame/reg-event-db
  ::mqtt-subscribe
  (fn [db [_ topic handle]]
    (let [connection (:mqtt-connection db)]
      (mqtt/subscribe connection topic handle))
    db))

(re-frame/reg-event-db
  ::mqtt-established
  (fn [db _]
    (let [device-id (:active-device db)]
      (re-frame/dispatch [::mqtt-subscribe (str device-id "/response/#") {:success ::mqtt-subscribed
                                                                          :failure ::mqtt-subscribe-failed}]))
    db))

(re-frame/reg-event-db
  ::mqtt-subscribed
  (fn [db _]
    (re-frame/dispatch [::planter-get-stats])
    db))

(re-frame/reg-event-db
  ::planter-get-config
  (fn [db _]
    (re-frame/dispatch [::mqtt-request {:method "Config.Get"
                                        :args   {:key "planter"}}])
    db))

(re-frame/reg-event-db
  ::planter-gpio-switch
  (fn [db [_ pin value]]
    (re-frame/dispatch [::mqtt-request {:method "Planter.GPIOSwitch.Set"
                                        :args   {:key   (str "/ctrl/gpio-switch/" pin)
                                                 :value value}}])
    db))

(re-frame/reg-event-db
  ::planter-get-stats
  (fn [db _]
    (re-frame/dispatch [::mqtt-request {:method "Planter.GetStats"
                                        :args   {}}])
    db))

(re-frame/reg-event-db
  ::mqtt-subscribe-failed
  (fn [db _]
    db))

(re-frame/reg-event-db
  ::mqtt-failed
  (fn [db _]
    db))

(re-frame/reg-event-db
  ::mqtt-arrived
  (fn [db [_ message]]
    (println "message-arrived: " message)
    (let [pin-value {:light (get (:result message) (keyword (str "ipin" (:light pin-type))))
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
        :else db))))

(re-frame/reg-event-db
  ::mqtt-delivered
  (fn [db [_ message]]
    (println "message-delivered: " message)
    db))

(re-frame/reg-event-db
  ::mqtt-lost
  (fn [db [_ response]]
    (println "MQTT lost" response)
    (assoc db :mqtt-connection nil)))

(re-frame/reg-event-db
  ::mqtt-request
  (fn [db [_ payload]]
    (let [device (get (:devices db) (:active-device db))
          connection (:mqtt-connection db)]
      (mqtt/request connection device payload))
    db))

(re-frame/reg-event-db
  ::devices-fail
  (fn [db _]
    (assoc (assoc db :loading false) :active-panel :devices)))

(re-frame/reg-event-db
  ::token-success
  (fn [db [_ token]]
    (re-frame/dispatch [::request-user token])
    (assoc (assoc (assoc db :loading false) :token token) :active-panel :devices)))

(re-frame/reg-event-db
  ::token-fail
  (fn [db _]
    (assoc (assoc db :loading false) :active-panel :login)))

(re-frame/reg-event-fx
  ::register-click
  (fn [_ [_ data]]
    {:dispatch-n (list [::request-register data]
                       [::set-active-panel :login])}))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  ::toggle-switch
  (fn [db [_ id]]
    (let [func (get (:funcs db) id)
          value (if (not (:state func)) 1 0)
          pin (:pin func)]
      (re-frame/dispatch [::planter-gpio-switch pin value])
      (assoc db :funcs (update-in (:funcs db) [id :state] not)))))

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
  ::set-active-panel
  (fn [db [_ value data]]
    (assoc (assoc db :active-panel value) :panel-data data)))

(re-frame/reg-event-fx
  ::reset-device
  (fn [{:keys [db]}]
    {:db         (assoc db :funcs nil)
     :dispatch-n (list [::set-active-device nil]
                       [::set-active-func nil])}))

(re-frame/reg-event-db
  ::set-grow-mode
  (fn [db [_ mode]]
    (assoc db :grow-mode mode)))

(re-frame/reg-event-db
  ::reset-user
  (fn [db _]
    (assoc db :user nil)))
