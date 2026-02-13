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
(def rxclojure-example-values
  (let [acc (atom [])]
    (rx/subscribe
     (->> (rx/seq->o ["c" "cl" "clj" "cloj" "clojure"])
          (rx/filter #(>= (count %) 3))
          (rx/map str/upper-case))
     #(swap! acc conj %))
    @acc))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(def rxclojure-marble
  {:title "RxClojure: seq->o → filter → map"
   :duration 4200
   :playback-rate 1.25
   :loop? false
   :width 780
   :tracks [{:label "源序列 (rx/seq->o)"
             :color "#6366f1"
             :events [{:t 200 :value "c"}
                      {:t 650 :value "cl"}
                      {:t 1050 :value "clj"}
                      {:t 2500 :value "cloj"}
                      {:t 3100 :value "clojure"}
                      {:t 4000 :kind :complete}]}
            {:label "filter #(>= (count %) 3)"
             :color "#14b8a6"
             :events [{:t 1400 :value "clj"}
                      {:t 2900 :value "cloj"}
                      {:t 3500 :value "clojure"}
                      {:t 4000 :kind :complete}]}
            {:label "map str/upper-case"
             :color "#10b981"
             :events [{:t 1700 :value "CLJ"}
                      {:t 3200 :value "CLOJ"}
                      {:t 3800 :value "CLOJURE"}
                      {:t 4100 :kind :complete}]}]})

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
