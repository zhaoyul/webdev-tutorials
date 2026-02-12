^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns core_async.reactive_marble_viewer
  (:require [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# Reactive marble diagram viewer\n\n这个 notebook 定义了一个 marble diagram viewer, 用来可视化 reactive 过程中的事件流与通信节奏.\n\n- 事件流以“轨道”排列, 每条轨道代表一个 stream.\n- 播放线会自动循环, 用动画展示事件的出现时刻.\n- 示例参考 [ReactiveX/RxClojure](https://github.com/ReactiveX/RxClojure) 的语义, 但数据完全可自定义.")

^{::clerk/visibility {:code :show :result :hide}}
(def marble-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:keys [title duration tracks]}]
                 (let [duration (or duration 4000)
                       tracks (or tracks [])
                       width 720
                       row-height 56
                       active-threshold 0.03]
                   (reagent.core/with-let [progress-atom (reagent.core/atom 0)
                                           start-time (js/Date.now)
                                           frame-id-atom (atom nil)
                                           tick (fn tick []
                                                  (let [now (js/Date.now)
                                                        elapsed (- now start-time)
                                                        wrapped-elapsed (mod elapsed duration)
                                                        progress (/ wrapped-elapsed duration)]
                                                    (reset! progress-atom progress))
                                                  (reset! frame-id-atom (js/requestAnimationFrame tick)))]
                     (when-not @frame-id-atom
                       (reset! frame-id-atom (js/requestAnimationFrame tick)))
                     (let [progress @progress-atom
                           playhead-x (* width progress)
                           near-playhead? (fn [t]
                                            (let [p (/ t duration)
                                                  delta (js/Math.abs (- progress p))
                                                  ;; 循环时间轴下取首尾最短距离, 避免播放线跨越边界跳变
                                                  circular-delta (if (> delta 0.5) (- 1 delta) delta)]
                                              (< circular-delta active-threshold)))
                           ->x (fn [t] (* width (/ t duration)))]
                       [:div {:class "rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                        [:div {:class "flex items-center justify-between"}
                         [:div {:class "text-lg font-semibold text-slate-800"}
                          (or title "Reactive marble diagram")]
                         [:div {:class "text-xs text-slate-500"} (str "循环时长 " duration " ms")]]
                        [:div {:class "mt-4 space-y-3 overflow-x-auto"}
                         (for [{:keys [label events color] :or {color "#6366f1"}} tracks
                               :let [track-color color]]
                           ^{:key label}
                           [:div {:class "flex items-center gap-3" :style {:height row-height}}
                            [:div {:class "w-28 text-sm font-medium text-slate-600"} label]
                            [:div {:class "relative" :style {:width (str width "px")}}
                             [:div {:class "absolute left-0 right-0 top-1/2 h-px bg-slate-200"}]
                             [:div {:class "absolute top-0 bottom-0 w-0.5 bg-indigo-400/70"
                                    :style {:left (str playhead-x "px")}}]
                             (for [[idx {:keys [t value kind color]}] (map-indexed vector events)]
                               (let [kind (or kind :next)
                                     x (->x t)
                                     event-color (or color track-color "#6366f1")
                                     active? (near-playhead? t)
                                     title (case kind
                                             :complete "完成"
                                             :error "错误"
                                             (str value " @" t "ms"))]
                                 (case kind
                                   :complete
                                   ^{:key (str label "-complete-" idx "-" t)}
                                   [:div {:class "absolute top-1/2 h-4 w-0.5 rounded-full bg-slate-500"
                                          :style {:left (str x "px")
                                                  :transform "translate(-50%, -50%)"}
                                          :title title}]
                                   :error
                                   ^{:key (str label "-error-" idx "-" t)}
                                   [:div {:class "absolute top-1/2 text-xs font-bold text-rose-500"
                                          :style {:left (str x "px")
                                                  :transform "translate(-50%, -50%)"}
                                          :title title}
                                    "×"]
                                   ^{:key (str label "-" idx "-" t "-" value)}
                                   [:div {:class (str "absolute top-1/2 flex h-6 w-6 items-center justify-center rounded-full border text-xs font-semibold text-white shadow-sm transition "
                                                      (if active?
                                                        "scale-110 border-white/80"
                                                        "border-white/40"))
                                          :style {:left (str x "px")
                                                  :transform "translate(-50%, -50%)"
                                                  :background-color event-color}
                                          :title title}
                                    value]))))])]
                        [:div {:class "mt-4 text-xs text-slate-500"}
                         "播放线会循环移动, Marbles 在靠近当前时间点时会轻微放大."]])
                     (finally
                       (when-let [id @frame-id-atom]
                         (js/cancelAnimationFrame id))))))})

^{::clerk/visibility {:code :show :result :hide}}
(def reactive-demo
  {:title "输入 → debounce → switchMap → 响应"
   :duration 4200
   :tracks [{:label "输入流"
             :color "#6366f1"
             :events [{:t 200 :value "c"}
                      {:t 650 :value "cl"}
                      {:t 1050 :value "clj"}
                      {:t 2500 :value "cloj"}
                      {:t 3100 :value "clojure"}
                      {:t 4000 :kind :complete}]}
            {:label "debounce 300ms"
             :color "#14b8a6"
             :events [{:t 1400 :value "clj"}
                      {:t 3400 :value "clojure"}
                      {:t 4000 :kind :complete}]}
            {:label "switchMap 请求"
             :color "#f59e0b"
             :events [{:t 1600 :value "GET /clj"}
                      {:t 3600 :value "GET /clojure"}]}
            {:label "响应流"
             :color "#10b981"
             :events [{:t 2100 :value "200"}
                      {:t 3900 :value "200"}
                      {:t 4100 :kind :complete}]}]})

^{::clerk/visibility {:code :show :result :show}}
(defn marble
  "用自定义 marble viewer 渲染 reactive 事件轨道."
  [spec]
  (clerk/with-viewer marble-viewer spec))

^{::clerk/visibility {:code :show :result :show}}
(marble reactive-demo)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 使用方式\n\n- `:duration` 控制动画循环时长, 单位毫秒.\n- 每条 `:tracks` 提供 `:label` 与 `:events`, `:events` 中包含 `:t` (时间) 与 `:value` (显示内容).\n- `:kind` 支持 `:next`(默认), `:complete`, `:error`, 可用于展示结束或错误事件.\n- 只需替换示例数据, 就能展示不同 reactive 管道的通信过程.")
