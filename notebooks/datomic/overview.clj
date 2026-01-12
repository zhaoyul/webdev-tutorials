^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.overview
  "Datomic Pull API: 声明式数据提取."
  (:require [nextjournal.clerk :as clerk]))


;; # Datomic 笔记目录
;;
;; 本页汇总 `notebooks/datomic/` 下各笔记及作用, 便于快速导航。
;;
;; ## 概述
;;
;; Datomic 是一个不可变数据库系统, 具有以下核心特性:
;; - **不可变性**: 数据只增不删, 历史永久保留
;; - **时间旅行**: 可查询任意历史时刻的数据状态
;; - **Datalog 查询**: 强大的声明式查询语言
;; - **Entity API**: 便捷的实体导航接口
;; - **Pull API**: 灵活的数据提取模式
;;
;; ## 目录
;;
;; ### 入门
;; - [intro.clj](intro.clj) - Datomic 简介与核心概念
;; - [schema.clj](schema.clj) - Schema 定义与数据建模
;;
;; ### 数据操作
;; - [transactions.clj](transactions.clj) - 事务与数据写入
;; - [queries.clj](queries.clj) - Datalog 查询基础
;;
;; ### 高级特性
;; - [pull.clj](pull.clj) - Pull API 数据提取
;; - [history.clj](history.clj) - 时间旅行与历史查询
;; - [keynote_time_travel.clj](keynote_time_travel.clj) - 时间旅行 Keynote 演示
;; - [local_persistence.clj](local_persistence.clj) - Datomic Local 持久化
;;
;; ## 支撑模块
;; - [common.clj](common.clj) - 公共初始化与辅助函数
;;
;; ## 运行说明
;;
;; 本系列笔记使用 **Datomic Local** (嵌入式内存数据库), 无需额外安装服务。
;;
;; 启动 Clerk 查看笔记:
;; ```sh
;; clojure -M:clerk
;; ```
;;
;; 或在 REPL 中:
;; ```clojure
;; (require '[nextjournal.clerk :as clerk])
;; (clerk/serve! {:browse? true})
;; (clerk/show! "notebooks/datomic/intro.clj")
;; ```
