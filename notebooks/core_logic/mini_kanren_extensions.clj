^#:nextjournal.clerk{:visibility {:code :hide} :toc true}
(ns core_logic.mini_kanren_extensions
  "miniKanren 扩展示例: 程序生成, quine 与简单程序归纳."
  (:require [clojure.core.logic :refer [!= == absento conde fresh membero numbero project run run* symbolo]]
            [nextjournal.clerk :as clerk]))

;; # miniKanren 的基础与扩展场景
;;
;; 真正让 miniKanren 变得很有趣的地方, 往往不是"查一个答案",
;; 而是把"程序"本身也当成搜索对象.
;;
;; 不过在进入程序生成之前, 先要看到 miniKanren 自己这门小语言是怎么工作的:
;; - 用 `==` 做统一;
;; - 用 `!=`、`symbolo`、`numbero`、`absento` 叠加约束;
;; - 用 `fresh` 引入新变量;
;; - 用 `conde` 表达多条搜索分支.
;;
;; 然后这一页再用一个极小的 Clojure 子集演示三个经典方向:
;; - 自动生成一个会得到 `(I love you)` 的 Clojure 程序.
;; - 自动生成 Clojure quine.
;; - "找一个 Clojure 程序, 使它把 X 变成 Y".
;;
;; 为了保持 notebook 可读, 这里不直接实现完整的 relational interpreter,
;; 而是采用"有限候选 Clojure form + 极小解释器 + core.logic 搜索"的方式.
;; 这里返回的搜索结果本身就是 Clojure form, 可以直接当成 Clojure 程序来读.

