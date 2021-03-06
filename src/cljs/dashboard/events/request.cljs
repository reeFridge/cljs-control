(ns dashboard.events.request
  (:require
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [re-frame.core :as re-frame]
    [dashboard.db :refer [id-map]]))

(def endpoint "api.agrofarm.city:9090")
(defn url [path]
  (str "http://" endpoint path))

(re-frame/reg-event-fx
  ::token
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

(re-frame/reg-event-db
  ::token-success
  (fn [db [_ token]]
    (re-frame/dispatch [::user token])
    (assoc (assoc (assoc db :loading false) :token token) :active-panel :devices)))

(re-frame/reg-event-db
  ::token-fail
  (fn [db _]
    (assoc (assoc db :loading false) :active-panel :login)))

(re-frame/reg-event-fx
  ::register
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

(re-frame/reg-event-fx
  ::devices
  (fn [{:keys [db]} _]
    (let [token (:token db)]
      {:http-xhrio {:method          :get
                    :uri             (url (str "/api/v1/users/" (:user_id token) "/relationships/devices"))
                    :timeout         5000
                    :format          (ajax/text-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::devices-success]
                    :on-failure      [::devices-fail]}})))

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
  ::devices-fail
  (fn [db _]
    (assoc (assoc db :loading false) :active-panel :devices)))

(re-frame/reg-event-fx
  ::user
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
                    :on-success      [::remove-device-success id]
                    :on-failure      [::remove-device-fail]}})))

(re-frame/reg-event-db
  ::remove-device-success
  (fn [db [_ id]]
    (assoc db :devices (dissoc (:devices db) id))))

(re-frame/reg-event-db
  ::remove-device-fail
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
                    :on-success      [::add-device-success]
                    :on-failure      [::add-device-fail]}})))

(re-frame/reg-event-db
  ::add-device-success
  (fn [db _]
    (re-frame/dispatch [::devices])
    db))

(re-frame/reg-event-db
  ::add-device-fail
  (fn [db _]
    db))
