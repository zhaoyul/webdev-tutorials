# core.logic 笔记目录

本专题围绕 `org.clojure/core.logic` 展示逻辑编程在 Clojure 中的典型用法.
当前结构分成两层:
- 第一层是对 wiki 中 Beginner / Intermediate / Advanced 学习路径的补充.
- 第二层是更贴近本仓库教学风格的专题式 notebook.

## 目录

### 1. Wiki 学习路径补充
- [beginner.clj](beginner.clj)
  - 对应 Primer 风格的基础概念
  - `goal`, `run*`, `fresh`
  - `conso`, `resto`
  - 约束式思维建立
- [intermediate.clj](intermediate.clj)
  - `defne`
  - `matche`
  - 递归 relation
  - pattern matching 驱动的 relation 设计
- [advanced.clj](advanced.clj)
  - `core.logic.fd`
  - 有限域约束
  - 排班 / 组合搜索类问题
  - 进一步扩展方向

### 2. 本仓库专题式 notebook
- [intro.clj](intro.clj)
  - `run` / `run*`
  - 逻辑变量与统一
  - `fresh`
  - `conde`
  - 关系的双向性

### 3. 关系建模
- [family_relations.clj](family_relations.clj)
  - 父母, 祖辈, 兄弟姐妹
  - 递归关系
  - 正向查询与反向查询
  - 从事实表构造可复用 relation

### 4. 应用场景
- [applications.clj](applications.clj)
  - 权限继承与规则推导
  - 组合式人员匹配
  - 有限域约束求解

### 5. 使用技巧
- [techniques.clj](techniques.clj)
  - 保持 relation 小而可组合
  - 控制搜索空间
  - 优先写可逆关系
  - 先关系后宿主语言

### 6. miniKanren 扩展场景
- [mini_kanren_extensions.clj](mini_kanren_extensions.clj)
  - 自动生成 `(I love you)` 的 Clojure 程序
  - 自动生成 Clojure quine
  - "找一个 Clojure 程序, 使它把 X 变成 Y"

## 支撑模块
- [common.clj](common.clj) - 公共事实表与展示辅助函数

## 阅读建议

如果你准备按难度阅读, 建议顺序是:

1. `beginner.clj`
2. `intermediate.clj`
3. `advanced.clj`
4. `mini_kanren_extensions.clj`
5. `intro.clj` / `family_relations.clj` / `applications.clj` / `techniques.clj`

如果你已经熟悉 Datomic 里的 Datalog 查询, 可以把本专题看作"把查询语言再向前走一步":
不仅能描述"查什么", 还可以描述"怎样生成满足约束的数据".
