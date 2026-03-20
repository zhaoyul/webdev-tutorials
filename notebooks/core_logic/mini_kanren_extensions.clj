^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core_logic.mini_kanren_extensions
  "miniKanren 扩展示例: 程序生成, quine 与简单程序归纳."
  (:require [clojure.core.logic :refer [== membero project run*]]
            [nextjournal.clerk :as clerk]))

;; # miniKanren 的三个典型扩展场景
;;
;; 真正让 miniKanren 变得很有趣的地方, 往往不是"查一个答案",
;; 而是把"程序"本身也当成搜索对象.
;;
;; 这一页用一个极小的 Lisp 子语言演示三个经典方向:
;; - 自动生成一个会得到 `(I love you)` 的程序.
;; - 自动生成 quine.
;; - "找一个程序, 使它把 X 变成 Y".
;;
;; 为了保持 notebook 可读, 这里不直接实现完整的 relational interpreter,
;; 而是采用"有限候选程序空间 + 极小解释器 + core.logic 搜索"的方式.

^{::clerk/visibility {:code :show :result :hide}}
(defn describe-value-type
  "给错误信息提供更易读的值类型描述."
  [value]
  (cond
    (nil? value) "nil"
    (map? value) "map"
    (vector? value) "vector"
    (list? value) "list"
    (seq? value) "seq"
    (symbol? value) "symbol"
    (keyword? value) "keyword"
    (string? value) "string"
    (number? value) "number"
    (boolean? value) "boolean"
    :else (or (some-> value type .getSimpleName) "unknown")))

