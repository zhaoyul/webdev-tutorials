^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.queries
  "Datomic Datalog 查询: 从基础到进阶."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # Datalog 查询
;;
;; Datalog 是一种声明式查询语言, 源自逻辑编程。
;; 它比 SQL 更灵活, 特别适合图状数据的查询。

;; ## 准备数据

^{::clerk/visibility {:code :show :result :hide}}
(def conn (c/setup-demo-db! "queries-demo"))

^{::clerk/visibility {:code :show :result :hide}}
(c/seed-sample-data! conn)

;; 添加一些文章:
^{::clerk/visibility {:code :show :result :hide}}
(d/transact conn
  {:tx-data [{:article/title       "Clojure 入门"
              :article/content     "Clojure 是一门优雅的函数式语言..."
              :article/author      [:user/name "张三"]
              :article/tags        #{"Clojure" "入门" "函数式"}
              :article/published-at (java.util.Date.)}
             {:article/title       "Datomic 实战"
              :article/content     "深入理解 Datomic 的设计理念..."
              :article/author      [:user/name "李四"]
              :article/tags        #{"Datomic" "数据库"}
              :article/published-at (java.util.Date.)}
             {:article/title       "函数式编程思想"
              :article/content     "为什么函数式编程很重要..."
              :article/author      [:user/name "张三"]
              :article/tags        #{"函数式" "编程范式"}}]})

^{::clerk/visibility {:code :show :result :show}}
(def db (d/db conn))

;; ## 查询结构

;; Datalog 查询的基本结构:

^{::clerk/visibility {:code :hide :result :show}}
(clerk/code "[:find  <返回的变量>
 :in    <输入参数>
 :where <匹配条件>]")

;; ## 基础查询

;; ### 查找所有用户名:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name
       :where
       [?e :user/name ?name]]
     db)

;; ### 返回多个属性:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?email
       :where
       [?e :user/name ?name]
       [?e :user/email ?email]]
     db)

;; ## 返回格式

;; ### 默认: 集合的集合
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name :where [_ :user/name ?name]] db)

;; ### 单个标量值
;; Datomic Client 返回关系结果, 可用 ffirst 取出单个值.
^{::clerk/visibility {:code :show :result :show}}
(ffirst (d/q '[:find ?name
               :where
               [?e :user/name ?name]
               [?e :user/age 28]]
             db))

;; ### 向量的集合
;; Datomic Client 只支持关系返回, 这里用 map 组装成向量.
^{::clerk/visibility {:code :show :result :show}}
(vec (map first
          (d/q '[:find ?name
                 :where [_ :user/name ?name]]
               db)))

;; ### 单个元组
;; 关系结果的第一行就是单个元组.
^{::clerk/visibility {:code :show :result :show}}
(first
  (d/q '[:find ?name ?age
         :where
         [?e :user/name ?name]
         [?e :user/age ?age]
         [(= ?name "张三")]]
       db))

;; ## 参数化查询 (:in)

;; ### 单值参数:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?age
       :in $ ?min-age
       :where
       [?e :user/name ?name]
       [?e :user/age ?age]
       [(>= ?age ?min-age)]]
     db 30)

;; ### 集合参数:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name
       :in $ [?target-name ...]
       :where
       [?e :user/name ?name]
       [(= ?name ?target-name)]]
     db ["张三" "李四"])

;; ## 谓词函数

;; ### 比较运算:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?age
       :where
       [?e :user/name ?name]
       [?e :user/age ?age]
       [(> ?age 26)]
       [(< ?age 35)]]
     db)

;; ### Clojure 函数:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name
       :where
       [?e :user/name ?name]
       [(clojure.string/starts-with? ?name "张")]]
     db)

;; ## 聚合函数

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["函数" "说明"]
  :rows [["(count ?x)" "计数"]
         ["(sum ?x)" "求和"]
         ["(avg ?x)" "平均值"]
         ["(min ?x)" "最小值"]
         ["(max ?x)" "最大值"]
         ["(distinct ?x)" "去重"]
         ["(sample n ?x)" "随机采样"]]})

;; ### 示例:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find (count ?e) (avg ?age) (min ?age) (max ?age)
       :where
       [?e :user/age ?age]]
     db)

;; ## 关联查询 (Join)

;; Datalog 的变量绑定自然实现 join:

;; 查找作者及其文章:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?author-name ?title
       :where
       [?article :article/title ?title]
       [?article :article/author ?author]
       [?author :user/name ?author-name]]
     db)

;; ## 反向引用

;; 使用 `_` 前缀进行反向查询:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?user-name ?title
       :where
       [?user :user/name ?user-name]
       [?article :article/author ?user]
       [?article :article/title ?title]]
     db)

;; ## 规则 (Rules)

;; 规则允许复用查询逻辑:

^{::clerk/visibility {:code :show :result :hide}}
(def rules
  '[[(active-adult ?e ?name)
     [?e :user/name ?name]
     [?e :user/age ?age]
     [(>= ?age 18)]]
    [(has-article ?user ?title)
     [?article :article/author ?user]
     [?article :article/title ?title]]])

^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?title
       :in $ %
       :where
       (active-adult ?user ?name)
       (has-article ?user ?title)]
     db rules)

;; ## 否定查询 (not)

;; 查找没有文章的用户:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name
       :where
       [?e :user/name ?name]
       (not [_ :article/author ?e])]
     db)

;; ## 或查询 (or)

;; 查找年龄小于 26 或大于 30 的用户:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find ?name ?age
       :where
       [?e :user/name ?name]
       [?e :user/age ?age]
       (or [(< ?age 26)]
           [(> ?age 30)])]
     db)

;; ## 性能提示

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["技巧" "说明"]
  :rows [["使用常量" "将已知值放在 where 子句中可减少扫描范围"]
         ["顺序优化" "将选择性高的子句放在前面"]
         ["避免笛卡尔积" "确保 where 子句中的变量有连接"]
         ["使用索引" "利用 :db.unique 属性的索引"]]})

;; ## 下一步
;;
;; - [pull.clj](pull.clj) - Pull API 树形数据提取
;; - [history.clj](history.clj) - 历史查询与时间旅行