^{::clerk/visibility {:code :show :result :hide}}
(defn describe-value-type
  "给错误信息提供更易读的值类型描述.
  接受任意值 `value`, 返回描述该值类型的字符串."
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
  "求值一个极小的 Clojure 子集.
  接受表达式 `expr` 和可选环境 `env`, 返回求值结果.
  支持 `quote`、`list`、`cons`、`first`、`second`、`rest` 和单参数 `fn`."
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
  "建立 Clojure 程序在给定输入上运行后得到指定输出的关系.
  `program` 是待执行程序, `input` 会绑定到程序里的 `input`, `output` 是期望结果."
  [program input output]
  (project [program input]
    (== output (tiny-eval program {'input input}))))

^{::clerk/visibility {:code :show :result :hide}}
(defn emit-clojure-code
  "把搜索得到的程序转成更直观的 Clojure 源码字符串.
  接受程序 form `program`, 返回它的字符串表示."
  [program]
  (pr-str program))

;; ## 0. 先看 miniKanren 这门小语言本身
;;
;; 在很多 miniKanren 演讲或 live demo 里, 往往会先从几块最小积木开始:
;; `==`、`!=`、`symbolo`、`numbero`、`absento`、`fresh`、`conde`.
;; 先熟悉这些构件, 再去看"搜索程序"就自然很多.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["构件" "作用"]
  :rows [["==" "统一两个值或结构"]
         ["!=" "声明两个值永远不能相同"]
         ["symbolo / numbero" "给逻辑变量加上类型约束"]
         ["fresh" "引入新的局部逻辑变量"]
         ["conde" "表达多条可能的搜索分支"]
         ["absento" "禁止某个符号出现在结构里"]]})

;; 最基本的操作是统一: 不是"赋值", 而是声明两个结构必须一致.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (== q '(mini kanren)))

;; `!=` 是最小的"反统一"约束: 它会保留搜索空间, 但排除不允许的答案.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q '(mini kanren logic))
  (!= q 'logic))

;; `symbolo` 和 `numbero` 很适合先把候选域定小, 再叠加类型约束.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [sym num]
    (membero sym '(mini kanren 42))
    (symbolo sym)
    (membero num '(oops 7 9))
    (numbero num)
    (== q [sym num])))

;; `fresh` 负责引入中间变量, `conde` 负责声明多条分支.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [op arg]
    (conde
      [(== op 'quote) (== arg 'mini)]
      [(== op 'quote) (== arg 'kanren)])
    (== q (list op arg))))

;; 当结果里出现 `_.0`、`_.1` 这类名字时, 它们不是字面量,
;; 而是"还没被完全约束住"的逻辑变量经过 reify 之后的占位名.
^{::clerk/visibility {:code :show :result :show}}
(run 3 [q]
  (fresh [x y]
    (== q [x y])
    (membero x '(mini kanren logic))))

;; `absento` 可以禁止某个符号出现在任意深度的结构里.
^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (fresh [candidate]
    (membero candidate '((mini kanren) (logic programming) (logic mini)))
    (absento 'kanren candidate)
    (== q candidate)))

;; 上面这些例子展示的, 就是原始 miniKanren 最核心的工作方式:
;; 先写约束, 再让系统枚举并过滤满足约束的答案.
;; 接下来我们只把"答案"从普通数据, 换成"一段 Clojure 程序".

;; ## 1. 自动生成 `(I love you)` 的 Clojure 程序
;;
;; 有了前面的积木之后, 这里就可以把搜索对象升级成"程序"本身.
;; 底层原理并没有变化: 仍然是 `fresh` / `conde` / `==` 一起描述约束,
;; 只不过现在约束的是"什么 Clojure form 算一个合格程序".
;;
;; 如果直接把"完整程序列表"手写出来, 会显得太像在做枚举.
;; 为了更接近 miniKanren 常见的工作方式, 这里改成:
;; - 先定义一些更小的 Clojure form 片段;
;; - 再按几种结构把它们拼成候选程序;
;; - 最后用执行结果约束, 自动筛掉不符合目标的候选.

^{::clerk/visibility {:code :show :result :hide}
  :doc "更接近 miniKanren 风格的 `(I love you)` 程序构造片段."}
(def love-atoms
  {:i '(quote I)
   :love '(quote love)
   :you '(quote you)})

^{::clerk/visibility {:code :show :result :hide}}
(defn love-tailo
  "生成 love 程序里可复用的 tail 片段关系.
  这里有意混入一个错误候选 `(quote (you love))`, 让后面的约束筛选更像 miniKanren 的搜索."
  [tail]
  (conde
    [(== tail '(quote (love you)))]
    [(fresh [love you]
       (== love (:love love-atoms))
       (== you (:you love-atoms))
       (== tail (list 'list love you)))]
    [(fresh [love you]
       (== love (:love love-atoms))
       (== you (:you love-atoms))
       (== tail (list 'cons love
                      (list 'cons you '(quote ())))))]
    [(== tail '(quote (you love)))]))

^{::clerk/visibility {:code :show :result :hide}}
(defn love-programo
  "生成若干候选 Clojure 程序, 再交给后续约束筛选.
  这里既保留成功候选, 也保留失败候选; 末尾那个 `(list love i you)` 分支就是故意保留的失败候选,
  让搜索过程更像原始 miniKanren 的试探方式."
  [program]
  (conde
    [(== program '(quote (I love you)))]
    [(fresh [i love you]
       (== i (:i love-atoms))
       (== love (:love love-atoms))
       (== you (:you love-atoms))
       (== program (list 'list i love you)))]
    [(fresh [i tail]
       (== i (:i love-atoms))
       (love-tailo tail)
       (== program (list 'cons i tail)))]
    [(fresh [love i you]
       (== love (:love love-atoms))
       (== i (:i love-atoms))
       (== you (:you love-atoms))
       (== program (list 'list love i you)))]))

^{::clerk/visibility {:code :show :result :show}}
(mapv emit-clojure-code
      (run* [program]
        (love-programo program)))

;; 上面先展示"搜索空间里都有什么候选".
;; 其中像 `(cons (quote I) (quote (you love)))` 和 `(list (quote love) (quote I) (quote you))`
;; 这样的程序会继续流到下一步, 但会被 `executeso` 的输出约束排除掉,
;; 因为它们的结果分别是 `(I you love)` 和 `(love I you)`, 与目标 `(I love you)` 不符.

^{::clerk/visibility {:code :show :result :show}}
(run* [program]
  (love-programo program)
  (executeso program nil '(I love you)))

^{::clerk/visibility {:code :show :result :show}}
(mapv emit-clojure-code
      (run* [program]
        (love-programo program)
        (executeso program nil '(I love you))))

;; 这一类查询的重点是:
;; 目标不是直接写出完整答案, 而是先描述"允许哪些程序结构"和"什么程序算对",
;; 再让 miniKanren 风格的搜索去试探这些候选, 并自动保留满足约束的结果.

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
  "把 body 放进经典 quine 外壳里.
  接受 `body`, 返回形如 `((fn [x] body) (quote (fn [x] body)))` 的 quine 程序."
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
;; 当前候选集合很小, 但这里仍然用 `delay` 推迟初始化, 避免 namespace 加载时立刻求值; 真正执行 query 时, 再通过 `force` 取出 `[body program]` 向量对.
(def quine-programs
  (delay (build-quine-programs)))

^{::clerk/visibility {:code :show :result :show}}
(run* [q]
  (membero q (force quine-programs)))

^{::clerk/visibility {:code :show :result :show}}
(mapv (fn [[body program]]
        {:body (emit-clojure-code body)
         :program (emit-clojure-code program)})
      (run* [q]
        (membero q (force quine-programs))))

;; 上面的结果会告诉我们:
;; 在这一小组候选 `body` 中,
;; `(list x (list (quote quote) x))` 正好能拼出一个 Clojure quine.

;; ## 3. "找一个 Clojure 程序, 使它把 X 变成 Y"
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

^{::clerk/visibility {:code :show :result :show}}
(let [x '(I (love you))
      y '(I love you)]
  (mapv emit-clojure-code
        (run* [program]
          (membero program transform-programs)
          (executeso program x y))))

;; 这个例子里, 系统会找到:
;; `(cons (first input) (second input))`
;;
;; 它并不是"硬编码的答案",
;; 而是在有限程序空间里, 自动找到能把 X 变成 Y 的那个程序.

;; ## 4. 这三个场景为什么重要

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["场景" "核心想法" "这页 notebook 的做法"]
  :rows [["自动生成目标程序" "把程序本身当成搜索对象" "在有限候选 Clojure form 里搜索能得到 `(I love you)` 的程序"]
         ["自动生成 quine" "让程序结果等于程序自身" "固定 Clojure quine 外壳, 把 `body` 留给搜索"]
         ["找 X -> Y 的程序" "用输入输出样例约束程序" "在小型 Clojure 子集里搜索满足转换条件的程序"]]})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 后续可以怎样继续扩展

如果你后面想把这页继续推向真正的 miniKanren 高级主题, 可以按这个顺序加深:

1. 把 `tiny-eval` 逐步改写成真正 relational 的 `evalo`, 覆盖更完整的 Clojure 子集.
2. 把有限候选模板, 改成按深度递归生成 AST.
3. 不只给一组 `X -> Y`, 而是给多组样例, 让程序搜索更稳定.
4. 再进一步接到 type constraint, 有限域约束或解释器特化.

从教学角度看, 这三个例子很适合帮助读者建立一个直觉:
logic programming 不只是 "查数据", 也可以开始 "查 Clojure 程序".
")
