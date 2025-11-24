(ns notebooks.plc.simple_executor
  (:require [ladder.sim :as sim]
            [nextjournal.clerk :as clerk]))

;; # 最简 PLC 执行器示例
;; 参考 notebooks/plc/design.org 的结构, 只覆盖 NO/TON/COIL 路径,
;; 并借鉴 plc-ladder/ladder-editor-main/ladder_editor.html 的网格布局思路,
;; 用 Clerk 展示单网络的逐扫状态。

(def demo-program sim/demo-program)

(def demo-inputs
  "每次扫描的 I0 输入, 使用 1s 周期模拟 design.org 中的时间轴。"
  [[false] [true] [true] [true] [true]])

(def demo-trace
  "累积扫描结果, 时间戳按 1s 递增, 方便观察 TON 延时。"
  (reductions (fn [ctx [inputs now]]
                (sim/scan-step ctx inputs now))
              (sim/->ctx demo-program {:scan-ms 1000})
              (map-indexed (fn [idx inputs] [inputs (* 1000 idx)]) demo-inputs)))

(defn trace-table [trace]
  (map-indexed (fn [idx ctx]
                 (let [timer (first (:timers ctx))]
                   {:scan idx
                    :inputs (get-in ctx [:io :I])
                    :outputs (get-in ctx [:io :Q])
                    :timer-acc (some-> timer :acc)
                    :timer-base (some-> timer :base)
                    :timer-done? (some-> timer :done?)}))
               trace))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table (trace-table demo-trace))

(defn cell-style [active?]
  {:border "1px solid #ddd"
   :border-radius "6px"
   :padding "8px"
   :background (if active? "#e6ffe6" "#fff")
   :text-align "center"
   :font-family "SFMono-Regular, Menlo, Consolas, monospace"})

(defn render-network [network]
  (let [cells (:cells network)
        cols (:cols network)
        rows (:rows network)]
    [:div {:style {:margin-top "16px"}}
     [:div {:style {:font-weight "700" :margin-bottom "6px"}} "网格取自 ladder_editor.html 的列/行排布方式"]
     [:div {:style {:display "grid"
                    :gridTemplateColumns (str "repeat(" cols ", 140px)")
                    :gridTemplateRows (str "repeat(" rows ", auto)")
                    :gap "8px"
                    :background "#f7f7f7"
                    :padding "12px"
                    :borderRadius "8px"
                    :boxShadow "0 2px 6px rgba(0,0,0,0.08)"}}
      (for [row cells
            cell row]
        [:div {:style (cell-style (:state cell))}
         [:div {:style {:font-weight "700"}} (name (:code cell))]
         [:div {:style {:color "#888" :margin-top "4px"}}
          (str (:data cell))]
         [:div {:style {:margin-top "4px"
                        :color (if (:state cell) "#1b5e20" "#999")
                        :font-weight "700"}}
          (if (:state cell) "导通" "断开")]])]]))

(def last-frame (last demo-trace))
(def last-network (-> last-frame :program :networks first))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/html (render-network last-network))

;; ## 说明
;; - 只实现 NO/NC/RE/FE/TON/COIL 子集, 便于后续在 src/ladder/sim.clj 逐步扩展。
;; - 输入使用布尔向量, scan-step 可注入 now-ms, 方便用 Clerk 重播时间轴。
