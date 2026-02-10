;; # Transit 数据格式深度解析与对比

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns web-dev.transit-deep-dive
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]
            [cognitect.transit :as transit]
            [nextjournal.clerk :as clerk])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.nio.charset StandardCharsets]
           [java.util Base64 Date UUID]))

;; 本页面面向对 Transit 不熟悉, 但需要和 Clojure/ClojureScript 或多语言服务打交道的同事.
;; 目标是把 Transit 的线上表示讲清楚, 并给出能直接跑的编码, 解码, 以及与 JSON/EDN 的对比示例.

;; ## 1. Transit 是什么, 它解决了什么问题

;; Transit 是一种数据传输编码方案, 重点在于跨语言传递 *值* 而不是 *文档*.
;; 它不试图取代 JSON 解析器, 而是把额外语义寄生在 JSON 或 MessagePack 之上:
;; - `Transit-JSON`: 底层是 JSON, 解析阶段可以利用 `JSON.parse` 这种高度优化的实现
;; - `Transit-MessagePack`: 底层是二进制 MessagePack, 用于体积更敏感或服务间通信的场景
;; Transit 的核心手段是 `标签(tag)` 和 `缓存(cache)`.
;; - 标签: 把 UUID, 时间, 关键字, 集合等类型编码到 JSON 的基础类型里
;; - 缓存: 对重复出现的 key 以及部分标量值做去重, 既减小体积, 又减少解析端的对象分配

;; ## 2. 基本类型(ground types)

;; | 语义类型 | JSON 表示 | 说明 |
;; |---|---|---|
;; | string | string | 直接映射 |
;; | boolean | true/false | 直接映射 |
;; | number | number | JSON 数字(在 JS 中有 53 位安全整数限制) |
;; | null | null | 直接映射 |
;; | array | array | 顺序集合的基础载体 |
;; | map(object) | object | 仅当 key 都是字符串时才可用 |

;; 当数据落在这些基本类型里时, Transit 额外开销很小.

;; ## 3. 标签系统: 三种常见形态

;; Transit 把额外类型信息写进字符串或数组里. 常见形态有三种:
;; 1. 标量扩展(escaped string): `"~<tag><value>"`
;; 2. 复合扩展(tagged array): `["~#<tag>", <rep>]`
;; 3. 复合扩展(tagged map): `{ "~#<tag>": <rep> }`

;; 下面用可运行的代码把它们打印出来.

(defn transit-write-bytes
  "把值写成 Transit 字节数组, type 为 :json, :json-verbose 或 :msgpack."
  [type v opts]
  (let [out (ByteArrayOutputStream.)
        w (transit/writer out type opts)]
    (transit/write w v)
    (.toByteArray out)))

(defn transit-read-bytes
  "从 Transit 字节数组读取值, type 为 :json, :json-verbose 或 :msgpack."
  [type bs opts]
  (let [in (ByteArrayInputStream. bs)
        r (transit/reader in type opts)]
    (transit/read r)))

(defn bytes->utf8
  [bs]
  (String. bs StandardCharsets/UTF_8))

(defn bytes->b64
  [bs]
  (.encodeToString (Base64/getEncoder) bs))

^{:nextjournal.clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true
  ::clerk/budget nil}
(let [v {:k1 "plain-string"
         :k2 :keyword
         :k3 'a.symbol
         :k4 (UUID/fromString "a1b2c3d4-e5f6-47b8-90ab-0123456789ab")
         :k5 (Date. 0)
         :k6 #{1 2 3}
         :k7 (list 1 2 3)}]
  {:transit-json (bytes->utf8 (transit-write-bytes :json v {}))
   :transit-json-verbose (bytes->utf8 (transit-write-bytes :json-verbose v {}))
   :transit-msgpack-base64 (bytes->b64 (transit-write-bytes :msgpack v {}))})

