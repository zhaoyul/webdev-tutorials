^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.common
  "Datomic 笔记公共模块: 数据库连接, schema 定义与辅助函数."
  (:require [datomic.client.api :as d]))

;; ## 创建内存数据库连接
;; 使用 Datomic Local 的内存存储, 适合教学和测试

(defonce client (d/client {:server-type :datomic-local
                           :storage-dir :mem
                           :system "datomic-tutorials"}))

(defn create-database!
  "创建一个新数据库, 如果已存在则先删除."
  [db-name]
  (d/delete-database client {:db-name db-name})
  (d/create-database client {:db-name db-name})
  (d/connect client {:db-name db-name}))

(defn get-connection
  "获取数据库连接."
  [db-name]
  (d/connect client {:db-name db-name}))

;; ## 示例 Schema: 用户和文章
;; 演示 Datomic schema 的基本属性类型

(def user-schema
  "用户实体的 schema 定义."
  [{:db/ident       :user/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "用户名称, 唯一标识"}
   {:db/ident       :user/email
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "用户邮箱"}
   {:db/ident       :user/age
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "用户年龄"}
   {:db/ident       :user/roles
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "用户角色 (多值引用)"}])

(def role-schema
  "角色实体的 schema 定义."
  [{:db/ident :role/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "角色名称"}])

(def article-schema
  "文章实体的 schema 定义."
  [{:db/ident       :article/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "文章标题"}
   {:db/ident       :article/content
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "文章内容"}
   {:db/ident       :article/author
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "文章作者 (引用 user)"}
   {:db/ident       :article/tags
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "文章标签 (多值字符串)"}
   {:db/ident       :article/published-at
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "发布时间"}])

(def all-schema
  "合并所有 schema."
  (concat role-schema user-schema article-schema))

;; ## 辅助函数

(defn transact-schema!
  "向数据库添加 schema."
  [conn schema]
  (d/transact conn {:tx-data schema}))

(defn setup-demo-db!
  "创建演示数据库并初始化 schema, 返回连接."
  [db-name]
  (let [conn (create-database! db-name)]
    (transact-schema! conn all-schema)
    conn))

;; ## 示例数据

(def sample-roles
  "示例角色数据."
  [{:role/name "admin"}
   {:role/name "editor"}
   {:role/name "reader"}])

(def sample-users
  "示例用户数据."
  [{:user/name  "张三"
    :user/email "zhangsan@example.com"
    :user/age   28
    :user/roles [[:role/name "admin"] [:role/name "editor"]]}
   {:user/name  "李四"
    :user/email "lisi@example.com"
    :user/age   32
    :user/roles [[:role/name "editor"]]}
   {:user/name  "王五"
    :user/email "wangwu@example.com"
    :user/age   25
    :user/roles [[:role/name "reader"]]}])

(defn seed-sample-data!
  "添加示例数据到数据库."
  [conn]
  (d/transact conn {:tx-data sample-roles})
  (d/transact conn {:tx-data sample-users}))
