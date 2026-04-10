# 项目导览

`webdev-tutorials` 是一个以 Clerk notebook 为中心的 Clojure 教学仓库. 主要内容位于 `notebooks/`, 通过浏览器和 REPL 展示各类 Web、数据访问、逻辑编程与可视化主题.

如果你是第一次进入仓库, 推荐先阅读根目录 [README.md](../README.md), 再按以下顺序开始:

1. `notebooks/web_dev/starting_web_server.clj`
2. `notebooks/web_dev/starting_ring_server.clj`
3. `notebooks/hugsql/intro.clj`
4. `notebooks/portal/overview.clj`

开发常用入口:

```sh
clojure -M:dev
clojure -X:clerk ':browse?' true
clojure -X:nextjournal/clerk
```

在 `:dev` REPL 中可使用:

```clojure
(require 'user)
(user/help)
(user/start-clerk!)
(user/show-notebook "notebooks/web_dev/starting_web_server.clj")
```
