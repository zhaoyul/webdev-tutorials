^{:nextjournal.clerk/toc true}
(ns transducer_guide
  "Transducer、reducers 与惰性序列对比 notebook."
  {:nextjournal.clerk/error-on-missing-vars :off}
  (:require [clojure.core.reducers :as r]
            [nextjournal.clerk :as clerk]))

;; # Clojure Transducer, Reducers 与惰性序列对比
;;
;; 这一页把三条常见路线放在一起看:
;;
;; - 最早、最直观的惰性序列管道;
;; - 更通用的 transducer;
;; - 面向归约与 `fold` 的 reducers.
;;
;; 它们都能表达 `map/filter/remove/reduce` 这一类逐元素处理流程, 但抽象层次和性能特征并不一样.
;;
;; 阅读与运行方式:
;;
;; - 启动 Clerk: `clojure -M:clerk`
;; - 在 REPL 中打开本页: `(user/show-notebook "notebooks/transducer_guide.clj")`

;; ## 1. 先从最早的惰性序列管道开始
;;
;; 这是大多数人最先接触到的写法:

^{::clerk/visibility {:code :show :result :hide}}
(def raw-numbers
  "一组简单整数, 用来对比惰性序列、transducer 与 reducers."
  (range 1 20))

^{::clerk/visibility {:code :show :result :show}}
(->> raw-numbers
     (filter odd?)
     (map #(* % %))
     (take 4)
     vec)

;; 如果把每一步拆开, 会更容易看清它的运行模型.
;;
;; 注意: 下面这些中间值不是一次性完整 materialize 出来的大集合,
;; 但每一步都会生成新的 lazy seq 视图层.

^{::clerk/visibility {:code :show :result :hide}}
(def lazy-odd-view
  "第 1 层惰性视图: 只保留奇数."
  (filter odd? raw-numbers))

^{::clerk/visibility {:code :show :result :hide}}
(def lazy-square-view
  "第 2 层惰性视图: 对奇数求平方."
  (map #(* % %)
       lazy-odd-view))

^{::clerk/visibility {:code :show :result :hide}}
(def lazy-top4-view
  "第 3 层惰性视图: 只取前 4 个."
  (take 4 lazy-square-view))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 [{:阶段 "filter odd?"
   :类型 (str (type lazy-odd-view))
   :预览 (vec (take 6 lazy-odd-view))}
  {:阶段 "map square"
   :类型 (str (type lazy-square-view))
   :预览 (vec (take 6 lazy-square-view))}
  {:阶段 "take 4"
   :类型 (str (type lazy-top4-view))
   :预览 (vec lazy-top4-view)}])

;; 这类写法的优点是直观、可读、支持按需消费.
;; 代价是: 终端归约时, 数据会穿过一层层 seq 管道.

