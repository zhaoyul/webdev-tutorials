^{:nextjournal.clerk/toc true}
(ns specter_guide
  "Specter 从入门到进阶的可执行笔记."
  {:nextjournal.clerk/error-on-missing-vars :off}
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [com.rpl.specter :as sp]
   [nextjournal.clerk :as clerk]))

;; # Clojure Specter 完全指南: 像手术刀一样精准地操作数据

;; 这份 Clerk notebook 走一条很明确的路线: 先让你感受到 `update-in` 在复杂嵌套与批量修改场景下的痛点, 再用 Specter 的 DSL(路径即程序)把同一类问题写到可读、可组合、可维护。
;;
;; 贯穿全文的核心场景是一份电商系统快照 `world-state`, 递归部分使用一个文件系统/分类树作为辅助场景。
;;
;; 阅读与运行方式:
;;
;; - 浏览器启动 Clerk: `clojure -M:clerk`
;; - 需要跑性能基准时, 建议用 `clojure -M:dev:clerk` 或在 `:dev` REPL 中手动调用文末的基准函数(默认不自动执行, 避免 notebook 变慢)。

;; ------------------------------------------------------------
;; 核心场景数据
;; ------------------------------------------------------------

^{::clerk/visibility {:code :show :result :hide}}
(def world-state
  "场景: 一个复杂的电商订单系统快照.

  注意: 为了把重点放在导航与变换上, 这里的金额使用了简单整数(不区分元/分)。"
  {:users
   {"user-1" {:profile {:name "Alice" :credits 100}
              :orders [{:id 101
                        :status :pending
                        :items [{:id "p1" :price 50 :tags #{:sale}}
                                {:id "p2" :price 100}]}
                       {:id 102
                        :status :shipped
                        :items [{:id "p3" :price 20}]}]}
    "user-2" {:profile {:name "Bob" :credits 50}
              :orders []}}
   :inventory {"p1" {:stock 5}
               "p2" {:stock 0}}})


;; 下面的几个例子会频繁用到同一个任务:
;;
;; > 给所有 `:pending` 状态订单中的 `:sale` 商品价格打 9 折
;;
;; 为了避免引入浮点误差, 示例用 `(* price 9/10)` 这种有理数折扣(在价格是 10 的倍数时结果是整数)。

;; ------------------------------------------------------------
;; 第一部分: 困境与破局
;; ------------------------------------------------------------

;; ## 1.1 标准库的极限: 能写, 但不想维护

^{::clerk/visibility {:code :show :result :hide}}
(defn discount-pending-sale-vanilla
  "反面教材: 使用 update/map/assoc 组合, 完成“pending 订单的 sale 商品打 9 折”.

这段代码的主要问题不在于“写不出来”, 而在于:

- 嵌套层数增加时, 可读性快速下降.
- 业务条件(状态、标签)散落在多个匿名函数里, 不利于复用与组合.
- 想复用其中一段路径时, 只能拷贝粘贴或再抽函数, 反而更重."
  [world]
  (update world :users
          (fn [users]
            (into {}
                  (for [[user-id user] users]
                    [user-id
                     (update user :orders
                             (fn [orders]
                               (mapv
                                (fn [order]
                                  (if (= :pending (:status order))
                                    (update order :items
                                            (fn [items]
                                              (mapv
                                               (fn [item]
                                                 (if (contains? (:tags item) :sale)
                                                   (update item :price #(* % 9/10))
                                                   item))
                                               items)))
                                    order))
                                orders)))])))))

^{::clerk/visibility {:code :show :result :show}}
(let [before (sp/select [:users "user-1" :orders sp/ALL :items sp/ALL :price] world-state)
      after (sp/select [:users "user-1" :orders sp/ALL :items sp/ALL :price]
                       (discount-pending-sale-vanilla world-state))]
  {:before before
   :after after})

;; ## 1.2 Specter 的哲学: 路径即程序


;; Specter 不是一个“更强的 get-in/update-in”, 它更像是一种用于**导航数据结构**的 DSL:
;;
;; - `select`: 选择(查询)路径命中的所有位置
;; - `transform`: 变换路径命中的所有位置, 得到一个新结构(持久化数据结构, 原结构不变)
;;
;; 关键差异在于: **条件、遍历、结构导航都可以写进路径里**, 从而把“怎么走到那里”和“到了之后怎么改”分开。

^{::clerk/visibility {:code :show :result :hide}}
(defn discount-pending-sale-specter
  "Specter 写法: 把“走到哪里”声明为路径, 把“怎么改”交给 transform 函数."
  [world]
  (sp/transform
   [:users sp/MAP-VALS
    :orders sp/ALL
    #(= :pending (:status %))
    :items sp/ALL
    #(contains? (:tags %) :sale)
    :price]
   #(* % 9/10)
   world))

^{::clerk/visibility {:code :show :result :show}}
(let [before (sp/select [:users "user-1" :orders sp/ALL :items sp/ALL :price] world-state)
      after (sp/select [:users "user-1" :orders sp/ALL :items sp/ALL :price]
                       (discount-pending-sale-specter world-state))]
  {:before before
   :after after})

;; ------------------------------------------------------------
;; 第二部分: 核心导航器详解
;; ------------------------------------------------------------


;; ## 2.1 基础导航: 在 Map 与 Sequence 中移动


;; 最常用的一组“结构导航器”:
;;
;; - `ALL`: 遍历序列的每个元素(对 map 来说, 会遍历 entry `[k v]`)
;; - `MAP-VALS`, `MAP-KEYS`: 遍历 map 的值/键
;; - `FIRST`, `LAST`: 选择序列首/尾元素(对 entry 也适用)
;;
;; 下面用 `world-state` 做几个很短的例子。

^{::clerk/visibility {:code :show :result :show}}
{:user-ids (sp/select [:users sp/MAP-KEYS] world-state)
 :user-names (sp/select [:users sp/MAP-VALS :profile :name] world-state)}

^{::clerk/visibility {:code :show :result :show}}
(let [world' (sp/transform [:users sp/MAP-VALS :profile :credits] #(+ % 10) world-state)]
  {:before (sp/select [:users sp/MAP-VALS :profile :credits] world-state)
   :after (sp/select [:users sp/MAP-VALS :profile :credits] world')})


;; ## 2.2 精准打击: 过滤与子集


;; Specter 把“过滤条件”也当作路径的一部分. 你可以用:
;;
;; - 直接把 predicate(例如 `even?`, `keyword?`, 或匿名函数)写进路径
;; - `selected?`: 如果某个子路径能选到东西, 就保留当前元素
;; - `filterer`: 在路径内部对当前序列做过滤视图, 继续向下导航
;;
;; 这会让代码看起来更像是一个声明式查询/更新。

^{::clerk/visibility {:code :show :result :show}}
(sp/select
 [:inventory
  sp/ALL
  (sp/selected? sp/LAST :stock zero?)
  sp/FIRST]
 world-state)

^{::clerk/visibility {:code :show :result :show}}
(sp/select
 [:users sp/MAP-VALS
  :orders (sp/filterer :status #(= :pending %))
  sp/ALL :id]
 world-state)


;; ## 2.3 改变结构: 不仅是修改值

;; 两组很实用的能力:
;;
;; - `setval` vs `transform`: 直接覆盖 vs 基于旧值计算
;; - `collect` / `collect-one`: 在向下导航时, 把“上文信息”收集起来, 让变换函数可以同时拿到这些上下文值
;;
;; `collect-one` 常用于“从父节点抓一个字段, 用来改子节点”。

^{::clerk/visibility {:code :show :result :show}}
(let [world' (sp/setval [:inventory "p2" :stock] 10 world-state)]
  {:before (get-in world-state [:inventory "p2"])
   :after (get-in world' [:inventory "p2"])})


;; ### 报表生成示例: 列出 `[用户名, 订单ID]` 的所有组合
;;
;; 思路: 在遍历每个用户时, 先用 `collect-one` 抓取用户名, 再把每个订单变换成 `[name order-id]` 的二元组. 这样就能用 `select` 直接把所有二元组取出来。

^{::clerk/visibility {:code :show :result :hide}}
(defn user-order-report
  "生成一个扁平的 `[用户名 订单ID]` 列表."
  [world]
  (let [report-world
        (sp/transform
         [:users sp/ALL
          (sp/collect-one sp/LAST :profile :name)
          sp/LAST
          :orders sp/ALL]
         (fn [user-name order] [user-name (:id order)])
         world)]
    (sp/select [:users sp/MAP-VALS :orders sp/ALL] report-world)))

^{::clerk/visibility {:code :show :result :show}}
(user-order-report world-state)

;; ------------------------------------------------------------
;; 第三部分: 高级特性
;; ------------------------------------------------------------


;; ## 3.1 递归路径: 处理不定深度的树


;; 辅助场景: 一个文件系统/分类树.

;; Specter 的 `recursive-path` 允许你定义一个可复用的递归导航器. 常见的两种遍历顺序:

;; - `stay-then-continue`: 先访问当前节点, 再访问子节点(前序)
;; - `continue-then-stay`: 先访问子节点, 再访问当前节点(后序)

^{::clerk/visibility {:code :show :result :hide}}
(def tree
  {:name "Root"
   :children [{:name "A" :children []}
              {:name "B"
               :children [{:name "B1" :children []}
                          {:name "B2" :children [{:name "B21" :children []}]}]}]})

^{::clerk/visibility {:code :show :result :hide}}
(def TREE-NODES
  "树的前序遍历导航器: 访问所有节点."
  (sp/recursive-path [] p
    (sp/stay-then-continue :children sp/ALL p)))

^{::clerk/visibility {:code :show :result :show}}
(sp/select [TREE-NODES :name] tree)

^{::clerk/visibility {:code :show :result :show}}
(sp/transform [TREE-NODES :name] str/upper-case tree)


;; ## 3.2 自定义导航器: 让 Specter 适配非标准结构

;; 当你的数据不是普通的 map/vector(例如 Java 对象、deftype、第三方库结构), 仍然可以通过 `defnav` 为它们定义 Specter 导航器.
;;
;; 示例里用 `deftype` 模拟一个 Java 风格的 `User` 对象(字段不可直接用 `assoc/update` 修改), 然后用自定义导航器让它可被 `select/transform` 操作。

^{::clerk/visibility {:code :show :result :hide}}
(deftype JUser [^String name ^long credits])

^{::clerk/visibility {:code :show :result :hide}}
(defn juser->map
  "把 JUser 转成 map, 便于在 notebook 中展示."
  [^JUser u]
  {:name (.-name u)
   :credits (.-credits u)})

^{::clerk/visibility {:code :show :result :hide}}
(sp/defnav JUSER-NAME
  []
  (select* [this u next-fn]
    (next-fn (.-name ^JUser u)))
  (transform* [this u next-fn]
    (let [name' (next-fn (.-name ^JUser u))]
      (JUser. name' (.-credits ^JUser u)))))

^{::clerk/visibility {:code :show :result :hide}}
(sp/defnav JUSER-CREDITS
  []
  (select* [this u next-fn]
    (next-fn (.-credits ^JUser u)))
  (transform* [this u next-fn]
    (let [credits' (long (next-fn (.-credits ^JUser u)))]
      (JUser. (.-name ^JUser u) credits'))))

^{::clerk/visibility {:code :show :result :show}}
(let [jworld {:users [(JUser. "Alice" 100)
                      (JUser. "Bob" 50)]}]
  {:names (sp/select [:users sp/ALL JUSER-NAME] jworld)
   :credits (sp/select [:users sp/ALL JUSER-CREDITS] jworld)
   :after (->> (sp/transform [:users sp/ALL JUSER-CREDITS] #(+ % 10) jworld)
               :users
               (mapv juser->map))})

;; ------------------------------------------------------------
;; 第四部分: 性能与内部机制
;; ------------------------------------------------------------

;; ## 4.1 宏的魔法: 编译期优化与路径缓存

;; Specter 的 `select/transform/setval` 都是宏. 当路径是**静态字面量**(例如 `[ALL :a]`)时, 宏可以在编译期把路径编译成高效的导航器组合, 并做缓存。

;; 你可以用 `macroexpand-1` 直观看到这一点: `transform` 会把路径包装进 `com.rpl.specter/path`, 后者会做缓存与预编译。

^{::clerk/visibility {:code :show :result :show}}
(let [form '(sp/transform [sp/ALL :a] inc data)]
  (clerk/md
   (str "```clojure\n"
        (with-out-str (pp/pprint (macroexpand-1 form)))
        "```")))

;; 对比一下 `transform*`(函数版本)的实现. 它会在运行时编译路径, 因此在热路径里反复调用通常更慢。

^{::clerk/visibility {:code :show :result :show}}
(clerk/md
 (str "```clojure\n"
      (with-out-str
        (pp/pprint
         '(defn transform* [path transform-fn structure]
            (compiled-transform (i/comp-paths* path) transform-fn structure))))
      "```"))


;; ## 4.2 性能对比实验: Vanilla vs Specter(Inline/Compiled/Dynamic)

;; 下面这段会在 `criterium` 可用时直接跑基准, 并把结果用 Plotly 柱状图展示出来。
;;
;; 注意:
;;
;; - 如果你用的是 `clojure -M:clerk`, 默认 classpath 没有 `criterium`, 本节会给出提示但不会报错.
;; - 建议用 `clojure -M:dev:clerk` 启动 Clerk(这样 `criterium` 才在 classpath 上).
;; - 基准默认只在当前 JVM 会话里跑一次并缓存, 需要重跑可调用 `(rerun-criterium!)`。

^{::clerk/visibility {:code :show :result :hide}}
(defonce ^{:doc "用于基准测试的 10 万个元素."} big-data
  (vec (map (fn [i] {:a i :b i}) (range 100000))))

^{::clerk/visibility {:code :show :result :hide}}
(defonce ^{:doc "预编译路径: 对所有元素的 :a 字段做操作."} compiled-a-path
  (sp/comp-paths sp/ALL :a))

^{::clerk/visibility {:code :show :result :hide}}
(defn vanilla-update
  "基准线: 原生 mapv + update."
  [xs]
  (mapv #(update % :a inc) xs))

^{::clerk/visibility {:code :show :result :hide}}
(defn specter-inline
  "Specter inline: 路径写在调用点, 由宏做缓存与预编译."
  [xs]
  (sp/transform [sp/ALL :a] inc xs))

^{::clerk/visibility {:code :show :result :hide}}
(defn specter-compiled
  "Specter compiled: 先 comp-paths 一次, 再在热路径里复用 compiled-* 函数."
  [xs]
  (sp/compiled-transform compiled-a-path inc xs))

^{::clerk/visibility {:code :show :result :hide}}
(defn specter-dynamic
  "Specter dynamic: 使用 transform* 每次都会在运行时编译路径(通常更慢)."
  [xs]
  (sp/transform* [sp/ALL :a] inc xs))

^{::clerk/visibility {:code :show :result :hide}}
(defn time-ms
  "执行 f, 返回 {:ms 耗时毫秒, :ret 返回值}. 用于粗略对比."
  [f]
  (let [t0 (System/nanoTime)
        ret (f)
        t1 (System/nanoTime)]
    {:ms (/ (- t1 t0) 1e6)
     :ret ret}))

^{::clerk/visibility {:code :show :result :hide}}
(defn run-time-bench!
  "粗略基准: 每个函数跑 n 次, 返回每次耗时(毫秒)向量.

注意: `time`/nanoTime 没有做 JVM 预热与统计学处理, 只能用于快速感知趋势。"
  [{:keys [n] :or {n 8}}]
  (let [cases [{:k :vanilla :f #(vanilla-update big-data)}
               {:k :specter-inline :f #(specter-inline big-data)}
               {:k :specter-compiled :f #(specter-compiled big-data)}
               {:k :specter-dynamic :f #(specter-dynamic big-data)}]
        warmup (doseq [{:keys [f]} cases] (dotimes [_ 2] (f)))]
    (into {}
          (map (fn [{:keys [k f]}]
                 [k (vec (for [_ (range n)]
                           (:ms (time-ms f))))]))
          cases)))

^{::clerk/visibility {:code :show :result :hide}}
(defonce ^:private criterium-cache (atom nil))

^{::clerk/visibility {:code :show :result :hide}}
(def ^:private bench-version
  "基准实现版本号. 修改基准逻辑时递增, 用于自动失效缓存."
  2)

^{::clerk/visibility {:code :show :result :hide}}
(def bench-cases
  "四组对比用例."
  [{:id :vanilla
    :label "Vanilla(mapv/update)"
    :thunk #(-> (vanilla-update big-data) first :a)}
   {:id :specter-inline
    :label "Specter(inline)"
    :thunk #(-> (specter-inline big-data) first :a)}
   {:id :specter-compiled
    :label "Specter(comp-paths)"
    :thunk #(-> (specter-compiled big-data) first :a)}
   {:id :specter-dynamic
    :label "Specter(dynamic transform*)"
    :thunk #(-> (specter-dynamic big-data) first :a)}])

;; 说明(这也是你截图里“和预期不一致”的关键原因):
;;
;; - 这组基准是非常浅的路径 `[ALL :a]`. 对这种场景, 手写 `mapv + update` 往往就是最快的, Specter 的通用导航层会带来固定开销, 所以慢一点很正常.
;; - `transform` 是宏, 内部会用 `path` 宏做“编译期预编译 + 缓存”. 所以 **inline 其实已经是编译过的路径**.
;; - `comp-paths` 是运行时把导航器组合起来, 并不等价于 `path` 宏的那套预编译策略. 因此 `comp-paths` 在某些简单路径上不一定更快(甚至可能略慢)。
;; - “动态路径很慢”通常出现在: 路径需要运行时拼装、参数化、或反复把 path 当数据传来传去. 这种开销在一次性更新 10 万个元素时, 很可能被数据改写本身的成本淹没。

^{::clerk/visibility {:code :show :result :hide}}
(def default-criterium-opts
  "用于 notebook 的默认参数, 目标是把运行时间控制在可接受范围内.

  如果你想要更稳定的统计结果, 可以:

  - 在 REPL 中调用 `(rerun-criterium! {:mode :quick :options {...}})`
  - 或者使用 `:mode :full` 跑更严格的 `benchmark*`"
  {:samples 4
   :target-execution-time 30000000
   :warmup-jit-period 1000000000
   :bootstrap-size 200})

^{::clerk/visibility {:code :show :result :hide}}
(defn- secs->ms [x]
  (* 1000.0 (double x)))

^{::clerk/visibility {:code :show :result :hide}}
(defn- stat->ci
  "解析 criterium 的统计字段格式: [estimate [lower upper]]."
  [stat]
  (when (and (vector? stat) (= 2 (count stat)))
    (let [[est ci] stat
          [lower upper] (when (sequential? ci) ci)]
      {:est (double est)
       :lower (double lower)
       :upper (double upper)})))

^{::clerk/visibility {:code :show :result :hide}}
(defn- case->summary
  [{:keys [id label result]}]
  (let [{:keys [est lower upper]} (stat->ci (:mean result))]
    {:id id
     :label label
     :mean-ms (secs->ms est)
     :lower-ms (secs->ms lower)
     :upper-ms (secs->ms upper)
     :execution-count (:execution-count result)
     :sample-count (:sample-count result)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn bench->plotly
  "将 summary(向量)渲染为 Plotly 柱状图."
  ([summary] (bench->plotly summary {}))
  ([summary {:keys [title]
             :or {title "Criterium 基准: mean(ms) + 95% CI"}}]
  (let [xs (mapv :label summary)
        ys (mapv :mean-ms summary)
        err+ (mapv (fn [{:keys [mean-ms upper-ms]}] (- upper-ms mean-ms)) summary)
        err- (mapv (fn [{:keys [mean-ms lower-ms]}] (- mean-ms lower-ms)) summary)
        colors ["#1f77b4" "#2ca02c" "#ff7f0e" "#d62728"]]
    (clerk/plotly
     {:data [{:type "bar"
              :x xs
              :y ys
              :marker {:color colors}
              :error_y {:type "data"
                        :symmetric false
                        :array err+
                        :arrayminus err-}}]
      :layout {:title title
               :margin {:l 50 :r 10 :b 80 :t 50}
               :paper_bgcolor "transparent"
               :plot_bgcolor "transparent"
               :yaxis {:title "mean (ms)" :rangemode "tozero"}
               :xaxis {:tickangle -20}}
      :config {:displayModeBar false
               :displayLogo false}}))))

^{::clerk/visibility {:code :show :result :hide}}
(defn run-criterium-cases
  "在当前 JVM 中对 cases 执行 criterium, 返回结构化结果.

cases 形如:

```clojure
{:id :case-id
 :label \"Case label\"
 :thunk (fn [] ...)}
```"
  [cases
   {:keys [mode options]
    :or {mode :quick
         options default-criterium-opts}}]
  (let [bench* (try
                 (case mode
                   :quick (requiring-resolve 'criterium.core/quick-benchmark*)
                   :full (requiring-resolve 'criterium.core/benchmark*)
                   (throw (ex-info "未知 mode" {:mode mode})))
                 (catch Throwable _ nil))]
    (if-not bench*
      {:status :missing
       :hint "未找到 criterium, 请用 clojure -M:dev:clerk 启动 Clerk(criterium 在 :dev alias 里)。"}
      (let [results (mapv (fn [{:keys [id label thunk]}]
                            (let [r (bench* thunk options)]
                              {:id id
                               :label label
                               ;; 避免把 benchmark 的返回值(:results)留在缓存里占用内存.
                               :result (select-keys r [:mean :execution-count :sample-count])}))
                          cases)]
        {:status :ok
         :mode mode
         :options options
         :summary (mapv case->summary results)}))))

^{::clerk/visibility {:code :show :result :hide}}
(defn run-criterium-bench!
  "在当前 JVM 中执行 criterium, 返回结构化结果.

启动方式建议:

- `clojure -M:dev:clerk` 打开 notebook
   - 或者在 `:dev` REPL 中 `(require 'specter_guide)` 后调用本函数"
  [opts]
  (run-criterium-cases bench-cases opts))

^{::clerk/visibility {:code :show :result :hide}}
(defn criterium-report
  "返回 criterium 结果, 成功时会缓存."
  ([] (criterium-report {:mode :quick}))
  ([opts]
   (let [cache-key {:bench :big-vector
                    :version bench-version
                    :mode (:mode opts :quick)
                    :options (:options opts default-criterium-opts)
                    :cases (mapv (juxt :id :label) bench-cases)}]
     (if (and @criterium-cache (= cache-key (:cache-key @criterium-cache)))
       @criterium-cache
       (let [res (assoc (run-criterium-bench! opts) :cache-key cache-key)]
         (when (= :ok (:status res))
           (reset! criterium-cache res))
         res)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn rerun-criterium!
  "清空缓存并重新跑 criterium."
  ([] (rerun-criterium! {:mode :quick}))
  ([opts]
   (reset! criterium-cache nil)
   (criterium-report opts)))

^{::clerk/visibility {:code :hide :result :show}}
(let [{:keys [status hint summary] :as report} (criterium-report {:mode :quick})]
  (cond
    (= :missing status)
    (clerk/md hint)

    (= :ok status)
    (bench->plotly summary {:title "Criterium 基准(10 万元素): mean(ms) + 95% CI"})

    :else
    report))

^{::clerk/visibility {:code :hide :result :show}}
(let [{:keys [status summary]} (criterium-report {:mode :quick})]
  (when (= :ok status)
    summary))

;; ------------------------------------------------------------
;; 4.2.1 路径编译开销放大实验: 小数据, 多次重复
;; ------------------------------------------------------------

;; ### 4.2.1 路径编译开销: 为什么动态 path 有时会慢得多?

;; 上一节的大数据基准里, 主要时间花在“改写 10 万个元素”本身, 路径编译的成本很容易被淹没。
;;
;; 这里我们换一个相反的场景:
;;
;; - 结构很小(单个嵌套 map)
;; - 操作重复很多次
;;
;; 这会放大“每次都编译路径”的固定开销, 更接近真实业务里那种: 小对象频繁更新、路径在运行时拼装/参数化的情况。

^{::clerk/visibility {:code :show :result :hide}}
(def tiny-structure
  {:a {:b {:c {:d {:e 0}}}}})

^{::clerk/visibility {:code :show :result :hide}}
(def tiny-path
  [:a :b :c :d :e])

^{::clerk/visibility {:code :show :result :hide}}
(def tiny-compiled-path
  (apply sp/comp-paths tiny-path))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-inline-once []
  (get-in (sp/transform [:a :b :c :d :e] inc tiny-structure) tiny-path))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-compiled-once []
  (get-in (sp/compiled-transform tiny-compiled-path inc tiny-structure) tiny-path))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-dynamic-once []
  (get-in (sp/transform* tiny-path inc tiny-structure) tiny-path))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-inline-loop
  "重复 n 次, 每次都做同样的 transform. 返回最终值(防止被优化掉)."
  [n]
  (loop [i 0
         m tiny-structure]
    (if (= i n)
      (get-in m tiny-path)
      (recur (unchecked-inc-int i)
             (sp/transform [:a :b :c :d :e] inc m)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-compiled-loop
  [n]
  (loop [i 0
         m tiny-structure]
    (if (= i n)
      (get-in m tiny-path)
      (recur (unchecked-inc-int i)
             (sp/compiled-transform tiny-compiled-path inc m)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-dynamic-loop
  [n]
  (loop [i 0
         m tiny-structure]
    (if (= i n)
      (get-in m tiny-path)
      ;; 每次都走 transform* -> 运行时编译 tiny-path
      (recur (unchecked-inc-int i)
             (sp/transform* tiny-path inc m)))))

^{::clerk/visibility {:code :show :result :hide}}
(defonce ^:private tiny-criterium-cache (atom nil))

^{::clerk/visibility {:code :show :result :hide}}
(def ^:private tiny-bench-version
  "tiny 基准实现版本号."
  1)

^{::clerk/visibility {:code :show :result :hide}}
(def tiny-bench-cases
  [{:id :inline-loop
    :label "inline(1000x)"
    :thunk #(tiny-inline-loop 1000)}
   {:id :compiled-loop
    :label "compiled(1000x)"
    :thunk #(tiny-compiled-loop 1000)}
   {:id :dynamic-loop
    :label "dynamic transform*(1000x)"
    :thunk #(tiny-dynamic-loop 1000)}])

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-criterium-report
  "返回 tiny benchmark 结果, 成功时会缓存."
  ([] (tiny-criterium-report {:mode :quick}))
  ([opts]
   (let [cache-key {:bench :tiny-loop
                    :version tiny-bench-version
                    :mode (:mode opts :quick)
                    :options (:options opts default-criterium-opts)
                    :cases (mapv (juxt :id :label) tiny-bench-cases)}]
     (if (and @tiny-criterium-cache (= cache-key (:cache-key @tiny-criterium-cache)))
       @tiny-criterium-cache
       (let [res (assoc (run-criterium-cases tiny-bench-cases opts) :cache-key cache-key)]
         (when (= :ok (:status res))
           (reset! tiny-criterium-cache res))
         res)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn rerun-tiny-criterium!
  "清空 tiny 基准缓存并重新跑."
  ([] (rerun-tiny-criterium! {:mode :quick}))
  ([opts]
   (reset! tiny-criterium-cache nil)
   (tiny-criterium-report opts)))

^{::clerk/visibility {:code :hide :result :show}}
(let [{:keys [status hint summary] :as report} (tiny-criterium-report {:mode :quick})]
  (cond
    (= :missing status)
    (clerk/md hint)

    (= :ok status)
    (bench->plotly summary {:title "Criterium 基准(小数据循环): mean(ms) + 95% CI"})

    :else
    report))

^{::clerk/visibility {:code :hide :result :show}}
(let [{:keys [status summary]} (tiny-criterium-report {:mode :quick})]
  (when (= :ok status)
    summary))

;; ## 4.3 缓存策略: comp-paths 与可复用的路径常量

;; 如果你在业务代码里反复执行同一条导航路径, 推荐把路径提取成常量, 并用 `compiled-*` 系列函数执行:
;;
;; - 可读性: 给路径取一个业务名字
;; - 性能: 路径只编译一次, 调用处不再承担编译成本

^{::clerk/visibility {:code :show :result :hide}}
(defonce ^{:doc "pending 订单里 sale 商品的价格位置."} PENDING-SALE-PRICE
  (sp/comp-paths
   :users sp/MAP-VALS
   :orders sp/ALL #(= :pending (:status %))
   :items sp/ALL #(contains? (:tags %) :sale)
   :price))

^{::clerk/visibility {:code :show :result :hide}}
(defn discount-pending-sale-compiled
  "使用预编译路径的版本. 适合在请求处理/批处理里反复调用."
  [world]
  (sp/compiled-transform PENDING-SALE-PRICE #(* % 9/10) world))

^{::clerk/visibility {:code :show :result :show}}
(let [after (discount-pending-sale-compiled world-state)]
  (sp/select [:users "user-1" :orders sp/ALL :items sp/ALL :price] after))

;; ------------------------------------------------------------
;; 第五部分: 最佳实践与总结
;; ------------------------------------------------------------

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md
 "## 5.1 什么时候用?

- 深层嵌套(通常超过 2 层)且要批量修改
- 更新逻辑里有明显的 where 条件(状态、标签、阈值等)
- 需要把“遍历 + 过滤 + 更新”写成可组合的一条路径
- 遇到递归结构(树/图的某种子集), 用 `recursive-path` 会非常省心

## 5.2 什么时候不用?

- 简单的 `get` / `assoc` / `update` 就能清晰表达
- 路径太长以至于读者需要 2 分钟才能看懂, 这时更该拆分成命名良好的路径常量或普通函数

## 5.3 总结

把 Specter 想成数据结构上的“正则表达式”很贴切:

- 路径表达式负责描述你想命中的位置
- `select/transform` 负责把查询与变换落到这些位置上")
