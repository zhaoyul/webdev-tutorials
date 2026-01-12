^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.local_persistence
  "Datomic Local 持久化演示."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
# Datomic Local 持久化

Datomic Local 是嵌入式 Java 库, 不需要独立的 Transactor 进程。
数据直接存储在本地磁盘目录中, 适合单机应用, 微服务, 本地开发与中小型生产环境。

核心优势:
- 零配置: 无需安装或运维外部数据库进程
- 低延迟: 查询与数据在同一 JVM, 没有网络开销
- 易于部署: 只需指定本地路径
")

;; ## 配置本地存储目录

^{::clerk/visibility {:code :show :result :show}}
(def storage-dir
  (str (System/getProperty "java.io.tmpdir") "/datomic-local-demo"))

^{::clerk/visibility {:code :show :result :show}}
{:storage-dir storage-dir}

^{::clerk/visibility {:code :show :result :hide}}
(def system "datomic-local-demo")

^{::clerk/visibility {:code :show :result :hide}}
(def db-name "local-persistence")

^{::clerk/visibility {:code :show :result :hide}}
(def client
  (d/client {:server-type :datomic-local
             :storage-dir storage-dir
             :system system}))

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-db!
  "确保数据库已创建."
  [client db-name]
  (when-not (some #{db-name} (d/list-databases client {}))
    (d/create-database client {:db-name db-name})))

^{::clerk/visibility {:code :show :result :hide}}
(ensure-db! client db-name)

^{::clerk/visibility {:code :show :result :hide}}
(def conn (d/connect client {:db-name db-name}))

;; ## 初始化 schema 与示例数据

^{::clerk/visibility {:code :show :result :hide}}
(def demo-schema
  [{:db/ident       :demo/id
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "示例 id"}
   {:db/ident       :demo/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "示例标题"}
   {:db/ident       :demo/status
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "示例状态"}])

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-schema!
  "如果 schema 未创建则写入."
  [conn]
  (when-not (ffirst (d/q '[:find ?e
                           :where
                           [?e :db/ident :demo/id]]
                         (d/db conn)))
    (d/transact conn {:tx-data demo-schema})))

^{::clerk/visibility {:code :show :result :hide}}
(defn ensure-sample!
  "如果示例数据不存在则写入."
  [conn]
  (when-not (ffirst (d/q '[:find ?e
                           :where
                           [?e :demo/id 1]]
                         (d/db conn)))
    (d/transact conn {:tx-data [{:demo/id 1
                                 :demo/title "本地持久化示例"
                                 :demo/status "created"}]})))

^{::clerk/visibility {:code :show :result :hide}}
(ensure-schema! conn)

^{::clerk/visibility {:code :show :result :hide}}
(ensure-sample! conn)

^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db conn) '[:demo/id :demo/title :demo/status] [:demo/id 1])

;; ## 模拟应用重启

^{::clerk/visibility {:code :show :result :hide}}
(def fresh-client
  (d/client {:server-type :datomic-local
             :storage-dir storage-dir
             :system system}))

^{::clerk/visibility {:code :show :result :hide}}
(def fresh-conn (d/connect fresh-client {:db-name db-name}))

^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db fresh-conn) '[:demo/id :demo/title :demo/status] [:demo/id 1])

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 小结

这个示例展示了 Datomic Local 的本地持久化特性:
即使重新创建 client 与 connection, 数据仍然保存在磁盘目录中。
")
