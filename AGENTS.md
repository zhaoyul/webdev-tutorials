# Project Context: web-tutorial

1. 项目中代码注释, 文档全部使用中文, 标点为英文标点.
2. notebook 内容使用中文.
3. 修改/读取 Clojure 代码时, 优先使用 Clojure-mcp 工具(`clojure_eval`/`clojure_edit` 等); 仅在不可行时再用 shell/file 操作.


## Project Overview
- **Name**: web-tutorial
- **Description**: A Clojure web tutorial project
- **Version**: 0.1.0-SNAPSHOT
- **Main Namespace**: rc.web-tutorial
- **License**: Eclipse Public License v1.0

## Project Structure
- 根目录: `build.clj`, `deps.edn`, `README.md`, `CHANGELOG.md`
- 代码: `src/rc/web_tutorial.clj` (greet/-main), `src/rc/web_tutorial/server.clj` (Jetty 内容协商示例)
- 资源: `resources/hugsql/playground.sql` (HugSQL 示例 SQL)
- Notebook:
  - `notebooks/notes/`：starting_web_server, starting_ring_server, adding_simple_middleware, demonstrating_middleware_functionality, adding_swagger_support, malli_spec_usage
  - `notebooks/muuntaja_content_negotiation.clj`, `notebooks/core_async_flow.clj`, `notebooks/clojure112_features.clj`
  - `notebooks/hugsql/`：common, intro, install, getting_sql, getting_clj, usage_fns, crud, transactions, composability, advanced_usage, deep_dive, adapters, faq
- 辅助: `env/dev/clj/user.clj` (REPL/Clerk 辅助), `doc/intro.md`, `test/rc/web_tutorial_test.clj`

## Dependencies (关键)
- 运行: `org.clojure/clojure 1.12.3`, `ring-core`, `ring-jetty-adapter`, `luminus/ring-undertow-adapter`, `reitit`, `muuntaja`, `malli`, `core.async`, `buddy-auth`, `hato`, `snakeyaml`
- Notebook/工具: `nextjournal/clerk`
- HugSQL/DB: `hugsql`, `hugsql-adapter-next-jdbc`, `next.jdbc`, `h2`
- Dev/Test: `tools.namespace`, `test.check`, `tools.build`, `test-runner`

## Aliases
- `:run-m` - Run via main opts
- `:run-x` - Run via exec fn
- `:build` - Build utilities using tools.build
- `:dev` - Development dependencies
- `:test` - Testing dependencies and paths
- `:nrepl` - nREPL server
- `:mcp` - MCP server for editor integration

## Main Functions
- `greet` / `-main`: 入口问候
- `server/start-server`: 以 Jetty 启动内容协商示例 (3000)

## Build Commands
- Run tests: `clojure -T:build test`
- CI pipeline: `clojure -T:build ci`
- Run application: `clojure -X:run-x` or `clojure -M:run-m`
- Create JAR: `clojure -T:build ci`

## Development Environment / MCP
- Clojure 1.12.3, tools.build, test runner
- 统一使用 Clojure-mcp 读写 Clojure/ClojureScript: 优先 `clojure_eval` 执行、`clojure_edit`/`clojure_edit_replace_sexp` 修改; 仅在 mcp 不可用时再用 shell/file 操作.

## 系统架构
- 应用入口: `src/rc/web_tutorial.clj` 中 `greet` 负责输出问候, `-main` 从命令行接收名字后调用 `greet`.
- Web 演示: `src/rc/web_tutorial/server.clj` 以 Jetty 形式启动 `muuntaja-content-negotiation` notebook 提供的 `complete-api-app`, 默认端口 3000, 适合内容协商示例.
- Notebook 组件: `notebooks/` 目录存放 Clerk 笔记本, 包含 core.async flow, 内容协商, middleware, Malli 等主题的可执行示例, 通过 `:clerk` alias 运行.
- 构建与测试: `build.clj` 基于 tools.build, `clojure -T:build test|ci` 运行测试/CI; `:test` alias 提供 test runner.
- 本地服务: `:run-m` 和 `:run-x` 以命令行/exec 方式运行入口; `:nrepl` 和 `:mcp` 分别启动 REPL 与 MCP server 支撑开发工具链.
