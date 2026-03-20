^{::clerk/visibility {:code :hide}}
(ns core-logic.common
  "core.logic 示例公共数据与辅助函数."
  (:require [nextjournal.clerk :as clerk]))

(def family-parent-pairs
  [[:祖母 :母亲]
   [:祖父 :母亲]
   [:母亲 :小明]
   [:父亲 :小明]
   [:母亲 :小美]
   [:父亲 :小美]
   [:小美 :豆豆]])

(def male-members
  [:祖父 :父亲 :小明 :豆豆])

(def female-members
  [:祖母 :母亲 :小美])

(def role-inherits
  [[:admin :editor]
   [:editor :writer]
   [:writer :reader]])

(def role-permissions
  [[:reader :read]
   [:writer :comment]
   [:writer :draft]
   [:editor :publish]
   [:admin :manage-users]])

(def user-roles
  [[:小李 :writer]
   [:小王 :editor]
   [:管理员 :admin]])

(def member-skills
  [[:小林 :clojure]
   [:小林 :sql]
   [:小周 :ui]
   [:小周 :clojurescript]
   [:小陈 :qa]
   [:小陈 :sql]
   [:小赵 :clojure]
   [:小赵 :ui]
   [:小赵 :ops]])

(def member-shifts
  [[:小林 :thu]
   [:小林 :fri]
   [:小周 :thu]
   [:小陈 :fri]
   [:小赵 :thu]])

(defn fact-table
  "以 Clerk table 展示事实表."
  [head rows]
  (clerk/table {:head head
                :rows rows}))
