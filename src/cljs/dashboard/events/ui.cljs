(ns dashboard.events.ui
  (:require
    [dashboard.events.common :as common]
    [dashboard.events.request :as request]
    [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  ::register-click
  (fn [_ [_ data]]
    {:dispatch-n (list [::request/register data]
                       [::common/set-active-panel :login])}))
