(ns dashboard.views
  (:require
    [re-frame.core :as re-frame]
    [dashboard.subs :as subs]
    [dashboard.events :as events]
    [cljs-time.core :as time]
    [cljs-time.format :as format]
    [reagent.core :as r]
    [clojure.string :refer [join]]
    [re-com.core :refer [v-box box h-box input-text input-password modal-panel]]
    [re-com.buttons :refer [button md-icon-button row-button]]
    [re-com.text :refer [title label]]
    [re-com.box :refer [line border]]
    [goog.object :as g]))

(defn header-action [icon on-click]
  [box :padding "10px"
   :child [md-icon-button :md-icon-name icon :size :larger :style {:color "white"}
           :on-click on-click]])

(defn header [items]
  (let [empty-item (fn [] [box :width "52px" :height "52px" :child ""])]
    [box :style {:background-color "rgba(90, 200, 245, 0.95)"}
     :child [h-box :children (vec (map (fn [item] (cond
                                                    (= item nil) (empty-item)
                                                    (string? item) [box :justify :center :size "1" :padding "5px"
                                                                    :child [title :label item :style {:font-size "20px"
                                                                                                      :color     "white"}]]
                                                    :else item)) items))]]))

(defn switch [state click-handler]
  (let [attr {:type "checkbox" :on-change click-handler}]
    [box :child [:label.toggle
                 [:input.toggle-checkbox (assoc attr :checked state)]
                 [:div.toggle-switch]]]))

(defn icon [class]
  [box :padding "10px" :style {:align-self "center"}
   :child [:div.icon {:class class}]])

(defn func-item []
  (fn [{:keys [name state id class]}]
    [border
     :border "none"
     :b-border "1px solid lightgrey"
     :child [h-box
             :children [(icon (str (when (not state) "disabled") " " (condp = class
                                                                       :light "light"
                                                                       :water "aeration")))
                        [box :size "1" :style {:align-self "center"}
                         :child [v-box :padding "10px"
                                 :children [[title :level :level3
                                             :label name]
                                            [title :level :level4 :margin-top "" :style {:font-weight "10"}
                                             :label (str "Состояние: " (if state "вкл" "выкл"))]]]]
                        [box :padding "10px"
                         :child [h-box
                                 :children [[row-button :md-icon-name "zmdi-settings" :mouse-over-row? true
                                             :on-click (fn []
                                                         (re-frame/dispatch-sync [::events/set-active-func id])
                                                         (re-frame/dispatch [::events/set-active-panel :func-control]))]]]]]]]))

(defn func-list []
  (let [funcs @(re-frame/subscribe [::subs/funcs])]
    [v-box
     :children (for [func (vals funcs)]
                 ^{:key (:id func)} [func-item func])]))

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