;; 观察 `Transit-JSON` 的字符串结果, 会看到类似 `"~:keyword"` 这样的片段.
;; - `~:` 代表关键字
;; - `~$` 代表符号
;; - `~u` 代表 UUID
;; - 时间点通常会以 `~m`(毫秒) 或 `~t`(RFC3339 文本) 出现
;; - `~#set`, `~#list` 这种是复合类型标签

;; `:transit-json` 和 `:transit-json-verbose` 在这个例子里, 除了时间表示(`~m` vs `~t`)之外差异不大, 属于正常现象.
;; 缓存码(`^0`, `^1`...)只有在 *同一个 payload* 里出现重复 key 或重复标签标量时才会触发, 单个 map 通常看不到.

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [v (vec (repeat 3 {:very/long-keyword :some/repeated-value
                        :another/long-keyword :some/repeated-value}))
      s (bytes->utf8 (transit-write-bytes :json v {}))]
  {:transit-json-sample (subs s 0 (min 420 (count s)))
   :hint "如果输出里出现 ^0/^1, 说明缓存已生效; 更明显的演示见第 7 节."})

;; ## 4. 特殊字符与转义

;; Transit 约定字符串首字符有特殊含义, 为了避免歧义, 需要转义:
;; - 以 `~` 开头: 写成 `~~...`
;; - 以 `^` 开头: 写成 `~^...`
;; - 以 `` ` `` 开头: 写成 `` ~`... ``

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [v ["~foo" "^0" "`bar" "normal"]
      s (bytes->utf8 (transit-write-bytes :json v {}))]
  {:transit-json s
   :roundtrip (transit-read-bytes :json (.getBytes s StandardCharsets/UTF_8) {})})

;; ## 5. 53 位安全整数与大整数编码

;; JSON 在很多语言里都能表示整数, 但在 JavaScript 中 number 是双精度浮点数, 超过 `2^53-1` 就无法保证精度.

;; Transit 的做法是: 超出安全范围时, 用带标签的字符串来表达大整数, 这样前端可以用 BigInt 或库提供的 Long 类型来还原.

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [safe-int 9007199254740991
      unsafe-int 9007199254740993
      bigint 9223372036854775807N
      payload {:safe safe-int :unsafe unsafe-int :big bigint}
      s (bytes->utf8 (transit-write-bytes :json payload {}))]
  {:transit-json s
   :roundtrip (transit-read-bytes :json (.getBytes s StandardCharsets/UTF_8) {})})

;; ## 6. 非字符串 key 的 Map: composite map keys

;; JSON object 的 key 必须是字符串, 但 Clojure 的 map key 可以是任意值.
;; Transit 会在必要时把 map 写成数组, 以 `"^ "`(caret + 空格) 开头, 后面按 `k1, v1, k2, v2...` 交替排列.

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [v {[1 2] "point"
         :id 100
         {:nested true} :as-key}
      s (bytes->utf8 (transit-write-bytes :json v {}))]
  {:transit-json s
   :roundtrip (transit-read-bytes :json (.getBytes s StandardCharsets/UTF_8) {})})

;; ## 7. 写入时缓存: 体积与解析成本的双重优化

;; Transit 会对 map 的 key, 以及部分可缓存标量做缓存.

;; 你会在输出里看到 `^0`, `^1` 这类缓存码. 这不是 gzip, 而是协议层的去重, 对解析端也更友好: Reader 可以复用缓存里的字符串对象, 减少分配与 GC 压力.

