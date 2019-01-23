(ns dashboard.views
  (:require
    [re-frame.core :as re-frame]
    [dashboard.subs :as subs]
    [dashboard.events :as events]
    [cljs-time.core :as time]
    [cljs-time.format :as format]
    [reagent.core :as r]
    [clojure.string :refer [join]]))

(defn switch [state click-handler]
  [:div.checkbox
   {:class    (if state "on" "off")
    :on-click click-handler}
   [:div.text (if state "вкл" "выкл")]])

(defn func-item []
  (fn [{:keys [name state id class]}]
    [:li
     [:div.func.item
      {:on-click (fn []
                   (re-frame/dispatch-sync [::events/set-active-func id])
                   (re-frame/dispatch [::events/set-active-panel :func-control]))}
      [:div.icon.left {:class class}] [:div.title name] [:div.icon.right.control] [switch state]]]))

(defn func-list []
  (let [funcs @(re-frame/subscribe [::subs/funcs])]
    [:div
     [:ul
      (for [func (vals funcs)]
        ^{:key (:id func)} [func-item func])]]))

(defn menu [cb]
  [:div.menu-container {:on-click cb}
   [:div] [:div] [:div]])

(defn device-overview-panel []
  (let [device-id (re-frame/subscribe [::subs/active-device])
        device-name (re-frame/subscribe [::subs/device-name])
        funcs (re-frame/subscribe [::subs/funcs])]
    [:div.main-container
     [:div.header [menu (fn []
                          (re-frame/dispatch [::events/reset-device])
                          (re-frame/dispatch [::events/set-active-panel :devices]))] @device-name]
     (when (seq @funcs) [func-list])
     [:div.footer
      [:div.button.red {:on-click (fn []
                                    (re-frame/dispatch [::events/remove-device @device-id])
                                    (re-frame/dispatch [::events/set-active-panel :devices]))} "Удалить устройство"]]]))

(defn timer-item []
  (fn [{:keys [name state id time action type weekset]}]
    [:li
     [:div.timer.item
      {:on-click (fn []
                   (re-frame/dispatch-sync [::events/set-active-event id])
                   (re-frame/dispatch [::events/set-active-panel :event]))}
      [:div.title name]
      [:div.timer-info-container
       [:div.time (time/hour time) ":" (time/minute time) (if (= type :cycle) (join "| " [" " weekset]))]
       [:div.action "Действие: " (condp = action
                                   :on "включить"
                                   :off "выключить")]]
      [switch state]]]))

(defn timer-list [timers]
  [:div
   [:ul
    (for [timer timers]
      ^{:key (:id timer)} [timer-item timer])]])

(defn tabs []
  (let [active-tab (r/atom :timer)]
    (fn [contents]
      [:div.tabs-container
       [:div.tab
        {:on-click #(reset! active-tab :timer)
         :class    (when (= :timer @active-tab) "active")}
        [:div.icon.timer_]
        [:div.label "Таймер"]]
       [:div.tab
        {:on-click #(reset! active-tab :cycle)
         :class    (when (= :cycle @active-tab) "active")}
        [:div.icon.cycle]
        [:div.label "Цикл"]]
       (get contents @active-tab)
       [:div.footer
        [:div.button {:on-click (fn []
                                  (re-frame/dispatch-sync [::events/set-active-event nil])
                                  (re-frame/dispatch [::events/set-active-panel :event]))} "+"]]])))

