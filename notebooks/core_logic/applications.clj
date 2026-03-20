^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core-logic.applications
  "core.logic 应用场景: 规则推导, 组合搜索与约束求解."
  (:require [clojure.core.logic :refer [!= == conde fresh membero run run*]]
            [clojure.core.logic.fd :as fd]
            [core-logic.common :as c]
            [nextjournal.clerk :as clerk]))

;; # core.logic 的典型应用场景
;;
;; 一个务实的判断标准是:
;; 当你的问题更像"找满足规则的答案", 而不是"按固定步骤计算结果"时, core.logic 往往会很好用.

;; ## 1. 权限继承与规则推导

^{::clerk/visibility {:code :show :result :hide}}
(defn inherits-roleo
  "角色继承关系."
  [role parent-role]
  (membero [role parent-role] c/role-inherits))

^{::clerk/visibility {:code :show :result :hide}}
(defn grants-directo
  "角色直接授予的权限."
  [role permission]
  (membero [role permission] c/role-permissions))

^{::clerk/visibility {:code :show :result :hide}}
(defn has-roleo
  "用户当前角色."
  [user role]
  (membero [user role] c/user-roles))

^{::clerk/visibility {:code :show :result :hide}}
(defn grants-roleo
  "角色最终可获得的权限, 支持递归继承."
  [role permission]
  (conde
    [(grants-directo role permission)]
    [(fresh [parent-role]
       (inherits-roleo role parent-role)
       (grants-roleo parent-role permission))]))

^{::clerk/visibility {:code :show :result :hide}}
(defn user-cano
  "用户是否具备某个权限."
  [user permission]
  (fresh [role]
    (has-roleo user role)
    (grants-roleo role permission)))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (user-cano :小王 q))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (user-cano q :publish))

;; ## 2. 组合式人员匹配
;;
;; 这里不写"先查后过滤"的命令式流程, 而是直接描述候选人的条件组合.

^{::clerk/visibility {:code :show :result :hide}}
(defn has-skillo
  "成员拥有某项技能."
  [member skill]
  (membero [member skill] c/member-skills))

^{::clerk/visibility {:code :show :result :hide}}
(defn availableo
  "成员在指定日期可排班."
  [member day]
  (membero [member day] c/member-shifts))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [backend frontend]
    (has-skillo backend :clojure)
    (has-skillo frontend :ui)
    (availableo backend :thu)
    (availableo frontend :thu)
    (!= backend frontend)
    (== q {:backend backend
           :frontend frontend})))

;; 也可以反过来问: 某个成员最适合填哪类空缺?
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [member]
    (== member :小赵)
    (conde
      [(has-skillo member :clojure)
       (== q :backend)]
      [(has-skillo member :ui)
       (== q :frontend)]
      [(has-skillo member :ops)
       (== q :support)])))

;; ## 3. 有限域约束求解
;;
;; `core.logic.fd` 适合离散排班, 顺序安排, 数字谜题等场景.

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [api db qa]
    (== q {:api api
           :db db
           :qa qa})
    (fd/in api db qa (fd/interval 1 3))
    (fd/distinct [api db qa])
    (fd/< api qa)
    (fd/!= db 1)))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["场景" "对应 relation"]
  :rows [["角色与权限" "grants-roleo / user-cano"]
         ["技能与排班" "has-skillo / availableo"]
         ["离散约束" "fd/in / fd/distinct / fd/<"]]})

;; ## 4. 什么时候值得优先考虑 core.logic

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["问题特征" "示例"]
  :rows [["规则会演化" "权限体系, 审批流, 资格判断"]
         ["答案不止一个" "候选人匹配, 备选路径, 配置组合"]
         ["约束比步骤更重要" "排班, 资源分配, 数字谜题"]
         ["希望反向求解" "已知结果, 反推输入或中间条件"]]})
