# webdev-tutorials

围绕 Clojure 教学与演示整理的一组 Clerk notebooks. 仓库重点不是单体应用, 而是可直接运行、可逐页阅读、可在 REPL 中拆解的教程示例. 主题覆盖 Ring/Reitit、Muuntaja、Malli、core.async、HugSQL、Datomic Local、Portal、Emmy、core.logic 等.

当前 `deps.edn` 使用 `org.clojure/clojure` `1.12.4`.

## 仓库定位

- 主要入口是 `notebooks/`, 通过 Clerk 在浏览器中阅读与执行.
- `env/dev/clj/user.clj` 提供开发 REPL 常用函数, 例如启动 Clerk、打开单页 notebook、构建静态页面.
- `resources/hugsql/playground.sql`、`chinook.db` 等文件为教程示例提供数据支撑.
- 仓库当前不是“启动一个完整 Web 应用”的结构, README 中的运行说明以 notebook / REPL 工作流为准.

## 前置条件

建议先准备以下环境:

- JDK 21 或更新版本.
- Clojure CLI (`clojure` 命令可用).
- Node.js 18+ 与 `yarn`, 用于安装 `package.json` 中的前端依赖. 不是每个 notebook 都依赖它, 但 MathBox / 部分 Clerk 前端扩展需要.
- 可访问 Maven 仓库的网络环境.

可选依赖:

- 浏览器环境, 用于查看 Clerk 页面.
- Portal 用户可准备本地桌面或浏览器环境, 便于体验 `notebooks/portal/`.

说明:

- H2、SQLite JDBC、Datomic Local、JeroMQ/ezzmq 等依赖已经在 `deps.edn` 中声明, 一般不需要单独安装系统级数据库或 ZeroMQ.
- `chinook.db` 已随仓库提供, 可直接用于部分 SQLite 示例.

## 安装与依赖准备

首次进入仓库建议先做两步:

```sh
clojure -P
yarn install
```

- `clojure -P` 用于提前下载 JVM 依赖.
- `yarn install` 用于安装 Clerk 前端扩展、MathBox 等相关包. 如果你只阅读不涉及这些前端能力的 notebook, 可以稍后再执行.
- 第一次执行任意 `clojure` 命令时, 可能会下载较多 Maven 包与 git 依赖, 启动时间会明显更长.

## 快速开始

如果你还不确定该运行哪个入口, 先按目标选择:

| 目标 | 推荐入口 | 说明 |
| --- | --- | --- |
| 边读边改 notebook | `clojure -M:dev` | 进入 REPL 后用 `user/start-clerk!` 和 `user/show-notebook` 打开具体页面. |
| 只在浏览器里浏览教程 | `clojure -X:clerk ':browse?' true` | 直接启动 Clerk, 默认监听 `notebooks/` 并打开浏览器. |
| 检查静态发布效果 | `clojure -X:nextjournal/clerk` | 构建静态 notebook 页面, 适合发布前预览. |

### 1. 启动开发 REPL

```sh
clojure -M:dev
```

进入 REPL 后可使用 `user` 命名空间中的辅助函数:

```clojure
(require 'user)
(user/help)
(user/start-clerk!)
(user/show-notebook "notebooks/web_dev/starting_web_server.clj")
```

### 2. 直接启动 Clerk

如果你只是想浏览 notebooks, 可以直接运行:

```sh
clojure -X:clerk ':browse?' true
```

默认端口为 `7777`. 如端口被占用, 可改成:

```sh
clojure -X:clerk ':browse?' false ':port' 8888
```

如果你使用的是 `zsh`, `?` 会参与通配匹配, 所以 `-X` 参数中的关键字建议像上面这样加引号.

### 3. 构建静态页面

```sh
clojure -X:nextjournal/clerk
```

当前配置会构建 `notebooks/*`, 默认索引页指向 `notebooks/hugsql/overview.clj`, 并启用 `:compile-css true` 与 SSR.

## 常用工作流

### REPL + Clerk

适合逐页调试、边看边改:

```clojure
(require 'user)
(user/start-watch!)
(user/start-clerk! {:browse? false})
(user/show-notebook "notebooks/hugsql/intro.clj")
```

### 静态发布预览

适合检查 notebook 在静态构建下的可读性:

```clojure
(require 'user)
(user/publish-notebooks {:paths ["notebooks"]
                         :out-path "public"
                         :cljs-namespaces '[mathbox.sci-extensions]})
```

