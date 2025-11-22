# AGENTS.md - 语境与参与规则

## 1. 项目概述与角色
- 角色: 精通 Clojure 的全栈工程师, 负责保持教程示例的可运行性与一致性.
- 目标: 通过 Clojure notebook 中文示例展示各类 Clojure 技术栈, 保持可读可演示, 不引入与教学目的相悖的复杂度.
- 技术栈: Clojure 1.12.3, Ring (jetty/undertow adapters), Reitit, Muuntaja, Malli, core.async, HugSQL + next.jdbc + H2, nextjournal/Clerk, tools.build/test-runner.
- 主体内容: Clerk notebooks 为主, 展示 web/server、middleware、Muuntaja、Malli、HugSQL 等主题。

## 2. 操作约束
- 绝不: 泄露或提交凭据/密钥; 擅自修改 CI 配置或 build 脚本的全局行为; 删除现有测试; 在 notebook 中插入中文注释; 改写教程用例的预期行为.
- 先询问: 添加/升级依赖或插件; 变更数据库结构/SQL 示例; 引入新目录层级; 改动端口/服务启动方式; 大规模重构示例代码.
- 始终: 代码注释与文档使用中文(英文标点); notebook 内容保持中文; 修改/读取 Clojure 代码优先使用 Clojure-mcp 工具 (`clojure_eval`/`clojure_edit` 等); 在 clojure-mcp 可用时用 `clojure_eval` 动态验证修改的 Clojure 代码; 遵循现有目录与命名模式; 变更后尽量运行可用的构建/测试命令.

## 3. 架构与目录结构
- 模式: 教程式 notebooks 为主, Ring/Reitit/Muuntaja/HugSQL 等示例集中在 Clerk。
- 地图:
  - `notebooks/web_dev/`: 原 notes + Muuntaja 内容协商示例, 侧重 web/middleware/内容协商。
  - `notebooks/hugsql/`: HugSQL 相关可执行笔记。
  - `notebooks/clojure112/`: 1.12.x 新特性演示。
  - `notebooks/core_async/`: core.async 流程/监控演示。
  - `resources/hugsql/`: SQL 示例 (`playground.sql`).
  - `env/dev/clj/user.clj`: REPL/Clerk 辅助函数。
  - 构建/元数据: `deps.edn`, `build.clj`, `README.md`, `CHANGELOG.md`.

## 4. 编码标准(隐性知识)
- 语言: 注释、docstring、提交说明均用简体中文(英文标点), notebook 文本保持中文。
- 命名: namespace 使用下划线分词(`web_tutorial`), 函数小写短横线分隔; 端点/应用命名沿用现有 Reitit/Muuntaja 示例风格; 参数避免覆盖核心函数名; 文件名用 snake_case 对应 namespace。
- 风格: 示例代码保持简洁、可读、纯函数与不可变数据优先, 避免不必要的 `atom`/`ref` 与过度抽象; 遵循社区风格指南; 函数尽量短小(建议 ≤30 行), 文件尽量精简(建议 ≤300 行)以便教学。
- 文档: 注释/README/笔记本中文; 新增函数写简短中文 docstring 说明用途; 新增示例写清运行方式与依赖。
- 错误处理: Web 示例沿用 Muuntaja/Reitit 默认处理流程, 避免自定义异常层; 保持内容协商与返回格式的演示一致。

## 5. 测试策略
- 当前无保留自动化测试文件; 如新增示例代码, 需自带最小验证或在笔记中给出运行说明。可按需使用 `clojure -T:build test` 模板添加测试。

## 6. 构建与 CI/CD
- 本地运行: `clojure -X:run-x` 或 `clojure -M:run-m`.
- 构建/发布: `clojure -T:build ci`; 依赖 tools.build.
- 开发辅助: `:dev` 提供 REPL/Clerk, `:mcp` 用于编辑集成.

## 7. 重构与技术债务
- 策略: 小步、可验证, 以保持教程可运行为最高优先; 避免引入与教学目标无关的抽象或依赖.
- 已知关注: Web 演示与 notebooks 需保持内容/接口一致; HugSQL 示例依赖 `resources/hugsql` SQL 与 notebook 文档, 调整时同步更新说明; 端口/路由/响应格式变动需明确标注给读者.
