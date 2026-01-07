^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.intro
  "Datomic 入门: 核心概念与基本用法."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # Datomic 介绍
;;
;; Datomic 是一个独特的数据库系统, 由 Rich Hickey (Clojure 创始人) 设计。
;; 它的核心理念是将数据视为**不可变的事实**, 而非可变的记录。

;; ## 核心概念

;; ### 1. 不可变性 (Immutability)
;; 在 Datomic 中, 数据永远不会被修改或删除, 只会被追加。
;; 每次"更新"实际上是添加一个新的事实, 标记旧事实不再有效。

;; ### 2. 时间维度 (Time)
;; 每个数据点都带有时间戳, 你可以查询任意历史时刻的数据库状态。
;; 这让审计、调试和数据恢复变得简单。

;; ### 3. Datom (数据原子)
;; Datomic 中的最小数据单元是 **Datom**, 它是一个五元组:
;; - **E (Entity)**: 实体 ID
;; - **A (Attribute)**: 属性
;; - **V (Value)**: 值
;; - **T (Transaction)**: 事务 ID
;; - **Op (Operation)**: 操作 (添加/撤回)

;; ### 4. Schema-on-write
;; 与许多 NoSQL 数据库不同, Datomic 要求预先定义 schema,
;; 这提供了数据验证和类型安全。

;; ## 快速开始

;; 让我们创建一个内存数据库来演示基本用法:

^{::clerk/visibility {:code :show :result :show}}
(def conn (c/setup-demo-db! "intro-demo"))

;; 查看当前数据库:
^{::clerk/visibility {:code :show :result :show}}
(def db (d/db conn))

;; ## 基本事务 (Transaction)

;; 添加一些示例数据:
^{::clerk/visibility {:code :show :result :show}}
(def tx-result
  (d/transact conn {:tx-data [{:user/name "测试用户"
                               :user/email "test@example.com"
                               :user/age 30}]}))

;; 事务返回结果包含:
;; - `:db-before` - 事务前的数据库快照
;; - `:db-after` - 事务后的数据库快照
;; - `:tx-data` - 本次事务产生的 datoms
;; - `:tempids` - 临时 ID 到实际 ID 的映射

^{::clerk/visibility {:code :show :result :show}}
(keys tx-result)

;; ## 基本查询

;; 使用 Datalog 查询所有用户:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?email
       :where
       [?e :user/name ?name]
       [?e :user/email ?email]]
     (d/db conn))

;; 带条件的查询 - 查找年龄大于 25 的用户:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?age
       :where
       [?e :user/name ?name]
       [?e :user/age ?age]
       [(> ?age 25)]]
     (d/db conn))

;; ## Entity API

;; 通过 lookup ref 获取实体:
^{::clerk/visibility {:code :show :result :show}}
(def user-entity (d/pull (d/db conn) '[*] [:user/name "测试用户"]))

(clerk/table [user-entity])

;; ## 与传统数据库的对比

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["特性" "传统 RDBMS" "Datomic"]
  :rows [["数据模型" "表/行/列" "Entity/Attribute/Value"]
         ["更新方式" "原地修改" "追加新事实"]
         ["历史数据" "需手动实现" "内置支持"]
         ["查询语言" "SQL" "Datalog"]
         ["事务" "ACID" "ACID + 时间维度"]
         ["扩展性" "垂直扩展" "读写分离, 水平扩展读"]]})

;; ## 下一步

;; 继续阅读:
;; - [schema.clj](schema.clj) - 深入了解 Schema 定义
;; - [transactions.clj](transactions.clj) - 事务操作详解
;; - [queries.clj](queries.clj) - Datalog 查询语法
