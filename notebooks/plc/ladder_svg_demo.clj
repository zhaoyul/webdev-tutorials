(ns notebooks.plc.ladder-svg-demo
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [ladder.sim :as sim]))

;; # 使用 SVG 图标渲染梯形图
;; 参考 `plc-ladder/ladder-editor-main/ladder_editor.html` 的网格排布,
;; 直接复用 `symbols/svg` 目录下的图标, 渲染最小 NO→TON→COIL 示意并高亮导通单元。

(def svg-root "plc-ladder/ladder-editor-main/symbols/svg")

(def svg-cache
  "按指令名缓存 SVG 文本, 便于渲染。"
  (into {}
        (for [f ["NO" "NC" "RE" "FE" "TON" "TOF" "TP"
                 "COIL" "COILL" "COILU" "CONN" "NOP"]]
          [ (keyword f)
            (slurp (str svg-root "/" f ".svg"))])))

(defn icon [code]
  (let [svg (get svg-cache code "<svg></svg>")]
    [:div {:style {:width "100%" :height "100%"}
           :dangerouslySetInnerHTML {:__html svg}}]))

(defn cell-box [{:keys [code state data]}]
  (let [active? (true? state)]
    [:div {:style {:border "1px solid #dcdcdc"
                   :borderRadius "10px"
                   :padding "6px"
                   :background (if active? "#e6ffe6" "#fff")
                   :boxShadow (when active? "0 0 8px rgba(0,128,0,0.35)")
                   :display "flex"
                   :alignItems "center"
                   :justifyContent "center"}}
     (icon code)]))

(defn render-network [network]
  (let [cells (:cells network)
        rows (:rows network)
        cols (:cols network)]
    [:div {:style {:marginTop "12px"}}
     [:div {:style {:fontWeight "700" :marginBottom "8px"}}
      "布局取自 ladder_editor.html: grid + 图标复用 symbols/svg"]
     [:div {:style {:display "grid"
                    :gridTemplateColumns (str "repeat(" cols ", 90px)")
                    :gridTemplateRows (str "repeat(" rows ", 90px)")
                    :gap "10px"
                    :padding "12px"
                    :background "#f5f5f5"
                    :borderRadius "12px"
                    :boxShadow "0 4px 10px rgba(0,0,0,0.08)"}}
      (for [row cells
            cell row]
        ^{:key (str (:code cell) (:data cell) (:state cell))}
        (cell-box cell))]]))

;; 使用现有 demo 程序与仿真轨迹
(def trace
  (reductions (fn [ctx [inputs now]] (sim/scan-step ctx inputs now))
              (sim/->ctx sim/demo-program {:scan-ms 1000})
              (map-indexed (fn [idx inputs] [inputs (* 1000 idx)])
                           [[false] [true] [true] [true] [true]])))

(def last-frame (last trace))
(def last-network (-> last-frame :program :networks first))

;;^{::clerk/visibility {:code :hide :result :show}}
(clerk/html (render-network last-network))


;; ## 观察
;; - 第 3 次扫描后 TON 完成, COIL 置位, 背景高亮。
;; - SVG 图标直接来自 ladder-editor, 方便后续扩展更多指令。
