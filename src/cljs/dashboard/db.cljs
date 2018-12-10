(ns dashboard.db
  (:require
   [clojure.string :refer [join]]
   [cljs-time.core :as time]))

(defn switch [name state]
  {:id (join "#" ["switch" name])
   :type "switch"
   :name name
   :state state})

(defn id-map [funcs]
  (reduce
   (fn [acc curr]
     (assoc acc (:id curr) curr))
   {} funcs))

(defn timer [name state func-id action time]
  {:id (join "#" [func-id :timer name])
   :state state
   :time time
   :name name
   :func-id func-id
   :action action
   :delay nil
   :cycle nil
   :type :timer})

(defn cycle_ [name state func-id action time]
  {:id (join "#" [func-id :cycle name])
   :state state
   :time time
   :name name
   :func-id func-id
   :action action
   :delay nil
   :cycle nil
   :type :cycle})

(def default-db
  {:active-panel :main
   :active-func nil
   :device-name "homepot#0"
   :funcs (id-map [(switch "light" true)
                   (switch "water" false)])
   :timers (id-map [(timer "timer1"
                           true
                           "switch#light"
                           :on
                           (time/today-at 12 30))
                    (timer "timer2"
                           true
                           "switch#water"
                           :off
                           (time/today-at 14 0))
                    (cycle_ "cycle1"
                           false
                           "switch#light"
                           :off
                           (time/today-at 16 30))])})
