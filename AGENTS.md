# Project Context: web-tutorial

1. 项目中代码注释, 文档, 都使用中文.
2. 标点是用英文标点.
3. notebook的内容使用中文.
4. 如果 Clojure-mcp 成功启动, 则优先使用 Clojure-mcp 的工具进行clojure/clojurescript代码读写.


## Project Overview
- **Name**: web-tutorial
- **Description**: A Clojure web tutorial project
- **Version**: 0.1.0-SNAPSHOT
- **Main Namespace**: rc.web-tutorial
- **License**: Eclipse Public License v1.0

## Project Structure
```
web-tutorial/
├── .dir-locals.el
├── .gitignore
├── build.clj
├── CHANGELOG.md
├── deps.edn
├── LICENSE
├── README.md
├── .cpcache/...
├── .gemini/
├── .git/...
├── .qwen/
├── doc/
│   └── intro.md
├── resources/
│   └── .keep
├── src/
│   └── rc/
│       └── web_tutorial.clj
├── target/...
└── test/
```

## Dependencies
- `org.clojure/clojure {:mvn/version "1.12.3"}`

## Aliases
- `:run-m` - Run via main opts
- `:run-x` - Run via exec fn
- `:build` - Build utilities using tools.build
- `:dev` - Development dependencies
- `:test` - Testing dependencies and paths
- `:nrepl` - nREPL server
- `:mcp` - MCP server for editor integration

## Main Functions
- `greet` - Callable entry point that prints a greeting
- `-main` - Main function that accepts command line arguments

## Build Commands
- Run tests: `clojure -T:build test`
- CI pipeline: `clojure -T:build ci`
- Run application: `clojure -X:run-x` or `clojure -M:run-m`
- Create JAR: `clojure -T:build ci`

## Source Files
- `src/rc/web_tutorial.clj` - Main application namespace with greet and -main functions
- `build.clj` - Build script using tools.build
- `deps.edn` - Project dependencies and aliases
- `README.md` - Project documentation
- Clerk Notebooks: `notebooks/` 目录包含教程示例; HugSQL 演示笔记 `notebooks/hugsql/demo.clj`

## Development Environment
- Clojure 1.12.3
- Tools.build for building
- Test runner for tests
- MCP server for tool integration

## 系统架构
- 应用入口: `src/rc/web_tutorial.clj` 中 `greet` 负责输出问候, `-main` 从命令行接收名字后调用 `greet`.
- Web 演示: `src/rc/web_tutorial/server.clj` 以 Jetty 形式启动 `muuntaja-content-negotiation` notebook 提供的 `complete-api-app`, 默认端口 3000, 适合内容协商示例.
- Notebook 组件: `notebooks/` 目录存放 Clerk 笔记本, 包含 core.async flow、内容协商、middleware、Malli 等主题的可执行示例, 通过 `:clerk` alias 运行.
- 构建与测试: `build.clj` 基于 tools.build, `clojure -T:build test|ci` 运行测试/CI; `:test` alias 提供 test runner.
- 本地服务: `:run-m` 和 `:run-x` 以命令行/exec 方式运行入口; `:nrepl` 和 `:mcp` 分别启动 REPL 与 MCP server 支撑开发工具链.
