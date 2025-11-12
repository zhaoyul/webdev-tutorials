# Project Context: web-tutorial

1. 项目中代码注释, 文档, 都使用中文.
2. 标点是用英文标点.


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

## Development Environment
- Clojure 1.12.3
- Tools.build for building
- Test runner for tests
- MCP server for tool integration
