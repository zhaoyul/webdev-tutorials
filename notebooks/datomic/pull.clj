^{:nextjournal.clerk/visibility {:code :hide}}
(ns datomic.pull
  "Datomic Pull API: 声明式数据提取."
  (:require [nextjournal.clerk :as clerk]
            [datomic.client.api :as d]
            [datomic.common :as c]))

;; # Pull API
;;
;; Pull API 是 Datomic 的声明式数据提取机制。
;; 它允许你用简洁的模式声明需要哪些属性, 包括嵌套的关联数据。

;; ## 准备数据

^{::clerk/visibility {:code :show :result :hide}}
(def conn (c/setup-demo-db! "pull-demo"))

^{::clerk/visibility {:code :show :result :hide}}
(c/seed-sample-data! conn)

;; 添加文章和嵌套地址:
^{::clerk/visibility {:code :show :result :hide}}
(d/transact conn
  {:tx-data [;; 添加地址 schema
             {:db/ident       :user/address
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/isComponent true}
             {:db/ident       :address/city
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :address/street
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             ;; 更新用户数据
             {:user/name    "张三"
              :user/address {:address/city   "北京"
                             :address/street "中关村大街1号"}}
             ;; 添加文章
             {:article/title       "深入 Datomic"
              :article/content     "这篇文章介绍 Datomic 的高级特性..."
              :article/author      [:user/name "张三"]
              :article/tags        #{"Datomic" "进阶"}
              :article/published-at (java.util.Date.)}]})

^{::clerk/visibility {:code :show :result :show}}
(def db (d/db conn))

;; ## 基础 Pull

;; ### 获取所有属性 (`*`)
^{::clerk/visibility {:code :show :result :show}}
(d/pull db '[*] [:user/name "张三"])

;; ### 选择特定属性
^{::clerk/visibility {:code :show :result :show}}
(d/pull db '[:user/name :user/email :user/age] [:user/name "张三"])

;; ### 属性不存在时返回 nil
^{::clerk/visibility {:code :show :result :show}}
(d/pull db '[:user/name :user/phone] [:user/name "张三"])

;; ## 默认值

;; 为可能缺失的属性提供默认值:
^{::clerk/visibility {:code :show :result :show}}
(d/pull db
  '[:user/name
    (:user/phone :default "未提供")]
  [:user/name "张三"])

;; ## 嵌套 Pull (关联)

;; ### 单值引用
^{::clerk/visibility {:code :show :result :show}}
(d/pull db
  '[:user/name
    {:user/address [:address/city :address/street]}]
  [:user/name "张三"])

;; ### 多值引用
^{::clerk/visibility {:code :show :result :show}}
(d/pull db
  '[:user/name
    {:user/roles [:role/name]}]
  [:user/name "张三"])

;; ## 反向引用

;; 使用 `_` 前缀获取引用当前实体的其他实体:
^{::clerk/visibility {:code :show :result :show}}
(d/pull db
  '[:user/name
    {:article/_author [:article/title :article/tags]}]
  [:user/name "张三"])

;; ## 递归 Pull

;; ### 无限递归 (`...`)
;; 用于自引用的数据结构, 如树形结构:

^{::clerk/visibility {:code :show :result :hide}}
(d/transact conn
  {:tx-data [{:db/ident       :category/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident       :category/parent
              :db/valueType   :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/id           "cat-tech"
              :category/name   "技术"}
             {:db/id           "cat-prog"
              :category/name   "编程"
              :category/parent "cat-tech"}
             {:db/id           "cat-clj"
              :category/name   "Clojure"
              :category/parent "cat-prog"}]})

;; 获取完整的分类层级:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find (pull ?e [:category/name {:category/parent ...}])
       :where [?e :category/name "Clojure"]]
     (d/db conn))

;; ### 限制递归深度 (数字)
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find (pull ?e [:category/name {:category/parent 1}])
       :where [?e :category/name "Clojure"]]
     (d/db conn))

;; ## 通配符扩展

;; ### 获取所有属性并展开引用
;; 使用通配符 `*` 获取所有属性, 并指定引用属性的展开模式:
^{::clerk/visibility {:code :show :result :show}}
(d/pull (d/db conn)
  '[* {:user/roles [:role/name]}]
  [:user/name "张三"])

;; ## 批量 Pull (pull-many)

;; 对多个实体执行相同的 pull 模式:
^{::clerk/visibility {:code :show :result :show}}
(let [user-ids (d/q '[:find [?e ...]
                      :where [?e :user/name]]
                    (d/db conn))]
  (d/pull-many (d/db conn)
    '[:user/name :user/age]
    user-ids))

;; ## 在查询中使用 Pull

;; 直接在 :find 子句中使用 pull:
^{::clerk/visibility {:code :show :result :show}}
(d/q '[:find (pull ?e [:user/name :user/age {:user/roles [:role/name]}])
       :where
       [?e :user/age ?age]
       [(> ?age 25)]]
     (d/db conn))

;; ## Pull 模式对照表

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["模式" "说明" "示例"]
  :rows [["*" "所有属性" "[:find (pull ?e [*]) ...]"]
         [":attr" "单个属性" "[:user/name]"]
         ["{:ref [...]}}" "嵌套引用" "{:user/address [:address/city]}"]
         [":ns/_attr" "反向引用" ":article/_author"]
         ["..." "无限递归" "{:category/parent ...}"]
         ["n" "递归 n 层" "{:category/parent 2}"]
         ["(:attr :default v)" "带默认值" "(:user/phone :default nil)"]]})

;; ## 与 Entity API 对比

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["特性" "Pull API" "Entity API"]
  :rows [["声明式" "✅ 预声明需要的数据" "❌ 命令式导航"]
         ["惰性加载" "❌ 一次获取" "✅ 按需加载"]
         ["嵌套数据" "✅ 模式控制" "✅ 导航获取"]
         ["序列化友好" "✅ 返回纯数据" "❌ 返回延迟实体"]
         ["适用场景" "API 响应, 数据导出" "交互式探索"]]})

;; ## 下一步
;;
;; - [history.clj](history.clj) - 时间旅行与历史查询
