^{:nextjournal.clerk/visibility {:code :hide}}
(ns advanced-note.reagent-clj-atom-sync
  "CLJ atom 变化驱动 Reagent 视图更新."
  (:require [nextjournal.clerk :as clerk]))


;; # CLJ 进程 atom 驱动 Reagent

;; 这个示例展示 `::clerk/sync` 在 JVM 与浏览器之间同步 atom. 当 CLJ 进程中的 atom 更新时, 浏览器里的 Reagent 组件会同步更新, 不需要手动刷新页面.

;; ## 1. 定义同步 atom
;; 使用 `::clerk/sync true` 标记 atom, Clerk 会把它同步到前端运行环境.

^{::clerk/sync true
  ::clerk/visibility {:code :show :result :show}}
(defonce !pulse
  (atom {:value 0
         :status "准备中"
         :updated-at (str (java.time.Instant/now))}))


;; ## 2. 在 CLJ 端驱动状态变化

;; 下面的函数在 JVM 进程里启动一个循环, 周期性更新 `!pulse`. 你可以按需启动或停止.

^{::clerk/visibility {:code :show :result :hide}}
(defonce !pulse-running? (atom false))

^{::clerk/visibility {:code :show :result :hide}}
(defonce !pulse-worker (atom nil))

^{::clerk/visibility {:code :show :result :show}}
(defn start-pulse!
  "启动后台循环, 周期性更新 `!pulse`."
  []
  (when-not @!pulse-running?
    (reset! !pulse-running? true)
    (reset! !pulse-worker
            (future
              (loop [n 0]
                (when @!pulse-running?
                  (swap! !pulse assoc
                         :value (mod n 100)
                         :status (if (zero? (mod n 15)) "更新中" "运行中")
                         :updated-at (str (java.time.Instant/now)))
                  (Thread/sleep 900)
                  (recur (inc n)))))))
  :started)

^{::clerk/visibility {:code :show :result :show}}
(defn stop-pulse!
  "停止后台循环."
  []
  (reset! !pulse-running? false)
  (when-let [worker @!pulse-worker]
    (future-cancel worker)
    (reset! !pulse-worker nil))
  :stopped)

;; ## 3. Reagent 组件读取同步状态
;; 下方组件直接读取 `!pulse` 的值. 当 JVM 端更新 atom, 组件会自动刷新.

^{::clerk/visibility {:code :show :result :hide}}
(def pulse-viewer
  {:render-fn '(fn [{:keys [title]}]
                 (let [{:keys [value status updated-at]} @!pulse]
                   [:div {:class "max-w-xl rounded-xl border border-slate-200 bg-white p-4 shadow-sm"}
                    [:div {:class "text-lg font-semibold text-slate-800"} title]
                    [:div {:class "mt-3 flex items-center gap-4"}
                     [:div {:class "text-3xl font-semibold text-slate-900"} value]
                     [:div {:class "text-sm text-slate-500"} status]]
                    [:div {:class "mt-2 text-xs text-slate-400"}
                     "更新时间: " (str updated-at)]
                    [:div {:class "mt-4 flex flex-wrap gap-2"}
                     [:button {:class "rounded-lg border border-slate-200 px-3 py-1 text-slate-600 hover:bg-slate-100"
                               :on-click #(reset! !pulse {:value 0
                                                          :status "已重置"
                                                          :updated-at (.toISOString (js/Date.))})}
                      "前端重置"]
                     [:span {:class "text-xs text-slate-400"} "提示: JVM 端更新会覆盖此值"]]]))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer pulse-viewer
  {:title "来自 JVM 的状态脉冲"})

;; ## 4. 使用方式

;; - 在 REPL 或笔记本里执行 `(start-pulse!)` 启动更新循环.
;; - 执行 `(stop-pulse!)` 停止更新.
;; - 当 CLJ 端更新 `!pulse`, 前端展示会同步变化.


(comment
  (start-pulse!)
  (stop-pulse!)
  ,)
