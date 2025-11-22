(ns hugsql.overview
  {:nextjournal.clerk/visibility {:code :hide :result :show}
   :nextjournal.clerk/toc false}
  (:require [nextjournal.clerk :as clerk]))

;; # HugSQL 笔记目录
;; 本页汇总 `notebooks/hugsql/` 下各笔记及作用, 便于快速导航。

;;
;; ## 目录
;;
;; - *安装*
;;   - [install.clj](install.clj)
;; - *入门*
;;   - 从 SQL 开始: [getting_sql.clj](getting_sql.clj) (配套 [playground.sql](../../resources/hugsql/playground.sql))
;;   - 现在来点 Clojure: [getting_clj.clj](getting_clj.clj)
;; - *使用 HugSQL*
;;   - def-db-fns: [getting_clj.clj](getting_clj.clj), [usage_fns.clj](usage_fns.clj)
;;   - def-sqlvec-fns: [getting_clj.clj](getting_clj.clj), [usage_fns.clj](usage_fns.clj)
;;   - 其他有用函数: [usage_fns.clj](usage_fns.clj)
;;   - DDL (建表/删表): [crud.clj](crud.clj)
;;   - Insert/Update/Delete/Select: [crud.clj](crud.clj)
;;   - 事务: [transactions.clj](transactions.clj)
;;   - 组合性: [composability.clj](composability.clj)
;;     - Clojure 表达式: [composability.clj](composability.clj)
;;     - Snippet 片段: [composability.clj](composability.clj)
;;   - 高级用法: [advanced_usage.clj](advanced_usage.clj)
;; - *深入介绍*
;;   - SQL 文件约定: [getting_sql.clj](getting_sql.clj), [deep_dive.clj](deep_dive.clj)
;;   - 命令/结果: [deep_dive.clj](deep_dive.clj)
;;   - 参数类型: [deep_dive.clj](deep_dive.clj), [composability.clj](composability.clj), [crud.clj](crud.clj)
;;     - 值/值列表/元组/元组列表: [deep_dive.clj](deep_dive.clj), [crud.clj](crud.clj)
;;     - 标识符/标识符列表: [deep_dive.clj](deep_dive.clj), [crud.clj](crud.clj)
;;     - 原生 SQL: [advanced_usage.clj](advanced_usage.clj)
;;     - Snippet 及列表: [composability.clj](composability.clj)
;; - *常见问题/对比*
;;   - FAQ: [faq.clj](faq.clj)
;;   - 适配器切换: [adapters.clj](adapters.clj)
;;
;; ## 支撑与导航
;; - 公共初始化: [common.clj](common.clj)
