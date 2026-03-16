# core.logic 笔记目录

本专题围绕 `org.clojure/core.logic` 展示逻辑编程在 Clojure 中的典型用法.
内容按"概念建立 -> 关系建模 -> 应用场景 -> 使用技巧"展开, 适合从零开始逐步阅读.

## 目录

### 1. 入门
- [intro.clj](intro.clj)
  - `run` / `run*`
  - 逻辑变量与统一
  - `fresh`
  - `conde`
  - 关系的双向性

### 2. 关系建模
- [family_relations.clj](family_relations.clj)
  - 父母, 祖辈, 兄弟姐妹
  - 递归关系
  - 正向查询与反向查询
  - 从事实表构造可复用 relation

### 3. 应用场景
- [applications.clj](applications.clj)
  - 权限继承与规则推导
  - 组合式人员匹配
  - 有限域约束求解

### 4. 使用技巧
- [techniques.clj](techniques.clj)
  - 保持 relation 小而可组合
  - 控制搜索空间
  - 优先写可逆关系
  - 先关系后宿主语言

## 支撑模块
- [common.clj](common.clj) - 公共事实表与展示辅助函数

## 阅读建议

如果你已经熟悉 Datomic 里的 Datalog 查询, 可以把本专题看作"把查询语言再向前走一步":
不仅能描述"查什么", 还可以描述"怎样生成满足约束的数据".