(def cache-demo
  (vec (repeat 5 {:user/id 1
                  :user/name "Alice"
                  :user/status :user.status/active
                  :user/roles [:admin :dev]
                  :user/profile {:profile/id 9
                                 :profile/tags #{:clojure :web :transit}}})))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [transit-json (bytes->utf8 (transit-write-bytes :json cache-demo {}))
      transit-json-verbose (bytes->utf8 (transit-write-bytes :json-verbose cache-demo {}))
      edn-str (pr-str cache-demo)
      json-str (json/write-str
                (walk/postwalk
                 (fn [x]
                   (cond
                     (keyword? x) (str x)
                     (symbol? x) (str x)
                     (set? x) (vec x)
                     :else x))
                 cache-demo))]
  (let [cache-codes (re-seq #"\\^[0-9A-Za-z]" transit-json)]
    {:sizes {:transit-json-bytes (count (.getBytes transit-json StandardCharsets/UTF_8))
             :transit-json-verbose-bytes (count (.getBytes transit-json-verbose StandardCharsets/UTF_8))
             :edn-bytes (count (.getBytes edn-str StandardCharsets/UTF_8))
             :json-bytes (count (.getBytes json-str StandardCharsets/UTF_8))}
     :cache {:total (count cache-codes)
             :distinct (count (set cache-codes))
             :sample (vec (take 12 (distinct cache-codes)))}
     :transit-json-sample (subs transit-json 0 (min 420 (count transit-json)))}))

;; 上面的 `:transit-json-sample` 里通常能看到 `^` 缓存码, 也能看到 `~:` 关键字标签. 这解释了为什么 Transit 在重复 key 很多的 API 响应里, 往往比纯 JSON 更紧凑.

;; ## 8. 自定义扩展类型与未知标签的透传

;; Transit 支持自定义类型, 关键点是定义 WriteHandler 和 ReadHandler.
;; 同时, Reader 遇到未知标签时默认不会报错, 会返回 `TaggedValue`, 使得中间层服务可以不理解类型也能无损透传.

(defrecord Point [x y])

(def point-write-handler
  (transit/write-handler
   (fn [_] "point")
   (fn [p] [(:x p) (:y p)])
   (fn [_] nil)
   (fn [_] nil)))

(def point-read-handler
  (transit/read-handler
   (fn [rep]
     (let [[x y] rep]
       (->Point x y)))))

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [p (->Point 10 20)
      bs (transit-write-bytes :json p {:handlers {Point point-write-handler}})
      s (bytes->utf8 bs)]
  {:transit-json s
   :roundtrip (transit-read-bytes :json (.getBytes s StandardCharsets/UTF_8) {:handlers {"point" point-read-handler}})})

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [tv (transit/tagged-value "point" [10 20])
      s (bytes->utf8 (transit-write-bytes :json tv {}))]
  {:transit-json s
   :unknown-tag-read-result
   (transit-read-bytes :json (.getBytes s StandardCharsets/UTF_8) {})})

;; ## 9. JSON vs EDN vs Transit: 实用对比

;; 从工程角度看, 三者的关键差异是:
;; - JSON: 生态最大, 但类型贫乏, 非字符串 key, 关键字, UUID, 时间等都需要约定或额外字段
;; - EDN: 表达力强且人类可读, 适合配置与调试, 但浏览器侧解析通常不如 `JSON.parse` 友好
;; - Transit: 在 JSON 的可传输性之上补齐语义类型, 并通过缓存降低体积与解析成本, 是 Clojure/CLJS 前后端通信的常见默认选择

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(let [v {:user/id 42
         :user/uuid (UUID/fromString "a1b2c3d4-e5f6-47b8-90ab-0123456789ab")
         :user/created-at (Date. 0)
         :user/roles #{:admin :dev}}
      transit-json (bytes->utf8 (transit-write-bytes :json v {}))
      edn-str (pr-str v)
      json-str (json/write-str
                {:user/id 42
                 :user/uuid "a1b2c3d4-e5f6-47b8-90ab-0123456789ab"
                 :user/created-at 0
                 :user/roles ["admin" "dev"]})]
  {:transit-json transit-json
   :edn edn-str
   :json json-str})

;; ## 10. 实战建议(与 Muuntaja/Reitit 配合)

;; 在本项目的 Web 教程里, `Muuntaja` 已经默认支持 Transit.

;; - 生产使用一般选 `application/transit+json` 或 `application/transit+msgpack`\
;; - 调试时可以用 `:json-verbose` 直观看到更多标签信息\n\n如果你正在做内容协商, 可以对照 `notebooks/web_dev/muuntaja_content_negotiation.clj` 里对 `application/transit+json` 的示例.
