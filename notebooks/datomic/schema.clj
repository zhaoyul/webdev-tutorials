^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.schema
  "Datomic Schema 定义与数据建模."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # Schema 定义
;;
;; Datomic 采用 Schema-on-write 模式, 需要在写入数据前定义 schema。
;; Schema 本身也是数据, 通过事务添加到数据库。

;; ## 属性的核心组成

;; 每个属性定义包含以下必需字段:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["字段" "说明" "可选值"]
  :rows [[":db/ident" "属性的唯一标识符 (keyword)" "如 :user/name"]
         [":db/valueType" "值的类型" ":db.type/string, :db.type/long, :db.type/ref 等"]
         [":db/cardinality" "基数 (单值/多值)" ":db.cardinality/one 或 :db.cardinality/many"]
         [":db/doc" "文档说明 (可选)" "字符串"]]})

;; ## 值类型 (Value Types)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["类型" "Clojure 类型" "说明"]
  :rows [[":db.type/string" "String" "字符串"]
         [":db.type/long" "Long" "64位整数"]
         [":db.type/double" "Double" "双精度浮点数"]
         [":db.type/boolean" "Boolean" "布尔值"]
         [":db.type/instant" "java.util.Date" "时间戳"]
         [":db.type/uuid" "java.util.UUID" "UUID"]
         [":db.type/keyword" "Keyword" "Clojure 关键字"]
         [":db.type/ref" "Long (Entity ID)" "引用其他实体"]]})

;; ## 实际示例

;; 创建一个新数据库:
^{::clerk/visibility {:code :show :result :show}}
(def conn (c/create-database! "schema-demo"))

;; ### 定义用户 Schema
^{::clerk/visibility {:code :show :result :hide}}
(def user-schema
  [{:db/ident       :user/username
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity  ;; 唯一标识
    :db/doc         "用户名, 唯一"}
   {:db/ident       :user/email
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/age
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/active?
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/created-at
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one}])

;; 应用 schema:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn {:tx-data user-schema})

;; ### 多值属性 (Cardinality Many)

^{::clerk/visibility {:code :show :result :hide}}
(def tag-schema
  [{:db/ident       :article/tags
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many  ;; 一篇文章可以有多个标签
    :db/doc         "文章标签"}])

^{::clerk/visibility {:code :show :result :show}}
(d/transact conn {:tx-data tag-schema})

;; ### 引用类型 (Reference)

;; 引用类型用于建立实体之间的关系:

^{::clerk/visibility {:code :show :result :hide}}
(def article-schema
  [{:db/ident       :article/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :article/author
    :db/valueType   :db.type/ref  ;; 引用 user 实体
    :db/cardinality :db.cardinality/one}
   {:db/ident       :article/likes
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])  ;; 多个用户可以点赞

^{::clerk/visibility {:code :show :result :show}}
(d/transact conn {:tx-data article-schema})

;; ## 唯一性约束

;; Datomic 支持两种唯一性:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["类型" "说明" "用途"]
  :rows [[":db.unique/identity" "值唯一, 可作为 lookup ref" "如用户名, 邮箱"]
         [":db.unique/value" "值唯一, 但不能作为 lookup ref" "如序列号"]]})

;; ## 组件属性 (Component)

;; 组件属性表示"拥有"关系, 删除父实体时会级联删除子实体:

^{::clerk/visibility {:code :show :result :hide}}
(def address-schema
  [{:db/ident       :address/street
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :address/city
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/address
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}])  ;; address 是 user 的组件

^{::clerk/visibility {:code :show :result :show}}
(d/transact conn {:tx-data address-schema})

;; ## 使用 Schema 写入数据

;; 现在可以写入符合 schema 的数据:

^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:user/username  "alice"
              :user/email     "alice@example.com"
              :user/age       28
              :user/active?   true
              :user/created-at (java.util.Date.)
              :user/address   {:address/street "中关村大街1号"
                               :address/city   "北京"}}]})

;; 查询验证:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find (pull ?e [* {:user/address [*]}])
       :where [?e :user/username]]
     (d/db conn))

;; ## Schema 演化

;; Datomic schema 可以演化:
;; - ✅ 添加新属性
;; - ✅ 修改属性的 :db/doc
;; - ❌ 不能修改 :db/valueType
;; - ❌ 不能修改 :db/cardinality

;; 添加新属性示例:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:db/ident       :user/phone
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "用户电话 (新增字段)"}]})

;; ## 下一步
;;
;; - [transactions.clj](transactions.clj) - 事务操作详解
;; - [queries.clj](queries.clj) - Datalog 查询
