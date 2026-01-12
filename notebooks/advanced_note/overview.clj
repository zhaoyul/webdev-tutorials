^{:nextjournal.clerk/visibility {:code :hide}}
(ns advanced-note.overview
  "高级 Notebook 功能导航."
  (:require [nextjournal.clerk :as clerk]))


;; # 高级 Notebook 功能

;; 本目录集中展示更偏进阶的 Clerk 用法, 包含 Reagent 组件状态, 第三方 JS 库加载, 以及 CLJ 端 atom 同步驱动浏览器渲染.

;; ## 导航

;; - [reagent_atom_state.clj](reagent_atom_state.clj) - Reagent atom 驱动的状态组件
;; - [reagent_third_party_render.clj](reagent_third_party_render.clj) - 运行时加载第三方 JS 并渲染
;; - [reagent_clj_atom_sync.clj](reagent_clj_atom_sync.clj) - CLJ 进程 atom 与前端同步

;; ## 打开方式

;; 可在 REPL 中执行 `(nextjournal.clerk/show! "notebooks/advanced_note/overview.clj")` 打开本页.
