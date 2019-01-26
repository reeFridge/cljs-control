(ns dashboard.db
  (:require
   [clojure.string :refer [join]]
   [cljs-time.core :as time]))

(defn switch [class-id id name state]
  {:id (join "#" ["switch" id])
   :pin id
   :class class-id
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

(defn cycle_ [name state func-id action time weekset]
  {:id (join "#" [func-id :cycle name])
   :state state
   :time time
   :name name
   :func-id func-id
   :action action
   :weekset weekset
   :delay nil
   :cycle nil
   :type :cycle})

(def default-db
  {:mqtt-connection nil
   :token nil
   :active-panel :login
   :active-func nil
   :active-event nil
   :active-device nil
   :loading false
   :devices {}
   :funcs nil
   :user nil
   :events (id-map [(timer "Полдень"
                           true
                           "switch#13"
                           :on
                           (time/today-at 12 30))
                    (timer "Дневной"
                           true
                           "switch#13"
                           :on
                           (time/today-at 16 30))
                    (timer "Вечер"
                           true
                           "switch#13"
                           :off
                           (time/today-at 20 10))
                    (timer "Вечер"
                           false
                           "switch#16"
                           :on
                           (time/today-at 21 45))
                    (timer "День"
                           true
                           "switch#16"
                           :off
                           (time/today-at 14 40))
                    (cycle_ "Цикл-1"
                           false
                           "switch#13"
                           :off
                           (time/today-at 16 30)
                           "Пн Ср Пт")
                    (cycle_ "Цикл-2"
                            true
                            "switch#13"
                            :on
                            (time/today-at 20 15)
                            "Вт Чт")
                    (cycle_ "Цикл-3"
                            true
                            "switch#16"
                            :off
                            (time/today-at 12 25)
                            "Пн - Пт")])})
