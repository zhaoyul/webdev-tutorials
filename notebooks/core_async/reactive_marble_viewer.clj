^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns core_async.reactive_marble_viewer
  (:require [nextjournal.clerk :as clerk]))


;; # Reactive marble diagram viewer

;; 这个 notebook 定义了一个 marble diagram viewer, 用来可视化 reactive 过程中的事件流与通信节奏.
;; - 事件流以“轨道”排列, 每条轨道代表一个 stream.
;; - 播放线会自动循环, 用动画展示事件的出现时刻.
;; - 示例参考 [ReactiveX/RxClojure](https://github.com/ReactiveX/RxClojure) 的语义, 但数据完全可自定义.

^{::clerk/visibility {:code :show :result :hide}}
(def marble-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:keys [title duration tracks playback-rate width loop?]
                     :or {playback-rate 1
                          loop? false}}]
                 (let [duration (or duration 4000)
                       tracks (or tracks [])
                       width (or width (max 760 (* 0.2 duration)))
                       row-height 64
                       timeline-left 20
                       timeline-right 44
                       timeline-span (max 80 (- width timeline-left timeline-right))
                       speed-options [0.5 0.75 1 1.25 1.5 2 3]
                       format-speed (fn [speed]
                                      (if (js/Number.isInteger speed)
                                        (str speed "x")
                                        (str (.toFixed speed 2) "x")))
                       ->x (fn [t]
                             (+ timeline-left
                                (* timeline-span (/ t duration))))
                       ->playhead-x (fn [progress]
                                      (+ timeline-left
                                         (* timeline-span progress)))]
                   (reagent.core/with-let [speed-atom (reagent.core/atom (if (pos? playback-rate) playback-rate 1))
                                           loop-atom (reagent.core/atom (boolean loop?))
                                           progress-atom (reagent.core/atom (if loop? 0 1))
                                           start-ts (atom (js/Date.now))
                                           timer-id (atom nil)
                                           sync-playback! (fn []
                                                            (when-let [id @timer-id]
                                                              (js/clearInterval id)
                                                              (reset! timer-id nil))
                                                            (if @loop-atom
                                                              (do
                                                                (reset! progress-atom 0)
                                                                (reset! start-ts (js/Date.now))
                                                                (reset! timer-id
                                                                        (js/setInterval
                                                                         (fn []
                                                                           (let [speed (max 0.1 @speed-atom)
                                                                                 cycle-ms (/ duration speed)
                                                                                 elapsed (- (js/Date.now) @start-ts)
                                                                                 wrapped (mod elapsed cycle-ms)
                                                                                 progress (/ wrapped cycle-ms)]
                                                                             (reset! progress-atom progress)))
                                                                         16)))
                                                              (reset! progress-atom 1)))]
                     (when (and @loop-atom (nil? @timer-id))
                       (sync-playback!))
                     (let [progress @progress-atom
                           playhead-x (->playhead-x progress)
                           reached? (fn [t]
                                      (<= (/ t duration) progress))]
                       [:div {:class "rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                        [:div {:class "flex flex-wrap items-center justify-between gap-3"}
                         [:div {:class "text-lg font-semibold text-slate-800"}
                          (or title "Reactive marble diagram")]
                         [:div {:class "flex items-center gap-3 text-xs text-slate-500"}
                          [:label {:class "inline-flex items-center gap-1.5 text-slate-600"}
                           [:input {:type "checkbox"
                                    :checked @loop-atom
                                    :on-change (fn [e]
                                                 (reset! loop-atom (.. e -target -checked))
                                                 (sync-playback!))}]
                           [:span "循环播放"]]
                          [:span "播放倍速"]
                          [:select {:class "rounded border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700"
                                    :value (str @speed-atom)
                                    :disabled (not @loop-atom)
                                    :on-change (fn [e]
                                                 (reset! speed-atom
                                                         (js/parseFloat (.. e -target -value)))
                                                 (when @loop-atom
                                                   (sync-playback!)))}
                           (for [option speed-options]
                             ^{:key (str "speed-" option)}
                             [:option {:value (str option)}
                              (format-speed option)])]
                          [:span {:class "tabular-nums text-slate-400"}
                           (format-speed @speed-atom)]
                          [:span {:class "text-slate-400"}
                           (str "循环 " duration " ms")]
                          (when-not @loop-atom
                            [:span {:class "rounded bg-slate-100 px-2 py-0.5 text-slate-500"}
                             "结束状态"])]]
                        [:div {:class "mt-4 w-full overflow-x-auto pb-2"}
                         [:div {:class "min-w-max space-y-3 pr-2"}
                          (for [{:keys [label events color] :or {color "#6366f1"}} tracks
                                :let [track-color color]]
                            ^{:key label}
                            [:div {:class "flex items-center gap-3" :style {:height row-height}}
                             [:div {:class "w-28 shrink-0 text-sm font-medium text-slate-600"} label]
                             [:div {:class "relative" :style {:width (str width "px")}}
                              [:div {:class "absolute top-1/2 bg-slate-900"
                                     :style {:left (str timeline-left "px")
                                             :width (str timeline-span "px")
                                             :height "3px"
                                             :border-radius "9999px"
                                             :transform "translateY(-50%)"}}]
                              [:div {:class "absolute top-1/2 bg-slate-900"
                                     :style {:left (str timeline-left "px")
                                             :width "3px"
                                             :height "48px"
                                             :border-radius "9999px"
                                             :transform "translate(-50%, -50%)"}}]
                              [:svg {:class "absolute overflow-visible"
                                     :style {:left (str (+ timeline-left timeline-span -6) "px")
                                             :top "50%"
                                             :width "18px"
                                             :height "18px"
                                             :transform "translateY(-50%)"}}
                               [:path {:d "M2 2 L14 9 L2 16"
                                       :fill "none"
                                       :stroke "#0f172a"
                                       :stroke-width "3"
                                       :stroke-linecap "round"
                                       :stroke-linejoin "round"}]]
                              [:div {:class "absolute top-0 bottom-0 w-0.5 bg-indigo-500/80"
                                     :style {:left (str playhead-x "px")
                                             :transition "left 16ms linear"}}]
                              (for [[event-idx {:keys [t value kind color]}] (map-indexed vector events)]
                                (let [kind (or kind :next)
                                      x (->x t)
                                      visible? (reached? t)
                                      event-color (or color track-color "#6366f1")
                                      tooltip (case kind
                                                :complete (str label " 完成 @" t "ms")
                                                :error (str label " 错误 @" t "ms")
                                                (str "值: " value "\n时间: " t "ms"))]
                                  (case kind
                                    :complete
                                    ^{:key (str label "-complete-" event-idx "-" t)}
                                    [:div {:class "absolute top-1/2 h-5 w-1 rounded-full bg-slate-800"
                                           :style {:left (str x "px")
                                                   :transform "translate(-50%, -50%)"
                                                   :opacity (if visible? 1 0)
                                                   :transition "opacity 120ms linear"}
                                           :title tooltip
                                           :aria-label tooltip}]
                                    :error
                                    ^{:key (str label "-error-" event-idx "-" t)}
                                    [:div {:class "absolute top-1/2 text-lg font-bold leading-none text-rose-500"
                                           :style {:left (str x "px")
                                                   :transform (str "translate(-50%, -50%) scale(" (if visible? "1" "0.85") ")")
                                                   :opacity (if visible? 1 0)
                                                   :transition "opacity 120ms linear, transform 140ms ease-out"}
                                           :title tooltip
                                           :aria-label tooltip}
                                    "×"]
                                    ^{:key (str label "-" event-idx "-" t "-" value)}
                                    [:div {:class "absolute top-1/2 h-8 w-8 rounded-full border-[3px] border-slate-900 shadow-sm"
                                           :style {:left (str x "px")
                                                   :transform (str "translate(-50%, -50%) scale(" (if visible? "1" "0.75") ")")
                                                   :opacity (if visible? 1 0)
                                                   :background-color event-color
                                                   :transition "opacity 120ms linear, transform 160ms ease-out"}
                                           :title tooltip
                                           :aria-label tooltip}])))]])]]
                        [:div {:class "mt-4 text-xs text-slate-500"}
                         "样式参考 marble 线图: 左起点竖线, 右箭头, 事件球按时间出现并可 tooltip 查看详情."]])
                     (finally
                       (when-let [id @timer-id]
                         (js/clearInterval id))))))})

^{::clerk/visibility {:code :show :result :hide}}
(def reactive-demo
  {:title "输入 → debounce → switchMap → 响应"
   :duration 4200
   :playback-rate 1.25
   :loop? false
   :width 780
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

;; ## 使用方式
;; - `:loop?` 控制是否循环播放, 默认 `false`, 关闭时直接展示结束状态.
;; - `:duration` 控制一轮时间轴时长, 单位毫秒.n- `:playback-rate` 指定循环播放时的倍速, 例如 `1.25` 表示 1.25x.
;; - `:width` 控制单条轨道宽度(像素), 超出可视区时可左右滑动查看.
;; - 每条 `:tracks` 提供 `:label` 与 `:events`, `:events` 中包含 `:t` (时间) 与 `:value` (事件值).
;; - `:kind` 支持 `:next`(默认), `:complete`, `:error`, 可用于展示结束或错误事件.
;; - 小球不显示文字, 将鼠标悬停在事件点上可查看 tooltip 详情.
