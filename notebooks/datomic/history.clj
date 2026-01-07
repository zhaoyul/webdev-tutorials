^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.history
  "Datomic 历史查询: 时间旅行与审计."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # 历史查询与时间旅行
;;
;; Datomic 的核心优势之一是内置的时间维度。
;; 每个数据变更都被记录, 你可以查询任意历史时刻的数据库状态。

;; ## 准备数据

^{::clerk/visibility {:code :show :result :hide}}
(def conn (c/setup-demo-db! "history-demo"))

;; 记录初始状态的事务时间:
^{::clerk/visibility {:code :show :result :show}}
(def t0 (:db-after (d/transact conn {:tx-data [{:user/name "张三"
                                                 :user/email "zhangsan@example.com"
                                                 :user/age 28}]})))

;; 等待一小段时间后修改数据:
^{::clerk/visibility {:code :show :result :hide}}
(Thread/sleep 100)

^{::clerk/visibility {:code :show :result :show}}
(def t1 (:db-after (d/transact conn {:tx-data [{:user/name "张三"
                                                 :user/age 29}]})))  ;; 年龄从 28 改为 29

^{::clerk/visibility {:code :show :result :hide}}
(Thread/sleep 100)

^{::clerk/visibility {:code :show :result :show}}
(def t2 (:db-after (d/transact conn {:tx-data [{:user/name "张三"
                                                 :user/email "zhangsan-new@example.com"}]})))  ;; 修改邮箱

;; ## 当前数据库快照

;; 默认查询当前状态:
^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db conn) '[*] [:user/name "张三"])

;; ## 时间旅行: as-of

;; `as-of` 让你查询过去某个时刻的数据库状态:

;; 查看初始状态 (t0 时刻):
^{::clerk/visibility {:code :show :result :show}}
(d/pull t0 '[:user/name :user/age :user/email] [:user/name "张三"])

;; 查看第一次修改后 (t1 时刻):
^{::clerk/visibility {:code :show :result :show}}
(d/pull t1 '[:user/name :user/age :user/email] [:user/name "张三"])

;; ## 使用时间点

;; 也可以使用具体的时间戳:
^{::clerk/visibility {:code :show :result :show}}
(let [now (java.util.Date.)
      past (java.util.Date. (- (.getTime now) 60000))  ;; 1分钟前
      db-past (d/as-of (d/db conn) past)]
  {:now-age (d/q '[:find ?age . :where [?e :user/name "张三"] [?e :user/age ?age]]
                 (d/db conn))
   :past-exists? (boolean (d/q '[:find ?e . :where [?e :user/name "张三"]]
                               db-past))})

;; ## 历史数据库: history

;; `history` 返回包含所有历史数据的数据库视图:
^{::clerk/visibility {:code :show :result :show}}
(def hist-db (d/history (d/db conn)))

;; 查看张三的年龄变化历史:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?age ?tx ?added
       :where
       [?e :user/name "张三"]
       [?e :user/age ?age ?tx ?added]]
     hist-db)

;; 结果说明:
;; - `?added` = true 表示添加
;; - `?added` = false 表示撤回 (被新值替代)

;; ## 查看完整变更历史

;; 查看张三所有属性的变更:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?attr ?val ?tx ?added
       :where
       [?e :user/name "张三"]
       [?e ?a ?val ?tx ?added]
       [?a :db/ident ?attr]
       [(not= ?attr :db/ident)]]
     hist-db)

;; ## 事务时间查询

;; 每个事务都有时间戳:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?attr ?val ?inst
       :where
       [?e :user/name "张三"]
       [?e ?a ?val ?tx true]
       [?a :db/ident ?attr]
       [?tx :db/txInstant ?inst]]
     hist-db)

;; ## since 数据库

;; `since` 返回某个时间点之后的变更:
^{::clerk/visibility {:code :show :result :show}}
(let [since-db (d/since (d/db conn) (:t (d/db conn)))]
  (d/q '[:find ?name ?age
         :where
         [?e :user/name ?name]
         [?e :user/age ?age]]
       since-db))

;; ## 实际应用场景

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["场景" "使用的 API" "说明"]
  :rows [["审计日志" "history" "查看谁在何时改了什么"]
         ["数据恢复" "as-of" "查看删除前的数据"]
         ["调试" "as-of" "重现特定时间点的问题"]
         ["报表" "as-of" "生成历史某时刻的快照报表"]
         ["版本对比" "as-of + history" "对比不同版本的数据差异"]]})

;; ## 审计示例: 查看用户修改记录

^{::clerk/visibility {:code :show :result :show}}
(defn user-audit-log
  "获取用户的完整修改日志."
  [db user-lookup]
  (d/q '[:find ?attr ?old-val ?new-val ?inst
         :in $ ?user
         :where
         [?e :user/name ?user]
         [?e ?a ?new-val ?tx true]
         [?a :db/ident ?attr]
         [?tx :db/txInstant ?inst]
         [(get-else $ ?e ?a nil) ?old-val]]
       (d/history db)
       user-lookup))

(user-audit-log (d/db conn) "张三")

;; ## 数据恢复示例

;; 假设要恢复张三的旧邮箱:
^{::clerk/visibility {:code :show :result :show}}
(let [old-email (d/q '[:find ?email .
                       :where
                       [?e :user/name "张三"]
                       [?e :user/email ?email]]
                     t0)]
  {:old-email old-email
   :current-email (d/q '[:find ?email .
                         :where
                         [?e :user/name "张三"]
                         [?e :user/email ?email]]
                       (d/db conn))})

;; 恢复操作只需要一个事务:
^{::clerk/visibility {:code :show :result :show}}
(d/transact conn {:tx-data [{:user/name "张三"
                              :user/email "zhangsan@example.com"}]})

;; 验证恢复:
^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db conn) '[:user/email] [:user/name "张三"])

;; ## 性能考虑

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
### 历史查询性能提示

1. **history 数据库较大** - 包含所有版本的数据, 查询时注意添加足够的过滤条件
2. **使用事务 ID 过滤** - 可以限制查询的时间范围
3. **索引利用** - 和普通查询一样, 将选择性高的条件放前面
4. **考虑数据量** - 对于频繁更新的属性, 历史数据可能非常大
")

;; ## 总结
;;
;; Datomic 的时间旅行能力让你可以:
;; - 查询任意历史时刻的数据状态
;; - 追踪数据的完整变更历史
;; - 轻松实现审计和数据恢复
;; - 无需额外的审计表或变更日志