(defn func-control-panel []
  (fn [{:keys [name state id class]}]
    (let [events @(re-frame/subscribe [::subs/events id])]
      [:div.main-container
       [:div.header
        [:div.icon.left.arrow-left
         {:on-click #(re-frame/dispatch [::events/set-active-panel :device-overview])}]
        "Управление"]
       [:div.func.detail
        [:div.icon.left {:class class}] [:div.title name]
        [switch state #(re-frame/dispatch [::events/toggle-switch id])]]
       (let [timers (filter (fn [{:keys [type]}] (= type :timer)) events)
             cycles (filter (fn [{:keys [type]}] (= type :cycle)) events)]
         [tabs {:timer (when (seq timers) [timer-list timers])
                :cycle (when (seq cycles) [timer-list cycles])}])])))

(defn register-panel []
  [:div.main-container
   [:div.header
    [:div.icon.left.arrow-left
     {:on-click #(re-frame/dispatch [::events/set-active-panel :login])}]
    "Регистрация"]
   [:div.form
    [:input.name {:type "text" :placeholder "Имя"}]
    [:input.email {:type "text" :placeholder "Email"}]
    [:input.password {:type "password" :placeholder "Пароль"}]
    [:input.password {:type "password" :placeholder "Повторите пароль"}]]
   [:div.footer
    [:div.button "Отправить"]]])

(defn device-item []
  (fn [{:keys [name id]}]
    [:li
     [:div.device.item
      {:on-click (fn []
                   (re-frame/dispatch-sync [::events/set-active-device id])
                   (let [devices (re-frame/subscribe [::subs/devices])
                         active (re-frame/subscribe [::subs/active-device])]
                     (re-frame/dispatch-sync [::events/mqtt-connect (get @devices @active)]))
                   (re-frame/dispatch [::events/set-active-panel :device-overview]))}
      [:div.title name]]]))

(defn devices-list [devices]
  [:div
   [:ul
    (for [device devices]
      ^{:key (:id device)} [device-item device])]])

(defn devices-panel []
  (let [devices @(re-frame/subscribe [::subs/devices])]
    [:div.main-container
     [:div.header "Мои устройства"]
     [devices-list (vals devices)]
     [:div.footer
      [:div.button {:on-click #(re-frame/dispatch [::events/set-active-panel :add-device])}
       "+"]]]))

(defn event-panel []
  (fn [{:keys [type name state time id]}]
    [:div.main-container
     [:div.header
      [:div.icon.left.arrow-left
       {:on-click #(re-frame/dispatch [::events/set-active-panel :func-control])}]
      (if type "Изменение" "Добавление") " события"]
     (when type [:div.func.detail
                 [:div.icon.left {:class (condp = type
                                           :cycle "cycle"
                                           :timer "timer_")}]
                 [:div.title name]
                 [switch state #(re-frame/dispatch [::events/toggle-event-switch id])]])
     [:div.time-detail (join ":" (if type [(time/hour time) (time/minute time)] ["00" "00"]))]]))

(defn edit-device-panel []
  (let [name (r/atom "")
        desc (r/atom "")]
    (fn [id]
      [:div.main-container
       [:div.header
        [:div.icon.left.arrow-left
         {:on-click #(re-frame/dispatch [::events/set-active-panel :devices])}]
        (if id "Изменение" "Добавление") " устройства"]
       [:div.form
        [:input.name {:type "text" :placeholder "Наименование"
                      :value @name :on-change #(reset! name (-> % .-target .-value))}]
        [:input.name {:type "text" :placeholder "Описание"
                      :value @desc :on-change #(reset! desc (-> % .-target .-value))}]]
       [:div.footer
        [:div.button.green {:on-click (fn []
                                        (re-frame/dispatch [::events/add-device {:name @name
                                                                                 :description @desc}])
                                        (re-frame/dispatch [::events/set-active-panel :devices]))}
         (if id "Применить" "Добавить")]]])))

(defn login-panel []
  (let [email (r/atom "farm@ecolog.io")
        password (r/atom "LF8KMFJZysdvAFaa")]
    (fn []
      [:div.main-container
       [:div.header "Авторизация"]
       [:div.form
        [:input.email {:type  "text" :placeholder "Email"
                       :value @email :on-change #(reset! email (-> % .-target .-value))}]
        [:input.password {:type  "password" :placeholder "Пароль"
                          :value @password :on-change #(reset! password (-> % .-target .-value))}]]
       [:div.footer
        [:div.button {:on-click #(re-frame/dispatch [::events/request-token {:email @email :password @password}])}
         "Вход"]
        [:div.button.green {:on-click #(re-frame/dispatch [::events/set-active-panel :register])}
         "Регистрация"]]])))

(defn app []
  (let [active (re-frame/subscribe [::subs/active-panel])
        funcs (re-frame/subscribe [::subs/funcs])
        events (re-frame/subscribe [::subs/events-map])
        devices (re-frame/subscribe [::subs/devices])
        active-device (re-frame/subscribe [::subs/active-device])
        active-func (re-frame/subscribe [::subs/active-func])
        active-event (re-frame/subscribe [::subs/active-event])
        loading (re-frame/subscribe [::subs/loading])]
    (fn []
      [:div
       ;(when @loading [:div.wrapper.load-stub
       ;                [:div.content]
       ;                [:div.progress]])
       (condp = @active
         :login [login-panel]
         :register [register-panel]
         :devices (do
                    (re-frame/dispatch [::events/request-devices])
                    [devices-panel])
         :add-device [edit-device-panel]
         :device-overview [device-overview-panel (get @devices @active-device)]
         :func-control [func-control-panel (get @funcs @active-func)]
         :event [event-panel (get @events @active-event)])])))
