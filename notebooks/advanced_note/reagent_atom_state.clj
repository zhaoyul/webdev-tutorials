^{:nextjournal.clerk/visibility {:code :hide}}
(ns advanced-note.reagent-atom-state
  "Reagent atom 驱动状态的组件示例."
  (:require [nextjournal.clerk :as clerk]))


;; # Reagent 的 atom 状态

;; 这份笔记展示如何在 Clerk 的浏览器渲染环境中使用 `reagent/atom` 管理组件状态. 示例强调状态变化对 UI 的即时影响, 适合用于理解交互式组件的更新模式.

;; ## 计数器与表单联动

;; 下面的组件把计数值, 步长和名字都放在同一个 atom 中, 通过 `swap!` 或 `reset!` 驱动 UI 变化.

^{::clerk/visibility {:code :show :result :hide}}
(def counter-viewer
  {:render-fn '(fn [{:keys [title min-step max-step]}]
                 (reagent.core/with-let [!state (reagent.core/atom {:count 0
                                                                    :step min-step
                                                                    :name "Clerk"})]
                   (let [{:keys [count step name]} @!state]
                     [:div {:class "max-w-xl rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                      [:div {:class "text-lg font-semibold text-slate-800"} title]
                      [:div {:class "mt-3 flex items-center gap-3"}
                       [:button {:class "rounded-lg border border-slate-200 px-3 py-1 text-slate-600 hover:bg-slate-100"
                                 :on-click #(swap! !state update :count - step)}
                        "减"]
                       [:div {:class "min-w-[64px] text-center text-2xl font-semibold text-slate-800"} count]
                       [:button {:class "rounded-lg border border-slate-200 px-3 py-1 text-slate-600 hover:bg-slate-100"
                                 :on-click #(swap! !state update :count + step)}
                        "加"]]
                      [:div {:class "mt-4 flex items-center gap-3"}
                       [:label {:class "text-sm text-slate-600"} "步长"]
                       [:input {:type "range"
                                :min min-step
                                :max max-step
                                :value step
                                :on-change #(swap! !state assoc :step
                                                   (js/parseInt (.. % -target -value) 10))}]
                       [:span {:class "text-sm font-medium text-slate-700"} (str step)]]
                      [:div {:class "mt-4 flex flex-wrap items-center gap-3"}
                       [:label {:class "text-sm text-slate-600"} "名字"]
                       [:input {:class "min-w-[180px] rounded-lg border border-slate-200 px-2 py-1 text-slate-700"
                                :value name
                                :on-change #(swap! !state assoc :name (.. % -target -value))}]
                       [:button {:class "rounded-lg border border-slate-200 px-3 py-1 text-slate-600 hover:bg-slate-100"
                                 :on-click #(reset! !state {:count 0 :step min-step :name "Clerk"})}
                        "重置"]]
                      [:div {:class "mt-4 text-sm text-slate-500"}
                       "当前状态: " name " / " count]])))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer counter-viewer
  {:title "atom 驱动的计数器"
   :min-step 1
   :max-step 5})


;; ## 说明

;; - 组件状态存储在浏览器中的 `reagent/atom`.
;; - `swap!` 负责局部更新, `reset!` 则一次性覆盖.
;; - Clerk 只负责把 `:render-fn` 送到浏览器执行, 真正的交互逻辑全部在客户端完成.