;(defn func-control-panel []
;  (fn [{:keys [name state id class]}]
;    (let [events @(re-frame/subscribe [::subs/events id])]
;      [:div.main-container
;       [:div.header
;        [:div.icon.left.arrow-left
;         {:on-click #(re-frame/dispatch [::events/set-active-panel :device-overview])}]
;        "Управление"]
;       [:div.func.detail
;        [:div.icon.left {:class class}] [:div.title name]
;        [switch state #(re-frame/dispatch [::events/toggle-switch id])]]
;       (let [timers (filter (fn [{:keys [type]}] (= type :timer)) events)
;             cycles (filter (fn [{:keys [type]}] (= type :cycle)) events)]
;         [tabs {:timer (when (seq timers) [timer-list timers])
;                :cycle (when (seq cycles) [timer-list cycles])}])])))

(defn func-control-panel []
  (fn [{:keys [name state id class]}]
    [v-box :min-height "100vh"
     :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :device-overview]))
                         "Управление"
                         (header-action "zmdi-refresh" #(re-frame/dispatch [::events/planter-get-stats]))])
                [box :size "1"
                 :child [v-box
                         :children [[box :padding "10px" :style {:background "rgba(0, 0, 0, 0.1)"}
                                     :child [h-box
                                             :children [(icon (str (when (not state) "disabled") " " (condp = class
                                                                                                       :light "light"
                                                                                                       :water "aeration")))
                                                        [box :size "1" :style {:align-self "center"}
                                                         :child [v-box :children [[title :level :level3 :margin-top "0.3em"
                                                                                   :label name]
                                                                                  [title :level :level4 :margin-top "" :style {:font-weight "10"}
                                                                                   :label (str "Состояние: " (if state "вкл" "выкл"))]]]]
                                                        [box :padding "10px"
                                                         :child [h-box :style {:font-size "35px"}
                                                                 :children [[switch state #(re-frame/dispatch [::events/toggle-switch id])]]]]]]]]]]
                [box :padding "10px"
                 :child [v-box :gap "10px"
                         :children [[button :label "Создать событие" :class "btn-block"
                                     :on-click (fn [])]]]]]]))

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

(defn device-item []
  (fn [{:keys [name id desc]}]
    [border :border "none" :b-border "1px solid lightgrey"
     :child [h-box
             :children [[v-box :justify :center :size "1" :padding "10px"
                         :children [[title :label name :level :level3]
                                    [title :level :level4 :margin-top "" :style {:font-weight "10"}
                                     :label desc]]]
                        [box :padding "10px"
                         :child [h-box
                                 :children [[row-button :md-icon-name "zmdi-edit" :mouse-over-row? true
                                             :on-click (fn []
                                                         (re-frame/dispatch-sync [::events/set-active-device id])
                                                         (re-frame/dispatch [::events/set-active-panel :edit-device]))]
                                            [row-button :md-icon-name "zmdi-settings" :mouse-over-row? true
                                             :on-click (fn []
                                                         (re-frame/dispatch-sync [::events/set-active-device id])
                                                         (let [devices (re-frame/subscribe [::subs/devices])
                                                               active (re-frame/subscribe [::subs/active-device])]
                                                           (re-frame/dispatch-sync [::events/mqtt-connect (get @devices @active)]))
                                                         (re-frame/dispatch [::events/set-active-panel :device-overview]))]]]]]]]))

(defn devices-list [devices]
  [v-box
   :children
   (for [device devices]
     ^{:key (:id device)} [device-item device])])

(defn devices-panel []
  (re-frame/dispatch [::events/reset-device])
  (let [devices (re-frame/subscribe [::subs/devices])
        user (re-frame/subscribe [::subs/user])]
    [v-box :min-height "100vh"
     :children [(header [(header-action "zmdi-square-right" (fn []
                                                              (re-frame/dispatch [::events/reset-user])
                                                              (re-frame/dispatch [::events/set-active-panel :login])))
                         "Мои устройства"
                         (header-action "zmdi-refresh" #(re-frame/dispatch [::events/request-devices]))])
                [box :size "1"
                 :child [v-box
                         :children [[box :padding "10px" :style {:background "rgba(0, 0, 0, 0.1)"}
                                     :child [v-box
                                             :children [[title :level :level2 :label (str (:first-name @user) " " (:last-name @user)) :style {:align-self "center"}]
                                                        [title :level :level4 :label (:email @user) :style {:align-self "center"}]]]]
                                    [devices-list (vals @devices)]]]]
                [box :padding "10px"
                 :child [v-box :gap "10px"
                         :children [[button :label "Добавить" :class "btn-block"
                                     :on-click #(re-frame/dispatch [::events/set-active-panel :add-device])]]]]]]))

(defn edit-device-panel [device]
  (let [name (r/atom (if (not (= nil device)) (:name device) ""))
        desc (r/atom (if (not (= nil device)) (:desc device) ""))]
    (fn [{:keys [id]}]
      [v-box :min-height "100vh"
       :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :devices]))
                           (str (if id "Изменение" "Добавление") " устройства")
                           nil])
                  [box :size "1"
                   :child [v-box
                           :children [[box :padding "10px" :style {:background "rgba(0, 0, 0, 0.1)"}
                                       :child [v-box
                                               :children [[title :level :level2 :label (if (= "" @name) "Наименование" @name) :style {:align-self "center"}]
                                                          [title :level :level3 :label (if (= "" @desc) "Описание" @desc) :style {:align-self "center"}]
                                                          [title :level :level4 :label id :style {:align-self "center"}]]]]
                                      [box :padding "10px"
                                       :child [v-box
                                               :gap "10px"
                                               :children [[input-text :width "100%"
                                                           :model name
                                                           :placeholder "Наименование"
                                                           :on-change #(reset! name %)
                                                           :change-on-blur? false]
                                                          [input-text :width "100%"
                                                           :model desc
                                                           :placeholder "Описание"
                                                           :on-change #(reset! desc %)
                                                           :change-on-blur? false]]]]]]]
                  [box :padding "10px"
                   :child [v-box :gap "10px"
                           :children [[button :label (if id "Применить" "Добавить") :class "btn-block"
                                       :on-click (fn []
                                                   (re-frame/dispatch [::events/add-device {:id          id
                                                                                            :name        @name
                                                                                            :description @desc}])
                                                   (re-frame/dispatch [::events/set-active-panel :devices]))]]]]]])))

(defn grow-mode-panel []
  (let [grow-mode (re-frame/subscribe [::subs/grow-mode])
        active-mode-part (r/atom :plant)
        variants (r/atom [])
        show-popup? (r/atom false)
        render-select (fn [key variants]
                        [modal-panel
                         :backdrop-on-click #(reset! show-popup? false)
                         :child [box
                                 :child [v-box :width "300px"
                                         :children (vec (map
                                                          (fn [name] [border :padding "10px" :border "none" :b-border "1px solid lightgrey"
                                                                      :child [title :label name :level :level2
                                                                              :style {:align-self "center"}
                                                                              :attr {:on-click (fn []
                                                                                                 (reset! show-popup? false)
                                                                                                 (re-frame/dispatch [::events/set-grow-mode (assoc @grow-mode key name)]))}]]) variants))]]])]
    (fn []
      (let [plant (:plant @grow-mode)
            stage (:stage @grow-mode)]
        [v-box :min-height "100vh"
         :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :device-overview]))
                             "Режим выращивания"
                             nil])
                    (when @show-popup? (render-select @active-mode-part @variants))
                    [box
                     :child [v-box
                             :children [[border :padding "15px" :border "none" :b-border "1px solid lightgrey"
                                         :child [h-box
                                                 :attr {:on-click (fn []
                                                                    (reset! active-mode-part :plant)
                                                                    (reset! variants ["Зелень" "Ягода" "Овощи" "Фрукты"])
                                                                    (reset! show-popup? true))}
                                                 :children [[box :size "1"
                                                             :child [title :level :level2 :label "Растение"]]
                                                            [box :style {:align-self "center"}
                                                             :child [title :level :level3 :label plant]]]]]
                                        [border :padding "15px" :border "none" :b-border "1px solid lightgrey"
                                         :child [h-box
                                                 :attr {:on-click (fn []
                                                                    (reset! active-mode-part :stage)
                                                                    (reset! variants ["Вегетация" "Плодоношение" "Авто"])
                                                                    (reset! show-popup? true))}
                                                 :children [[box :size "1"
                                                             :child [title :level :level2 :label "Стадия"]]
                                                            [box :style {:align-self "center"}
                                                             :child [title :level :level3 :label stage]]]]]]]]]]))))

(defn device-overview-panel []
  (let [grow-mode (re-frame/subscribe [::subs/grow-mode])
        devices (re-frame/subscribe [::subs/devices])
        device-id (re-frame/subscribe [::subs/active-device])
        name (:name (get @devices @device-id))
        desc (:desc (get @devices @device-id))
        id (:id (get @devices @device-id))
        funcs (re-frame/subscribe [::subs/funcs])]
    [v-box :min-height "100vh"
     :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :devices]))
                         "Управление"
                         (header-action "zmdi-refresh" #(re-frame/dispatch [::events/planter-get-stats]))])
                [box :size "1"
                 :child [v-box
                         :children [[box :padding "10px" :style {:background "rgba(0, 0, 0, 0.1)"}
                                     :child [v-box
                                             :children [[title :level :level2 :label name :style {:align-self "center"}]
                                                        [title :level :level3 :label desc :style {:align-self "center"}]
                                                        [title :level :level4 :label id :style {:align-self "center"}]]]]
                                    [border :border "none" :b-border "1px solid lightgrey"
                                     :child [h-box
                                             :children [(icon "grow")
                                                        [box :size "1" :style {:align-self "center"}
                                                         :child [v-box :padding "10px"
                                                                 :children [[title :level :level3
                                                                             :label "Режим выращивания"]
                                                                            [title :level :level4 :margin-top "" :style {:font-weight "10"}
                                                                             :label (str (:plant @grow-mode) " > " (:stage @grow-mode))]]]]
                                                        [box :padding "10px"
                                                         :child [row-button :md-icon-name "zmdi-tune" :mouse-over-row? true
                                                                 :on-click #(re-frame/dispatch [::events/set-active-panel :grow-mode])]]]]]
                                    (when (seq @funcs) [func-list])]]]
                [box :padding "10px"
                 :child [v-box :gap "10px"
                         :children [[button :label "Удалить устройство" :class "btn-block btn-danger"
                                     :on-click (fn []
                                                 (re-frame/dispatch [::events/remove-device @device-id])
                                                 (re-frame/dispatch [::events/set-active-panel :devices]))]]]]]]))

(defn captcha-panel [data]
  (let [script-id "recaptcha-script"]
    (r/create-class
      {:display-name           "captcha-panel"
       :component-did-mount    (fn []
                                 (let [script-tag (.createElement js/document "script")]
                                   (.setAttribute script-tag "src" "https://www.google.com/recaptcha/api.js?onload=captchaLoaded&render=explicit")
                                   (.setAttribute script-tag "id" script-id)
                                   (g/set js/window "captchaLoaded" (fn []
                                                                      (let [captcha (g/get js/window "grecaptcha")]
                                                                        (.render captcha "recaptcha-container" (clj->js {:sitekey "6LdUFlEUAAAAADQCy19MC9ZizHMfpNV-F9aFJI2v"
                                                                                                                         :theme   "light"
                                                                                                                         :hl      "ru"})))))
                                   (.appendChild (.-head js/document) script-tag)))
       :component-will-unmount (fn [] (let [script-tag (.getElementById js/document script-id)]
                                        (.removeChild (.-head js/document) script-tag)))
       :reagent-render
                               (fn []
                                 [v-box :min-height "100vh"
                                  :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :register]))
                                                      "Регистрация"
                                                      nil])
                                             [box :padding "10px" :justify :center :size "1"
                                              :child [v-box
                                                      :gap "10px"
                                                      :children [[box
                                                                  :child [:div {:id "recaptcha-container" :style {:margin "0 auto"}}]]]]]
                                             [box :padding "10px"
                                              :child [v-box :gap "10px"
                                                      :children [[button :label "Отправить" :class "btn-block"
                                                                  :on-click #(re-frame/dispatch [::events/register-click data])]]]]]])})))

(defn register-panel []
  (let [first-name (r/atom "")
        last-name (r/atom "")
        email (r/atom "")
        pass (r/atom "")
        pass-again (r/atom "")]
    (fn []
      [v-box :min-height "100vh"
       :children [(header [(header-action "zmdi-chevron-left" #(re-frame/dispatch [::events/set-active-panel :login]))
                           "Регистрация"
                           nil])
                  [box :padding "10px" :justify :center :size "1"
                   :child [v-box
                           :gap "10px"
                           :children [[box :style {:align-self "center"}
                                       :child [:div.logo]]
                                      [input-text :width "100%"
                                       :model first-name
                                       :placeholder "Имя"
                                       :on-change #(reset! first-name %)]
                                      [input-text :width "100%"
                                       :model last-name
                                       :placeholder "Фамилия"
                                       :on-change #(reset! last-name %)]
                                      [input-text :width "100%"
                                       :model email
                                       :placeholder "Email"
                                       :on-change #(reset! email %)]
                                      [input-password :width "100%"
                                       :model pass
                                       :placeholder "Пароль"
                                       :on-change #(reset! pass %)]
                                      [input-password :width "100%"
                                       :model pass-again
                                       :placeholder "Повторите пароль"
                                       :on-change #(reset! pass-again %)]]]]
                  [box :padding "10px"
                   :child [v-box :gap "10px"
                           :children [[button :label "Продолжить" :class "btn-block"
                                       :on-click #(re-frame/dispatch [::events/set-active-panel :captcha {:first-name @first-name
                                                                                                          :last-name  @last-name
                                                                                                          :email      @email
                                                                                                          :pass       @pass}])]]]]]])))

(defn login-panel []
  (let [email (r/atom "farm@ecolog.io")
        password (r/atom "LF8KMFJZysdvAFaa")]
    (fn []
      [v-box :min-height "100vh"
       :children [(header [nil "Авторизация" nil])
                  [box :padding "10px" :justify :center :size "1"
                   :child [v-box :gap "10px" :children [[box :style {:align-self "center"}
                                                         :child [:div.logo]]
                                                        [input-text :width "100%"
                                                         :model email
                                                         :placeholder "Email"
                                                         :on-change #(reset! email %)]
                                                        [input-password :width "100%"
                                                         :model password
                                                         :placeholder "Пароль"
                                                         :on-change #(reset! password %)]]]]
                  [box :padding "10px"
                   :child [v-box :gap "10px"
                           :children [[button :label "Вход" :class "btn-block btn-info"
                                       :on-click #(re-frame/dispatch [::events/request-token {:email @email :password @password}])]
                                      [button :label "Регистрация" :class "btn-block"
                                       :on-click #(re-frame/dispatch [::events/set-active-panel :register])]]]]]])))

(defn app []
  (let [active (re-frame/subscribe [::subs/active-panel])
        funcs (re-frame/subscribe [::subs/funcs])
        events (re-frame/subscribe [::subs/events-map])
        devices (re-frame/subscribe [::subs/devices])
        active-device (re-frame/subscribe [::subs/active-device])
        active-func (re-frame/subscribe [::subs/active-func])
        active-event (re-frame/subscribe [::subs/active-event])
        panel-data (re-frame/subscribe [::subs/panel-data])
        loading (re-frame/subscribe [::subs/loading])]
    (fn []
      ;(when @loading [:div.wrapper.load-stub
      ;                [:div.content]
      ;                [:div.progress]])
      (condp = @active
        :login [login-panel]
        :register [register-panel]
        :captcha [captcha-panel @panel-data]
        :devices (do
                   (re-frame/dispatch [::events/request-devices])
                   [devices-panel])
        :add-device [edit-device-panel]
        :edit-device [edit-device-panel (get @devices @active-device)]
        :device-overview [device-overview-panel (get @devices @active-device)]
        :func-control [func-control-panel (get @funcs @active-func)]
        :grow-mode [grow-mode-panel]
        :event [event-panel (get @events @active-event)]))))
