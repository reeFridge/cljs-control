(ns dashboard.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [dashboard.events.common :as events]
   [dashboard.views :as views]
   [dashboard.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