^{::clerk/visibility {:code :show :result :hide}}
(defn tiny-eval
  "求值一个极小的 Lisp 子语言."
  ([expr]
   (tiny-eval expr {}))
  ([expr env]
   (cond
     (or (string? expr)
         (number? expr)
         (keyword? expr)
         (boolean? expr)
         (nil? expr))
     expr

     (symbol? expr)
     (get env expr expr)

     (seq? expr)
     (let [op (first expr)]
       (case op
         quote (second expr)
         list (into '() (reverse (map #(tiny-eval % env) (rest expr))))
         cons (cons (tiny-eval (second expr) env)
                    (tiny-eval (nth expr 2) env))
         first (first (tiny-eval (second expr) env))
         second (second (tiny-eval (second expr) env))
         rest (rest (tiny-eval (second expr) env))
         fn (let [params (second expr)
                  body (nth expr 2 nil)]
              (when-not (and (vector? params)
                             (= 1 (count params))
                             (some? body))
                (throw (ex-info "`fn` 表达式格式错误: 期望 `(fn [param] body)` 形式."
                                {:expr expr})))
              {:tag :closure
               :param (first params)
               :body body
               :env env})
          (let [closure (tiny-eval op env)
                arg (tiny-eval (second expr) env)
                type-desc (describe-value-type closure)]
            (when-not (= :closure (:tag closure))
              (throw (ex-info (str "尝试调用非闭包值, 表达式为: " expr
                                   ", 实际得到类型: " type-desc)
                              {:expr expr
                               :value closure
                               :value-type type-desc})))
            (tiny-eval (:body closure)
                       (assoc (:env closure) (:param closure) arg)))))

     :else expr)))

^{::clerk/visibility {:code :show :result :hide}}
(defn executeso
  "当程序在给定输入上运行后得到 output."
  [program input output]
  (project [program input]
    (== output (tiny-eval program {'input input}))))

;; ## 1. 自动生成 `(I love you)` 的程序
;;
;; 最简单的做法, 是先定义一组很小的程序模板,
;; 然后让 logic search 自动挑出"运行结果刚好等于目标"的候选者.

^{::clerk/visibility {:code :show :result :hide}
  :doc "用于演示自动生成 `(I love you)` 程序的候选模板集合."}
(def love-programs
  ['(quote (I love you))
   '(list (quote I) (quote love) (quote you))
   '(cons (quote I) (quote (love you)))
   '(cons (quote I)
          (cons (quote love)
                (cons (quote you) (quote ()))))])

^{::clerk/visibility {:code :show :result :show}}
(run* [program]
  (membero program love-programs)
  (executeso program nil '(I love you)))

;; 这一类查询的重点是:
;; 目标不是直接写出程序, 而是先描述"什么程序算对",
;; 再让 miniKanren 风格的搜索去找它.

;; ## 2. 自动生成 quine
;;
;; quine 是"输出自己"的程序.
;; 经典思路是先固定一个外层框架:
;; `((fn [x] body) (quote (fn [x] body)))`
;; 然后把 `body` 当成搜索对象.
;; 这里保留了几个会失败的 `body` 候选, 是为了让搜索过程更像"试探 + 过滤",
;; 而不是一开始就把唯一正确答案直接写死.

^{::clerk/visibility {:code :show :result :hide}}
(def quine-bodies
  ['x
   '(list x x)
   '(list x (quote x))
   '(list (quote quote) x)
   '(list x (list (quote quote) x))
   '(cons x (quote ()))])

^{::clerk/visibility {:code :show :result :hide}}
(defn make-quine
  "把 body 放进经典 quine 外壳里."
  [body]
  (let [f `(fn [x] ~body)]
    (list f (list 'quote f))))

^{::clerk/visibility {:code :show :result :hide}}
(defn build-quine-programs
  "构造 quine 搜索结果, 返回 `[body program]` 形式的向量序列.
  这里把实际计算放进单独函数, 方便配合 `delay` 做延迟初始化."
  []
  (vec
   (keep (fn [body]
           (let [program (make-quine body)]
             (when (= program (tiny-eval program))
               [body program])))
         quine-bodies)))

^{::clerk/visibility {:code :show :result :hide}}
;; 这里先把 quine 候选预计算出来, 是为了让后面的 query 更聚焦在"搜索结果长什么样".
;; 当前候选集合很小, 但这里仍然用 `delay` 推迟初始化, 避免 namespace 加载时立刻求值.
;; 这样做的原因是 notebook 初次加载时无需马上执行筛选计算, 只有真正查看 quine 查询结果时才展开.
;; 真正执行 query 时, 再通过 `force` 取出 `[body program]` 向量对.
(def quine-programs
  (delay (build-quine-programs)))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q (force quine-programs)))

;; 上面的结果会告诉我们:
;; 在这一小组候选 `body` 中,
;; `(list x (list (quote quote) x))` 正好能拼出一个 quine.

;; ## 3. "找一个程序, 使它把 X 变成 Y"
;;
;; 这就是程序归纳(program synthesis)里最经典的提问方式:
;; - 给输入样例 X
;; - 给目标输出 Y
;; - 让系统搜索一个满足条件的程序

^{::clerk/visibility {:code :show :result :hide}
  :doc "用于程序归纳示例的候选转换程序集合."}
;; 这里有意保留了一些可复用的小表达式片段,
;; 让读者能看到更小的程序块如何被重新组合成满足 `X -> Y` 的候选程序.
(def transform-programs
  ['input
   '(first input)
   '(second input)
   '(rest input)
   '(first (rest input))
   '(rest (rest input))
   '(cons (first input) (second input))
   '(cons (first input) (first (rest input)))
   '(list (first input) (second input))
   '(cons (quote hello) input)])

^{::clerk/visibility {:code :show :result :show}}
(let [x '(I (love you))
      y '(I love you)]
  (run* [program]
    (membero program transform-programs)
    (executeso program x y)))

;; 这个例子里, 系统会找到:
;; `(cons (first input) (second input))`
;;
;; 它并不是"硬编码的答案",
;; 而是在有限程序空间里, 自动找到能把 X 变成 Y 的那个程序.

;; ## 4. 这三个场景为什么重要

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["场景" "核心想法" "这页 notebook 的做法"]
  :rows [["自动生成目标程序" "把程序本身当成搜索对象" "在有限候选空间里搜索能得到 `(I love you)` 的程序"]
         ["自动生成 quine" "让程序结果等于程序自身" "固定 quine 外壳, 把 `body` 留给搜索"]
         ["找 X -> Y 的程序" "用输入输出样例约束程序" "在小语言里搜索满足转换条件的程序"]]})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 后续可以怎样继续扩展

如果你后面想把这页继续推向真正的 miniKanren 高级主题, 可以按这个顺序加深:

1. 把 `tiny-eval` 逐步改写成真正 relational 的 `evalo`.
2. 把有限候选模板, 改成按深度递归生成 AST.
3. 不只给一组 `X -> Y`, 而是给多组样例, 让程序搜索更稳定.
4. 再进一步接到 type constraint, 有限域约束或解释器特化.

从教学角度看, 这三个例子很适合帮助读者建立一个直觉:
logic programming 不只是'查数据', 也可以开始'查程序'.
")
