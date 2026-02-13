^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns core_async.rxclojure_marble_intro
  (:require [clojure.string :as str]
            [core_async.reactive_marble_viewer :refer [marble]]
            [rx.lang.clojure.core :as rx]))

;; # RxClojure marble diagram 入门

;; 这个 notebook 基于 `reactive_marble_viewer.clj` 的 marble viewer, 用 RxClojure 表达同类响应式流程.
;; 目标是用 `RxClojure` 替代教程里原本面向 rx.net 的叙述方式, 保持操作符语义一致.

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def collect-timeout-ms 2000)

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def timeline-padding-ms 200)

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def visible-span-ratio 0.85)

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def fallback-max-time-ms 1)

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn collect-events
  "订阅 observable 并采集 on-next/on-complete 的时间点, 让 marble viewer 能看到真实执行过程."
  [observable]
  (let [start (System/currentTimeMillis)
        events (atom [])
        done (promise)]
    (rx/subscribe
     observable
     (fn [v]
       (swap! events conj {:t (- (System/currentTimeMillis) start)
                           :value v}))
     (fn [e]
       (swap! events conj {:t (- (System/currentTimeMillis) start)
                           :kind :error
                           :value (.getMessage e)})
       (deliver done true))
     (fn []
       (swap! events conj {:t (- (System/currentTimeMillis) start)
                           :kind :complete})
       (deliver done true)))
    (when-not (deref done collect-timeout-ms false)
      (swap! events conj {:t collect-timeout-ms
                          :kind :error
                          :value "observable collection timeout"}))
    @events))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn normalize-events
  "把真实执行时间缩放到 viewer 时间轴, 保持轨道可读."
  [events duration]
  (if (seq events)
    (let [timings (keep :t events)
          max-t (if (seq timings) (apply max timings) fallback-max-time-ms)
          span (* duration visible-span-ratio)]
      (mapv (fn [event]
              (update event :t
                      (fn [t]
                        (+ timeline-padding-ms (int (* (/ t max-t) span))))))
            events))
    []))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def rx-execution
  (let [input-values ["c" "cl" "clj" "cloj" "clojure"]
        keep-min-length-3? #(>= (count %) 3)
        source-o (fn []
                   (->> (rx/seq->o input-values)
                        (rx/do (fn [_] (Thread/sleep 80)))))
        filtered-o (fn []
                     (->> (source-o)
                          (rx/filter keep-min-length-3?)
                          (rx/do (fn [_] (Thread/sleep 90)))))
        mapped-o (fn []
                   (->> (source-o)
                        (rx/filter keep-min-length-3?)
                        (rx/map str/upper-case)
                        (rx/do (fn [_] (Thread/sleep 100)))))
        duration 4200
        source-events (normalize-events (collect-events (source-o)) duration)
        filtered-events (normalize-events (collect-events (filtered-o)) duration)
        mapped-events (normalize-events (collect-events (mapped-o)) duration)]
    {:duration duration
     :tracks [{:label "源序列 (rx/seq->o)"
               :color "#6366f1"
               :events source-events}
              {:label "filter #(>= (count %) 3)"
               :color "#14b8a6"
               :events filtered-events}
              {:label "map str/upper-case"
               :color "#10b981"
               :events mapped-events}]}))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def rxclojure-example-values
  (->> (:tracks rx-execution)
       (filter #(= "map str/upper-case" (:label %)))
       first
       :events
       (filter #(not= :complete (:kind %)))
       (mapv :value)))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def rxclojure-marble
  {:title "RxClojure: seq->o → filter → map"
   :duration (:duration rx-execution)
   :playback-rate 1.25
   :loop? false
   :width 780
   :tracks (:tracks rx-execution)})

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(marble rxclojure-marble)

;; ## 对照代码
;;
;; ```clojure
;; (->> (rx/seq->o ["c" "cl" "clj" "cloj" "clojure"])
;;      (rx/filter #(>= (count %) 3))
;;      (rx/map str/upper-case))
;; ```
;;
;; `rxclojure-example-values` 的结果应为 `["CLJ" "CLOJ" "CLOJURE"]`.
