(ns dashboard.views
  (:require
   [re-frame.core :as re-frame]
   [dashboard.subs :as subs]
   [dashboard.events :as events]
   [cljs-time.core :as time]
   [cljs-time.format :as format]))

(defn switch [state click-handler]
  [:div.checkbox
   {:class (if state "on" "off")
    :on-click click-handler}
   [:div.text (if state "ON" "OFF")]])

(defn func-item []
  (fn [{:keys [name state id]}]
    [:li
     [:div.func.item
      {:on-click (fn []
                   (re-frame/dispatch [::events/set-active-func id])
                   (re-frame/dispatch [::events/set-active-panel :func-control]))}
      [:div.icon.left {:class name}] [:div.title name] [:div.icon.right.control] [switch state]]]))

(defn func-list []
  (let [funcs @(re-frame/subscribe [::subs/funcs])]
    [:div
     [:ul
      (for [func (vals funcs)]
        ^{:key (:id func)} [func-item func])]]))

(defn menu []
  [:div.menu-container
   [:div] [:div] [:div]])

(defn main-panel []
  (let [device-name (re-frame/subscribe [::subs/device-name])
        funcs (re-frame/subscribe [::subs/funcs])]
    [:div.main-container
     [:div.header [menu] @device-name]
     (when (seq @funcs) [func-list])]))

(defn timer-item []
  (fn [{:keys [name state id time action]}]
    [:li
     [:div.timer.item
      [:div.title name]
      [:div.timer-info-container
       [:div.time (time/hour time) ":" (time/minute time)]
       [:div.action "action: " action]]
      [switch state]]]))

(defn timer-list []
  (let [timers @(re-frame/subscribe [::subs/timers])]
    [:div
     [:ul
      (for [timer (vals timers)]
        ^{:key (:id timer)} [timer-item timer])]]))

(defn func-control-panel []
  (let [timers (re-frame/subscribe [::subs/timers])]
    (fn [{:keys [name state id]}]
      [:div.main-container
       [:div.header
        [:div.icon.left.arrow-left
         {:on-click #(re-frame/dispatch [::events/set-active-panel :main])}]
        "Control"]
       [:div.func.detail
        [:div.icon.left {:class name}] [:div.title name]
        [switch state #(re-frame/dispatch [::events/toggle-switch id])]]
       [:div.tabs-container
        [:div.tab
         [:div.icon.timer_]
         [:div.label "Timer"]]
        [:div.tab
         [:div.icon.cycle]
         [:div.label "Cycle"]]]
       (when (seq @timers) [timer-list])])))

(defn app []
  (let [active (re-frame/subscribe [::subs/active-panel])
        funcs (re-frame/subscribe [::subs/funcs])
        active-func (re-frame/subscribe [::subs/active-func])]
    (fn []
      (condp = @active
        :main [main-panel]
        :func-control [func-control-panel (get @funcs @active-func)]))))
