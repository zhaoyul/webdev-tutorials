^{:nextjournal.clerk/visibility {:code :hide}}
(ns hugsql.faq
  (:require [nextjournal.clerk :as clerk]))

;; # 7. FAQ 摘要

;; - 与 Yesql 对比：HugSQL 有适配器、更多参数类型、命名参数、显式命令/结果配置、表达式与 Snippet。

;; - SQL 注入防护：值参数默认绑定；标识符需开启 `:quoting`；原生 SQL 必须白名单；自定义参数自行转义；表达式中返回的字符串继续按 HugSQL 参数解析。

;; - DSL: 可与 HoneySQL 输出的 sqlvec 配合 Snippet 参数使用。

;; - 兼容性: HugSQL 对“奇葩 SQL”采用保留策略，若遇解析问题应提 issue。

;; # 7. FAQ 摘要
;; - 与 Yesql 对比：HugSQL 有适配器、更多参数类型、命名参数、显式命令/结果配置、表达式与 Snippet。
;; - SQL 注入防护：值参数默认绑定；标识符需开启 `:quoting`；原生 SQL 必须白名单；自定义参数自行转义；表达式中返回的字符串继续按 HugSQL 参数解析。
;; - DSL: 可与 HoneySQL 输出的 sqlvec 配合 Snippet 参数使用。
;; - 兼容性: HugSQL 对“奇葩 SQL”采用保留策略，若遇解析问题应提 issue。
