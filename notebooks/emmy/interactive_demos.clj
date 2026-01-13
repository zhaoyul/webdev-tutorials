^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.interactive-demos
  "Emmy 交互式可视化与第三方组件集成."
  (:require [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
# 复杂数学交互: MathBox 与第三方组件

这一页演示如何在 Clerk 中集成第三方 JavaScript 组件, 用更直观的方式表达数学与物理过程。
重点示例包括 MathBox 的 3D 曲线与 ECharts 的交互式曲线, 用来展示可玩性的数学交互界面。
")

;; ## MathBox 3D 曲线

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:keys [title]}]
                 (reagent.core/with-let [!freq (reagent.core/atom 1.2)
                                         !mathbox (reagent.core/atom nil)
                                         mount! (fn [el]
                                                  (when (and el (nil? @!mathbox))
                                                    (let [mathbox-fn (.-mathBox js/window)]
                                                      (when mathbox-fn
                                                        (let [controls (.-OrbitControls js/THREE)
                                                              options (cond-> {:element el
                                                                               :plugins ["core" "controls" "cursor"]}
                                                                        controls (assoc :controls {:klass controls}))
                                                              mathbox (mathbox-fn (clj->js options))
                                                              three (.-three mathbox)
                                                              renderer (.-renderer three)]
                                                          (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                                                          (when-let [camera (.-camera three)]
                                                            (.set (.-position camera) 0 0 4)
                                                            (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                                                          (let [view (.cartesian mathbox
                                                                                 (clj->js {:range [[-4 4] [-1.6 1.6] [-1.6 1.6]]
                                                                                           :scale [1 1 1]}))
                                                                _ (.axis view (clj->js {:axis 1 :color 0x94a3b8}))
                                                                _ (.axis view (clj->js {:axis 2 :color 0x94a3b8}))
                                                                _ (.axis view (clj->js {:axis 3 :color 0x94a3b8}))
                                                                _ (.grid view (clj->js {:divideX 6 :divideY 6 :color 0x1e293b}))
                                                                curve (.interval view
                                                                                 (clj->js {:width 256
                                                                                           :expr (fn [emit x i t]
                                                                                                   (let [freq @!freq
                                                                                                         y (js/Math.sin (+ (* x freq) t))
                                                                                                         z (js/Math.cos (+ (* x 0.7) (* t 0.5)))]
                                                                                                     (emit x y z)))}))]
                                                            (.line curve (clj->js {:width 4 :color 0x38bdf8})))
                                                          (reset! !mathbox mathbox))))))]
                   [:div {:class "space-y-4"}
                    [:div {:class "flex flex-wrap items-center justify-between gap-3"}
                     [:div {:class "text-lg font-semibold text-slate-800"}
                      (or title "MathBox 3D 曲线")]
                     [:div {:class "flex items-center gap-3 text-sm text-slate-600"}
                      [:span "频率"]
                      [:input {:type "range"
                               :min 0.5
                               :max 3.0
                               :step 0.1
                               :value @!freq
                               :on-change (fn [e]
                                            (reset! !freq (js/parseFloat (.. e -target -value))))}]
                      [:span (str (.toFixed @!freq 1))]]]
                    [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
                     (fn [_]
                       [:div {:style {:width "100%"
                                      :height "480px"
                                      :border "1px solid #e2e8f0"
                                      :border-radius "16px"
                                      :overflow "hidden"
                                      :background "#0f172a"}
                              :ref mount!}])]]
                   (finally
                     (when-let [mathbox @!mathbox]
                       (when (.-destroy mathbox)
                         (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-demo
  {:title "MathBox 3D 曲线"})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-viewer mathbox-demo)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
说明:
- MathBox 通过 CDN 引入, 适合快速展示 3D 数学交互.
- 控件与画布都由 Hiccup 组件驱动, 仅在渲染层使用 JS 互操作.
- 如果网络环境无法访问 CDN, 需要改为本地资源.
")

;; ## ECharts 交互式曲线

^{::clerk/visibility {:code :show :result :hide}}
(def echarts-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:keys [title color]}]
                 (reagent.core/with-let [!amp (reagent.core/atom 1.2)
                                         !chart (reagent.core/atom nil)]
                   (let [build-option (fn [amp]
                                        (let [xs (range 0 64)
                                              data (map (fn [i]
                                                          [i (* amp (js/Math.sin (/ i 6.0)))])
                                                        xs)]
                                          (clj->js {:title {:text title
                                                            :left "center"
                                                            :textStyle {:color "#0f172a"}}
                                                    :grid {:left 40 :right 20 :top 50 :bottom 30}
                                                    :xAxis {:type "value"
                                                            :axisLine {:lineStyle {:color "#94a3b8"}}
                                                            :axisLabel {:color "#64748b"}}
                                                    :yAxis {:type "value"
                                                            :axisLine {:lineStyle {:color "#94a3b8"}}
                                                            :axisLabel {:color "#64748b"}}
                                                    :series [{:type "line"
                                                              :smooth true
                                                              :data data
                                                              :showSymbol false
                                                              :lineStyle {:width 3
                                                                          :color color}}]})))
                         update-chart (fn []
                                        (when @!chart
                                          (.setOption @!chart (build-option @!amp) true)))]
                     (update-chart)
                     [:div {:class "max-w-3xl rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                      [:div {:class "text-lg font-semibold text-slate-800"} "ECharts: 交互式正弦曲线"]
                      [:div {:class "mt-3 flex items-center gap-3 text-sm text-slate-600"}
                       [:span "幅度"]
                       [:input {:type "range"
                                :min 0.6
                                :max 2.5
                                :step 0.1
                                :value @!amp
                                :on-change (fn [e]
                                             (reset! !amp (js/parseFloat (.. e -target -value)))
                                             (update-chart))}]
                       [:span (str (.toFixed @!amp 1))]]
                      [:div {:class "mt-4"}
                       [nextjournal.clerk.render/with-d3-require {:package ["echarts@5.4.3/dist/echarts.min.js"]}
                        (fn [echarts]
                          [:div {:style {:width "100%" :height "320px"}
                                 :ref (fn [el]
                                        (when (and el (nil? @!chart))
                                          (reset! !chart (.init echarts el))
                                          (update-chart)))}])]]])))})

^{::clerk/visibility {:code :show :result :hide}}
(def echarts-demo
  {:title "正弦曲线交互"
   :color "#38bdf8"})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer echarts-viewer echarts-demo)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 小结

这一页展示了两种整合路径:
- MathBox 用于 3D 数学场景, 适合几何与动力系统的可视化.
- ECharts 用于快速交互式曲线, 适合展示参数变化对函数的影响.
")
