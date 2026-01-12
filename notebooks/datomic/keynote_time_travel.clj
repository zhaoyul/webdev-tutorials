^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.keynote_time_travel
  "Datomic 时间旅行 Keynote: 三段式演示."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
# 时间旅行 Keynote: 数据库的时间机器

目标受众: 熟悉 SQL 的开发者.
展示重点: 不可变事实带来的时间旅行能力.
")

^{::clerk/visibility {:code :show :result :hide}}
(def conn (c/setup-demo-db! "keynote-time-travel"))

^{::clerk/visibility {:code :show :result :hide}}
(def keynote-user "时间旅人")

^{::clerk/visibility {:code :show :result :hide}}
(def keynote-t0
  (:db-after
   (d/transact conn {:tx-data [{:user/name keynote-user
                                :user/email "traveler@example.com"
                                :user/age 30}]})))

^{::clerk/visibility {:code :show :result :hide}}
(Thread/sleep 100)

^{::clerk/visibility {:code :show :result :hide}}
(def keynote-t1
  (:db-after
   (d/transact conn {:tx-data [{:user/name keynote-user
                                :user/age 66}]})))

^{::clerk/visibility {:code :show :result :hide}}
(Thread/sleep 100)

^{::clerk/visibility {:code :show :result :hide}}
(def keynote-t2
  (:db-after
   (d/transact conn {:tx-data [{:user/name keynote-user
                                :user/age 31}]})))

^{::clerk/visibility {:code :show :result :hide}}
(Thread/sleep 100)

^{::clerk/visibility {:code :show :result :hide}}
(def keynote-t3
  (:db-after
   (d/transact conn {:tx-data [[:db/retract [:user/name keynote-user]
                                :user/email "traveler@example.com"]]})))

^{::clerk/visibility {:code :show :result :show}}
{:t0 (:t keynote-t0)
 :t1 (:t keynote-t1)
 :t2 (:t keynote-t2)
 :t3 (:t keynote-t3)}

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 1. 消失的数据还原")

^{::clerk/visibility {:code :show :result :show}}
{:current (d/pull (d/db conn)
                  '[:user/name :user/email :user/age]
                  [:user/name keynote-user])
 :as-of-before-delete (d/pull (d/as-of (d/db conn) (:t keynote-t2))
                              '[:user/name :user/email :user/age]
                              [:user/name keynote-user])}

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 2. 时空穿梭式 Debug")

^{::clerk/visibility {:code :show :result :show}}
(let [current (d/db conn)
      bug-db (d/as-of current (:t keynote-t1))]
  {:current-age (ffirst (d/q '[:find ?age
                               :in $ ?name
                               :where
                               [?e :user/name ?name]
                               [?e :user/age ?age]]
                             current keynote-user))
   :bug-time-age (ffirst (d/q '[:find ?age
                                :in $ ?name
                                :where
                                [?e :user/name ?name]
                                [?e :user/age ?age]]
                              bug-db keynote-user))})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 3. 审计追踪与变更解释")

^{::clerk/visibility {:code :show :result :show}}
(def keynote-history
  (d/q '[:find ?attr ?val ?inst ?added
         :in $ ?name
         :where
         [?e :user/name ?name]
         [?e ?a ?val ?tx ?added]
         [?a :db/ident ?attr]
         [?tx :db/txInstant ?inst]]
       (d/history (d/db conn))
       keynote-user))

^{::clerk/visibility {:code :show :result :show}}
(clerk/table
 {:head ["属性" "值" "时间" "是否添加"]
  :rows (sort-by #(nth % 2) keynote-history)})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 小结

这组演示强调 Datomic 的时间旅行能力, 让数据的过去可追溯, 让调试更直接, 让审计更自然。
")
