^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.transactions
  "Datomic 事务: 数据写入与修改."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # 事务 (Transactions)
;;
;; Datomic 中所有的数据变更都通过事务完成。
;; 事务是原子的, 要么全部成功, 要么全部失败。

;; ## 准备工作

^{::clerk/visibility {:code :show :result :show}}
(def conn (c/setup-demo-db! "transactions-demo"))

;; 添加示例数据:
^{::clerk/visibility {:code :show :result :hide}}
(c/seed-sample-data! conn)

;; ## 基本事务操作

;; ### 添加数据 (Assert)

;; 使用 map 形式添加新实体:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:user/name  "赵六"
              :user/email "zhaoliu@example.com"
              :user/age   35}]})

;; ### 使用临时 ID

;; 临时 ID 用于在同一事务中引用新创建的实体:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:db/id        "temp-author"
              :user/name    "钱七"
              :user/email   "qianqi@example.com"
              :user/age     29}
             {:article/title   "Datomic 入门指南"
              :article/content "这是一篇关于 Datomic 的教程..."
              :article/author  "temp-author"  ;; 引用同一事务中的临时 ID
              :article/tags    #{"教程" "数据库" "Clojure"}}]})

;; ### 使用 Lookup Ref

;; Lookup ref 是用唯一属性值来引用实体的方式:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:article/title   "高级 Datalog 技巧"
              :article/content "本文介绍 Datalog 查询的高级用法..."
              :article/author  [:user/name "张三"]  ;; lookup ref
              :article/tags    #{"查询" "进阶"}}]})

;; ## 修改数据 (Upsert)

;; 对于有唯一属性的实体, 相同唯一值会更新而非插入:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:user/name "张三"          ;; 唯一标识
              :user/age  29}]})         ;; 更新年龄

;; 验证更新:
^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db conn) '[:user/name :user/age] [:user/name "张三"])

;; ## 撤回数据 (Retract)

;; ### 撤回单个属性值
^{::clerk/visibility {:code :show :result :show}}
(let [zhangsan-id (d/q '[:find ?e . :where [?e :user/name "张三"]] (d/db conn))]
  (d/transact conn
    {:tx-data [[:db/retract zhangsan-id :user/email "zhangsan@example.com"]]}))

;; ### 撤回实体的所有属性
^{::clerk/visibility {:code :show :result :show}}
(let [zhaoliu-id (d/q '[:find ?e . :where [?e :user/name "赵六"]] (d/db conn))]
  (d/transact conn
    {:tx-data [[:db/retractEntity zhaoliu-id]]}))

;; 验证删除:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name :where [?e :user/name ?name]] (d/db conn))

;; ## 列表操作语法

;; Datomic 支持两种事务数据格式:

;; **Map 形式** (推荐用于添加):
^{::clerk/visibility {:code :show :result :hide}}
(comment
  {:user/name "example" :user/age 25})

;; **List 形式** (用于精细控制):
^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["操作" "语法" "说明"]
  :rows [[":db/add" "[:db/add entity attr value]" "添加属性值"]
         [":db/retract" "[:db/retract entity attr value]" "撤回属性值"]
         [":db/retractEntity" "[:db/retractEntity entity]" "撤回整个实体"]]})

;; ### List 形式示例

^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [[:db/add [:user/name "李四"] :user/age 33]]})

;; ## 事务元数据

;; 每个事务本身也是一个实体, 可以附加元数据:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn
  {:tx-data [{:user/name "孙八"
              :user/email "sunba@example.com"
              :user/age 40}
             ;; 事务元数据
             {:db/id "datomic.tx"
              :db/doc "批量导入用户数据"}]})

;; 查询事务元数据:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?doc ?inst
       :where
       [?tx :db/doc ?doc]
       [?tx :db/txInstant ?inst]]
     (d/db conn))

;; ## 事务结果

;; 事务返回的结果包含有用信息:

^{::clerk/visibility {:code :show :result :show}}
(def tx-result
  (d/transact conn
    {:tx-data [{:user/name "周九"
                :user/email "zhoujiu@example.com"
                :user/age 27}]}))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["字段" "说明"]
  :rows [[":db-before" "事务前的数据库值"]
         [":db-after" "事务后的数据库值"]
         [":tx-data" "本次事务产生的所有 datoms"]
         [":tempids" "临时 ID 到实际 ID 的映射"]]})

;; 查看产生的 datoms:
^{::clerk/visibility {:code :show :result :show}}
(count (:tx-data tx-result))

;; ## 并发控制

;; Datomic 使用乐观并发控制。如果需要条件更新, 可以使用 `:db/cas`:
^{::clerk/visibility {:code :show :result :show}}
(let [lisi-id (d/q '[:find ?e . :where [?e :user/name "李四"]] (d/db conn))]
  (d/transact conn
    {:tx-data [[:db/cas lisi-id :user/age 33 34]]}))  ;; 只有当前值是 33 时才更新为 34

;; ## 下一步
;;
;; - [queries.clj](queries.clj) - Datalog 查询语法
;; - [pull.clj](pull.clj) - Pull API 数据提取
