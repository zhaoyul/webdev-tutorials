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
(def marble-diagram-duration-ms 4200)

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn now-ms
  [start]
  (- (System/currentTimeMillis) start))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn tap-stage
  "在不改写值流的前提下记录某个阶段的值."
  [observable mark-value! stage]
  (rx/do #(mark-value! stage %) observable))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn rx-filter
  "为 `->` 管道准备的 filter 帮助函数: observable 放在第一个参数."
  [observable pred]
  (rx/filter pred observable))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(defn rx-map
  "为 `->` 管道准备的 map 帮助函数: observable 放在第一个参数."
  [observable xf]
  (rx/map xf observable))

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
        start (System/currentTimeMillis)
        stage-events {:source (atom [])
                      :filtered (atom [])
                      :mapped (atom [])}
        source-events (:source stage-events)
        filtered-events (:filtered stage-events)
        mapped-events (:mapped stage-events)
        track-atoms [source-events filtered-events mapped-events]
        output-values (atom [])
        done (promise)
        mark-value! (fn [stage value]
                      (swap! (stage-events stage) conj {:t (now-ms start)
                                                        :value value}))
        mark-complete! (fn [events]
                         (swap! events conj {:t (now-ms start)
                                             :kind :complete}))
        mark-error! (fn [events message]
                      (swap! events conj {:t (now-ms start)
                                          :kind :error
                                          :value message}))
        mark-all! (fn [f & args]
                    (doseq [events track-atoms]
                      (apply f events args)))]
    (rx/subscribe
     (-> (rx/seq->o input-values)
         (tap-stage mark-value! :source)
         (rx-filter keep-min-length-3?)
         (tap-stage mark-value! :filtered)
         (rx-map str/upper-case)
         (tap-stage mark-value! :mapped))
     #(swap! output-values conj %)
     (fn [e]
       (let [msg (.getMessage e)]
         (mark-error! mapped-events msg))
       (deliver done true))
     (fn []
       (mark-all! mark-complete!)
       (deliver done true)))
    (when-not (deref done collect-timeout-ms false)
      (mark-error! mapped-events "observable collection timeout"))
    (let [duration marble-diagram-duration-ms
          source-events (normalize-events @source-events duration)
          filtered-events (normalize-events @filtered-events duration)
          mapped-events (normalize-events @mapped-events duration)]
      {:values @output-values
       :duration duration
       :tracks [{:label "源序列 (rx/seq->o)"
                 :color "#6366f1"
                 :events source-events}
                {:label "filter #(>= (count %) 3)"
                 :color "#14b8a6"
                 :events filtered-events}
                {:label "map str/upper-case"
                 :color "#10b981"
                 :events mapped-events}]})))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def rxclojure-example-values
  (:values rx-execution))

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
;; (-> (rx/seq->o ["c" "cl" "clj" "cloj" "clojure"])
;;     (tap-stage mark-value! :source)
;;     (rx-filter #(>= (count %) 3))
;;     (tap-stage mark-value! :filtered)
;;     (rx-map str/upper-case)
;;     (tap-stage mark-value! :mapped))
;; ```
;;
;; `rxclojure-example-values` 的结果应为 `["CLJ" "CLOJ" "CLOJURE"]`.