^{::clerk/visibility {:code :show :result :show}}
(->> raw-numbers
     (filter odd?)
     (map #(* % %))
     (remove #(zero? (mod % 5)))
     (reduce + 0))

;; ## 2. 同样的逐元素逻辑, 改写成 transducer
;;
;; Transducer 关心的不是" 结果是不是 seq", 而是" 如何变换 reducing function".
;; 这让同一段处理流程可以复用到多个出口.

^{::clerk/visibility {:code :show :result :hide}}
(def odd-square-xf
  "先过滤奇数, 再求平方, 最后只保留前 4 个结果."
  (comp
   (filter odd?)
   (map #(* % %))
   (take 4)))

^{::clerk/visibility {:code :show :result :hide}}
(def odd-square-eduction
  "把 transducer 与原始数据绑定成可归约视图."
  (eduction odd-square-xf raw-numbers))

^{::clerk/visibility {:code :show :result :show}}
(clerk/table
 [{:入口 "sequence"
   :返回值 "惰性 seq"
   :结果 (vec (sequence odd-square-xf raw-numbers))}
  {:入口 "into []"
   :返回值 "向量"
   :结果 (into [] odd-square-xf raw-numbers)}
  {:入口 "transduce +"
   :返回值 "标量"
   :结果 (transduce odd-square-xf + 0 raw-numbers)}
  {:入口 "eduction"
   :返回值 (str (type odd-square-eduction))
   :结果 (into [] odd-square-eduction)}])

;; 关键区别是: `odd-square-xf` 不再绑定某一种返回容器.
;; 同一个 transducer 可以同时服务于 `sequence`、`into`、`transduce`、`eduction`.

;; ## 3. Reducer / reducers 是什么
;;
;; `clojure.core.reducers` 提供的是另一条思路:
;;
;; - 把数据处理写成 reducible / foldable 视图;
;; - 重点放在归约与并行 `fold`, 而不是 seq 遍历;
;; - 特别适合 vector 这类可高效切分的数据结构.

^{::clerk/visibility {:code :show :result :hide}}
(def odd-square-reducer-view
  "reducers 视图: 先过滤奇数, 再求平方."
  (->> (vec raw-numbers)
       (r/filter odd?)
       (r/map #(* % %))))

^{::clerk/visibility {:code :show :result :show}}
(clerk/table
 [{:入口 "into []"
   :返回值 "向量"
   :结果 (into [] (take 4) odd-square-reducer-view)}
  {:入口 "r/reduce +"
   :返回值 "标量"
   :结果 (r/reduce + 0 odd-square-reducer-view)}
  {:入口 "r/fold +"
   :返回值 "标量"
   :结果 (r/fold + odd-square-reducer-view)}
  {:入口 "view type"
   :返回值 "类型"
   :结果 (str (type odd-square-reducer-view))}])

;; 可以把 reducers 理解为" 为归约优化的数据处理管道".
;; 它不像 transducer 那样完全通用, 但在 `fold` 友好的集合上往往能跑得很好.

;; ## 4. 三条路线的抽象层次对比

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 [{:方案 "惰性序列"
   :核心抽象 "seq"
   :优势 "最直观, 可按需消费"
   :典型代价 "形成多层 lazy seq 视图, 终端归约时逐层穿过"}
  {:方案 "transducer"
   :核心抽象 "reducing function 的变换"
   :优势 "同一逻辑可复用到 sequence/into/transduce/eduction"
   :典型代价 "抽象更底层, 初学者需要适应"}
  {:方案 "reducers"
   :核心抽象 "reducible / foldable view"
   :优势 "归约友好, `r/fold` 在可切分集合上可能很快"
   :典型代价 "API 面更窄, 更偏向批处理与归约"}])

;; 一个经验法则:
;;
;; - 只是想写一段清楚的逐元素处理逻辑, 先用惰性序列;
;; - 想把同一套逻辑复用到多个目标容器或汇总过程, 用 transducer;
;; - 输入是大向量, 终点又是归约/汇总, 可以考虑 reducers.

;; ## 5. 一个更接近业务的 transducer 例子

^{::clerk/visibility {:code :show :result :hide}}
(def orders
  "模拟订单流, 用来展示 transducer 在业务规则中的复用方式."
  [{:order-id "SO-1001" :customer "Alice" :status :paid :amount 120 :region :east}
   {:order-id "SO-1002" :customer "Bob" :status :draft :amount 220 :region :west}
   {:order-id "SO-1003" :customer "Chen" :status :shipped :amount 260 :region :north}
   {:order-id "SO-1004" :customer "Dora" :status :paid :amount 180 :region :east}
   {:order-id "SO-1005" :customer "Evan" :status :cancelled :amount 320 :region :south}
   {:order-id "SO-1006" :customer "Fang" :status :paid :amount 340 :region :west}
   {:order-id "SO-1007" :customer "Gao" :status :shipped :amount 90 :region :north}
   {:order-id "SO-1008" :customer "Hui" :status :paid :amount 410 :region :south}])

^{::clerk/visibility {:code :show :result :hide}}
(def order-report-xf
  "挑出已支付/已发货且金额不低于 180 的订单, 并补一个简单分层标签."
  (comp
   (filter #(#{:paid :shipped} (:status %)))
   (filter #(>= (:amount %) 180))
   (map (fn [{:keys [amount] :as order}]
          (assoc (select-keys order [:order-id :customer :status :amount :region])
                 :tier (if (>= amount 300) :vip :standard))))))

^{::clerk/visibility {:code :show :result :show}}
{:订单列表 (into [] order-report-xf orders)
 :总金额 (transduce (comp order-report-xf (map :amount)) + 0 orders)
 :客户集合 (into #{} (comp order-report-xf (map :customer)) orders)}

;; 同一个 `order-report-xf`, 可以直接落到向量、集合或标量.
;; 这正是 transducer 在业务层最常见的价值.

;; ## 6. `sequence`、`into`、`transduce`、`eduction` 怎么选

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 [{:API "sequence"
   :适合 "想保留惰性拉取特性"
   :说明 "返回惰性 seq, 按需消费结果"}
  {:API "into"
   :适合 "想直接落到某个容器"
   :说明 "最常见, 如 `[]`、`#{}`、`()`"}
  {:API "transduce"
   :适合 "想直接做归约或汇总"
   :说明 "避免中间集合, 直接得到最终值"}
  {:API "eduction"
   :适合 "想保留可归约视图, 稍后再消费"
   :说明 "不缓存结果, 适合把 xform 与数据源绑定为可复用视图"}])

^{::clerk/visibility {:code :show :result :hide}}
(def premium-order-view
  "把订单 transducer 与原始订单集合绑定成一个可归约视图."
  (eduction order-report-xf orders))

^{::clerk/visibility {:code :show :result :show}}
{:预览前两条 (into [] (take 2) premium-order-view)
 :总金额 (transduce (map :amount) + 0 premium-order-view)}

;; `eduction` 不是缓存后的序列, 而是可再次消费的 reducible 视图.

;; ## 7. 自己写一个 transducer
;;
;; 自定义 transducer 的模板通常长这样:
;;
;; - `([] ...)` 初始化;
;; - `([result] ...)` 收尾;
;; - `([result input] ...)` 处理单个输入.

^{::clerk/visibility {:code :show :result :hide}}
(defn sample-every
  "每隔 n 个输入保留一个元素, 用于演示自定义 transducer 的状态处理."
  [n]
  (fn [rf]
    (let [index* (volatile! -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [index (vswap! index* inc)]
           (if (zero? (mod index n))
             (rf result input)
             result)))))))

^{::clerk/visibility {:code :show :result :show}}
(into [] (sample-every 3) (range 1 16))

;; 状态是允许存在的, 只是它属于这一次归约过程, 不会逃逸成外部共享状态.

;; ## 8. 借助 `reduced` 提前结束处理
;;
;; 这类场景是 transducer 相比普通 `map/filter` 更容易扩展的地方:
;; 终止条件可以依赖累计状态, 而不只是固定的 `take n`.

^{::clerk/visibility {:code :show :result :hide}}
(defn take-until-total
  "累计订单金额达到 limit 后立即终止归约, 并保留触发阈值的那条订单."
  [limit]
  (fn [rf]
    (let [running-total* (volatile! 0)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result order]
         (let [next-total (+ @running-total* (:amount order))
               enriched-order (assoc order :running-total next-total)
               next-result (rf result enriched-order)]
           (vreset! running-total* next-total)
           (if (>= next-total limit)
             (ensure-reduced next-result)
             next-result)))))))

^{::clerk/visibility {:code :show :result :show}}
(into []
      (comp order-report-xf
            (take-until-total 780))
      orders)

;; ## 9. `transduce` 的另一个强项: 直接产出汇总结构

^{::clerk/visibility {:code :show :result :hide}}
(defn order-summary-rf
  "把订单流直接归约成一个汇总视图."
  ([] {:count 0
       :total-amount 0
       :customers #{}
       :regions {}})
  ([acc]
   (update acc :customers #(vec (sort %))))
  ([acc {:keys [customer amount region]}]
   (-> acc
       (update :count inc)
       (update :total-amount + amount)
       (update :customers conj customer)
       (update-in [:regions region] (fnil inc 0)))))

^{::clerk/visibility {:code :show :result :show}}
(transduce order-report-xf order-summary-rf orders)

;; 这里没有先 `into []`, 再 `reduce` 一遍.
;; `transduce` 直接把处理流程和归约目标粘在一起.

;; ## 10. 性能对比: 惰性序列 vs transducer vs reducers
;;
;; 下面的基准只用于观察趋势, 不是严格论文级 benchmark.
;;
;; 重点观察三件事:
;;
;; - 惰性序列版本最直观, 但会经过多层 seq 管道;
;; - transducer 版本通常更省中间层开销;
;; - reducers 在 vector 上使用 `r/fold` 时, 可能因为可切分/并行而更快.

^{::clerk/visibility {:code :show :result :hide}}
(def benchmark-size
  "默认 benchmark 数据规模, 控制在 notebook 可接受的加载成本内."
  500000)

^{::clerk/visibility {:code :show :result :hide}}
(def benchmark-data
  "性能对比使用 vector, 方便 reducers/fold 发挥优势."
  (vec (range benchmark-size)))

^{::clerk/visibility {:code :show :result :hide}}
(def benchmark-xf
  "统一的处理逻辑: 过滤奇数、求平方、排除 7 的倍数."
  (comp
   (filter odd?)
   (map #(* % %))
   (filter #(not (zero? (mod % 7))))))

^{::clerk/visibility {:code :show :result :hide}}
(defn lazy-seq-sum
  "惰性序列版本: 通过 seq 管道再归约."
  [xs]
  (->> xs
       (filter odd?)
       (map #(* % %))
       (remove #(zero? (mod % 7)))
       (reduce + 0)))

^{::clerk/visibility {:code :show :result :hide}}
(defn transducer-sum
  "transducer 版本: 直接 transduce."
  [xs]
  (transduce benchmark-xf + 0 xs))

^{::clerk/visibility {:code :show :result :hide}}
(defn reducers-reduce-sum
  "reducers 版本: 保留 reducible 管道, 用 `r/reduce` 结束."
  [xs]
  (->> xs
       (r/filter odd?)
       (r/map #(* % %))
       (r/filter #(not (zero? (mod % 7))))
       (r/reduce + 0)))

^{::clerk/visibility {:code :show :result :hide}}
(defn reducers-fold-sum
  "reducers 版本: 在 vector 上用 `r/fold` 汇总."
  [xs]
  (->> xs
       (r/filter odd?)
       (r/map #(* % %))
       (r/filter #(not (zero? (mod % 7))))
       (r/fold +)))

^{::clerk/visibility {:code :show :result :hide}}
(defn benchmark-once
  "执行一次计时, 返回结果和毫秒数."
  [f]
  (let [start (System/nanoTime)
        result (f)
        elapsed-ms (/ (- (System/nanoTime) start) 1000000.0)]
    {:result result
     :ms elapsed-ms}))

^{::clerk/visibility {:code :show :result :hide}}
(defn benchmark-entry
  "对一个方案做 warmup 和多次采样, 返回平均/最小/最大耗时."
  [label f warmup runs]
  (dotimes [_ warmup]
    (f))
  (let [measurements (vec (repeatedly runs #(benchmark-once f)))
        milliseconds (mapv :ms measurements)]
    {:方案 label
     :结果 (:result (first measurements))
     :平均毫秒 (/ (reduce + milliseconds) runs)
     :最小毫秒 (apply min milliseconds)
     :最大毫秒 (apply max milliseconds)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn format-ms
  "把毫秒值格式化为便于阅读的字符串."
  [value]
  (format "%.2f ms" (double value)))

^{::clerk/visibility {:code :show :result :hide}}
(def benchmark-results
  "基于同一份 benchmark 数据对比四种方案."
  (let [warmup 2
        runs 4
        raw-results [(benchmark-entry "惰性序列 + reduce" #(lazy-seq-sum benchmark-data) warmup runs)
                     (benchmark-entry "transduce" #(transducer-sum benchmark-data) warmup runs)
                     (benchmark-entry "reducers + r/reduce" #(reducers-reduce-sum benchmark-data) warmup runs)
                     (benchmark-entry "reducers + r/fold" #(reducers-fold-sum benchmark-data) warmup runs)]
        baseline (-> raw-results first :平均毫秒)]
    (mapv (fn [{:keys [方案 结果 平均毫秒 最小毫秒 最大毫秒]}]
            {:方案 方案
             :结果 结果
             :平均耗时 (format-ms 平均毫秒)
             :最小耗时 (format-ms 最小毫秒)
             :最大耗时 (format-ms 最大毫秒)
             :相对惰性序列 (format "%.2fx" (/ baseline 平均毫秒))})
          raw-results)))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table benchmark-results)

;; 解释 benchmark 时, 最好带着几个前提看:
;;
;; - 这里的绝对数值依赖当前 JVM、CPU 核数、是否已经 JIT 预热;
;; - `reducers + r/fold` 的优势通常要求输入像 vector 一样可高效切分;
;; - 如果输入是流式来源、一次一条到达, transducer 往往更通用;
;; - 如果只是普通业务代码里的小管道, 先选择最清楚的写法, 再按需优化.

;; ## 11. 最后怎么选
;;
;; 可以把今天的内容压缩成下面几条经验:
;;
;; - 惰性序列: 最容易读, 适合普通数据变换与按需消费;
;; - transducer: 最通用, 适合把逐元素处理逻辑复用到不同出口;
;; - reducers: 更偏批量归约, 在 vector + `r/fold` 这类场景可能很强;
;; - 性能判断不要凭感觉, 最好像本页一样在当前数据规模下测一遍.