### 构建与测试入口

```sh
clojure -T:build test
```

`build.clj` 仍保留 `tools.build` 流程, 但它当前指向的主命名空间 `rc.web-tutorial` 不在仓库中. 因此:

- `clojure -T:build test` 可作为“有没有测试目录 / 测试依赖问题”的检查入口.
- `clojure -T:build ci` 目前更像模板式构建脚手架, 不适合作为本仓库教程内容的首选运行方式.

## 教程与示例结构

### Web 与中间件

目录: `notebooks/web_dev/`

- `starting_web_server.clj`: 最简 Jetty / Undertow HTTP 服务.
- `starting_ring_server.clj`: Ring 应用与 handler 结构.
- `adding_simple_middleware.clj`: 自定义中间件与顺序.
- `demonstrating_middleware_functionality.clj`: 中间件链路观测.
- `muuntaja_content_negotiation.clj`: Muuntaja 内容协商.
- `transit_deep_dive.clj`: Transit 格式与读写.
- `malli_spec_usage.clj`: Malli schema 校验.
- `adding_swagger_support.clj`: Reitit + Swagger 文档.
- `ring_middleware_with_portal.clj`: 用 Portal 观察请求处理差异.
- `kit_aero_integrant.clj`: Aero 配置与 Integrant 生命周期.
- `ezzmq_complete_test_scenario.clj`: ezzmq/ZeroMQ 端到端场景.
- `zmq_message_flow_visualization.clj`: ZeroMQ 消息流可视化.

### HugSQL

目录: `notebooks/hugsql/`

- `install.clj`: 安装与依赖背景.
- `intro.clj`: 入门说明.
- `getting_sql.clj`: 从 SQL 文件组织开始.
- `getting_clj.clj`: 生成函数与 Clojure 调用方式.
- `usage_fns.clj`: 常用 API.
- `crud.clj`: 增删改查.
- `transactions.clj`: 事务.
- `composability.clj`: 组合能力与 snippet.
- `advanced_usage.clj`: 进阶技巧.
- `adapters.clj`: 适配器切换.
- `deep_dive.clj`: 深入约定与参数类型.
- `faq.clj`: 常见问题.
- `common.clj`: 公共初始化.
- `resources/hugsql/playground.sql`: 配套 SQL 示例.

### Datomic

目录: `notebooks/datomic/`

- `intro.clj`: Datomic 核心概念.
- `schema.clj`: Schema 与数据建模.
- `transactions.clj`: 事务写入.
- `queries.clj`: Datalog 查询.
- `pull.clj`: Pull API.
- `history.clj`: 历史与时间旅行.
- `local_persistence.clj`: 本地持久化实验.
- `keynote_time_travel.clj`: 时间旅行演示.
- `common.clj`: 公共辅助函数.

### Portal

目录: `notebooks/portal/`

- `overview.clj`: 打开 Portal 与基本配置.
- `tap_flow.clj`: `tap>` 工作流.
- `inspect.clj`: 表格、树、Diff 等 viewer.
- `custom_viewers.clj`: 自定义 viewer.
- `advanced_scenarios.clj`: 进阶调试场景.
- `dashboard_viewer.clj`: 聚合展示.
- `realtime_dashboard.clj`: 定时刷新与流式展示.
- `multi_service_dashboard.clj`: 多服务指标对比.
- `multi_service_realtime.clj`: 多服务实时刷新.

### 其他专题

- `notebooks/clojure112/`: Clojure 1.12 特性演示.
- `notebooks/core_async/`: core.async 与 RxClojure 流式示例.
- `notebooks/core_logic/`: 逻辑编程学习路径与专题 notebook.
- `notebooks/emmy/`: 符号计算、微积分、经典力学.
- `notebooks/advanced_note/`: Reagent、第三方 JS、状态同步等进阶 Clerk 用法.
- `notebooks/coffi/`: 字节码 / SQLite 相关实验性专题.
- `notebooks/plc/`: PLC 与 Ladder 示例.
- `notebooks/mathbox/`: MathBox 相关前端实验.
- `notebooks/notes/`: 较长篇的主题笔记.
- 根目录单页: `notebooks/book.clj`、`notebooks/specter_guide.clj`、`notebooks/transducer_guide.clj`.

## 建议阅读路径

如果你第一次进入这个仓库, 建议从以下路径开始:

