^{:nextjournal.clerk/visibility {:code :hide}}
(ns advanced-note.reagent-third-party-render
  "Reagent 驱动第三方 JS 渲染示例."
  (:require [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# Reagent 驱动第三方 JS 渲染\n\n本笔记演示如何在 Clerk 的 `:render-fn` 中按需加载第三方 JavaScript 库, 并用 `reagent/atom` 驱动渲染结果的变化. 示例使用 Mermaid 绘制图形, 通过按钮切换不同的图表定义.")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## Mermaid 示例数据\n\n这些字符串会被直接传给 Mermaid, 用来生成 SVG 图形.")

^{::clerk/visibility {:code :show :result :hide}}
(def mermaid-diagrams
  {"流程图"
   "flowchart LR
    Start --> Decide
    Decide -->|yes| Ship
    Decide -->|no| Iterate"

   "状态图"
   "stateDiagram-v2
    [*] --> Idle
    Idle --> Busy
    Busy --> Idle
    Busy --> Error
    Error --> Idle"

   "序列图"
   "sequenceDiagram
    participant User
    participant API
    User->>API: 请求数据
    API-->>User: 返回结果"})

^{::clerk/visibility {:code :show :result :hide}}
(def mermaid-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [label->diagram]
                 (reagent.core/with-let [!active (reagent.core/atom (ffirst label->diagram))]
                   (let [diagram (get label->diagram @!active)]
                     [:div {:class "max-w-2xl rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                      [:div {:class "text-lg font-semibold text-slate-800"} "Mermaid 图表预览"]
                      [:div {:class "mt-3 flex flex-wrap gap-2"}
                       (for [label (keys label->diagram)]
                         ^{:key label}
                         [:button {:class (str "rounded-full border px-3 py-1 text-sm "
                                               (if (= label @!active)
                                                 "border-slate-700 bg-slate-900 text-white"
                                                 "border-slate-200 text-slate-600 hover:bg-slate-100"))
                                   :on-click #(reset! !active label)}
                          label])]
                      [:div {:class "mt-4 rounded-lg border border-slate-200 bg-slate-50 p-3"}
                       [nextjournal.clerk.render/with-d3-require {:package ["mermaid@8.14/dist/mermaid.js"]}
                        (fn [mermaid]
                          (reagent.core/with-let [_ (.initialize mermaid (clj->js {:startOnLoad false}))]
                            [:div {:class "min-h-[160px]"
                                   :key diagram
                                   :ref (fn [el]
                                          (when el
                                            (.render mermaid (str (gensym "mermaid")) diagram
                                                     (fn [svg] (set! (.-innerHTML el) svg)))))}]))]]])))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mermaid-viewer mermaid-diagrams)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 说明\n\n- Mermaid 通过 `with-d3-require` 在浏览器端按需加载, 适合演示无需打包的第三方库.\n- `reagent/atom` 保存当前选中的图表标签, 按钮更新状态后触发重新渲染.\n- 如果所在环境无法访问 CDN, 需要把 Mermaid 放到本地资源或调整加载方式.")
