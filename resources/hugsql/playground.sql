-- 表管理
-- :name drop-table :!
drop table if exists guests;

-- :name create-table :! :raw
create table if not exists guests (
  id integer auto_increment primary key,
  name varchar(40),
  specialty varchar(80),
  created_at timestamp default current_timestamp
);

-- 插入
-- :name insert-guest :! :n
insert into guests (name, specialty) values (:name, :specialty);

-- :name insert-guests :! :n
insert into guests (name, specialty) values :t*:guests;

-- 插入返回主键 (使用 :insert 触发 getGeneratedKeys)
-- :name insert-guest-returning :insert :raw
insert into guests (name, specialty) values (:name, :specialty);

-- 查询
-- :name all-guests :? :*
select * from guests order by id;

-- :name guest-by-id :? :1
select * from guests where id = :id;

-- :name guests-by-ids-cols :? :*
select :i*:cols from guests where id in (:v*:ids) order by id;

-- 更新/删除
-- :name update-guest :! :n
update guests set specialty = :specialty where id = :id;

-- :name delete-guest :! :n
delete from guests where id = :id;

-- 原生 SQL 参数, 注意调用端需白名单
-- :name order-by-raw :? :*
select * from guests order by :sql:field :sql:dir;

-- Clojure 表达式示例(单行) 可选列
-- :name expr-cols :? :*
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from guests
order by id;

-- 片段定义
-- :snip select-snip
select :i*:cols

-- :snip from-snip
from guests

-- :snip where-snip
where :snip*:cond

-- :snip cond-snip
:sql:conj :i:cond.0 :sql:cond.1 :v:cond.2

-- 组合查询
-- :name snip-query :? :*
:snip:select
:snip:from
--~ (when (:where params) ":snip:where")