1. Web 基础: `notebooks/web_dev/starting_web_server.clj`
2. Ring 与中间件: `notebooks/web_dev/starting_ring_server.clj` + `adding_simple_middleware.clj`
3. 内容协商与校验: `muuntaja_content_negotiation.clj` + `malli_spec_usage.clj`
4. 数据访问: `notebooks/hugsql/intro.clj` + `crud.clj`
5. 观察工具: `notebooks/portal/overview.clj`
6. 进阶专题: Datomic / core.async / core.logic / Emmy

## 目录速查

- `notebooks/`: 所有教程 notebook 与专题内容.
- `resources/hugsql/playground.sql`: HugSQL SQL 示例.
- `env/dev/clj/user.clj`: 开发 REPL 辅助函数.
- `build.clj`: `tools.build` 入口.
- `deps.edn`: 依赖与 alias 定义.
- `doc/intro.md`: 简短项目导览.
- `chinook.db`: SQLite 示例数据库.

## 故障排查

### `clojure -M:clerk` 无法启动

当前 `:clerk` alias 在 `deps.edn` 中使用的是 `:exec-fn`, 正确调用方式是:

```sh
clojure -X:clerk
```

同理, 静态构建使用:

```sh
clojure -X:nextjournal/clerk
```

如果你在 `zsh` 下传入额外参数, 记得给带 `?` 的关键字加引号, 例如:

```sh
clojure -X:clerk ':browse?' false ':port' 8888
```

### `user/start-clerk!` 或 `user/show-notebook` 找不到

请确认你是用 `:dev` 启动 REPL:

```sh
clojure -M:dev
```

然后再执行:

```clojure
(require 'user)
```

### 7777 端口已占用

显式指定端口:

```sh
clojure -X:clerk ':browse?' false ':port' 8888
```

或在 REPL 中:

```clojure
(user/start-clerk! {:port 8888})
```

### 静态构建或某些页面缺少前端资源

先安装前端依赖:

```sh
yarn install
```

MathBox 相关页面还依赖浏览器可访问外部资源. 如果页面空白, 先检查:

- `node_modules` 是否已安装.
- 当前网络环境是否能访问依赖下载源.
- 浏览器控制台是否有 JS 资源报错.

### HugSQL / SQLite 示例打不开数据库

确认当前工作目录就是仓库根目录, 并且 `chinook.db` 存在. 仓库已自带该文件, 一般不需要手工初始化.

### `clojure -T:build test` 结果显示 `Ran 0 tests`

当前 `build test` 可以正常执行, 但仓库暂时没有实际的自动化测试用例, 所以你会看到 `Ran 0 tests`. 这表示:

- `tools.build` 与 `test-runner` 入口本身是通的.
- 当前仓库仍以 notebook 演示与 REPL 验证为主, 不是以测试套件驱动.

这种情况下优先使用:

- `clojure -M:dev` + Clerk / REPL 验证.
- 单页 notebook 的交互式检查.

### `clojure -T:build ci` 构建失败

当前 `build.clj` 里的 `:main` 指向 `rc.web-tutorial`, 但仓库中没有对应命名空间. 这说明 `ci` 更接近模板保留项, 不是日常教程运行入口. 优先使用 Clerk 和 REPL 工作流.

### 启动 Clerk 时看到 `native access` warning

在较新的 JDK 上直接运行 `clojure -X:clerk` 时, 可能会看到类似:

```text
WARNING: java.lang.System::load has been called ...
WARNING: Use --enable-native-access=ALL-UNNAMED ...
```

这通常只是 JNA 相关的提示, 不代表 Clerk 启动失败. 如果你想避免这类 warning:

- 优先使用 `clojure -M:dev` 进入 REPL, 再执行 `(user/start-clerk!)`. `:dev` alias 已带上 `--enable-native-access`.
- 或自行在启动命令中补充对应 JVM 选项.

### 首次解析依赖时出现 `~/.gitlibs` clone 冲突

如果你在两个 Clojure 进程里同时首次拉取 git 依赖, 可能会看到类似:

```text
destination path '.../.gitlibs/.../sci.configs' already exists and is not an empty directory
```

这通常不是仓库代码问题, 而是并发下载引起的本地缓存竞争. 处理方式:

- 先串行执行一个 `clojure` 命令, 等依赖下载完成后再开第二个进程.
- 如果缓存目录已经损坏, 删除对应 `~/.gitlibs/_repos/...` 目录后重试.

## 许可证

Copyright © 2025 Kevinli

Distributed under the Eclipse Public License version 1.0.
