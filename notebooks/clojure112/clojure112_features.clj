(ns notebooks.clojure112_features
  (:require [nextjournal.clerk :as clerk]))

(set! *warn-on-reflection* true)

;; Clojure 1.12 对 Java 互操作性的增强主要集中在以下几个方面:
;;
;; ## 方法值(method value)

;; Clojure中的函数可以很容易地传递, 一个常见的问题是如何将Java的静态方法(或实例方法, 构造函数)去做同样的map/filter/reduce.

;; 在1.11以及以前版本以前, 我们必须手动将这些方法包装成函数.
;; 这种做法往往比较繁琐, 有时还需要手动指定类型以解决方法重载的问题, 或者可能会引起不必要的反射或装箱操作.

;; 在最新的Clojure 1.12版本中, 程序员现在可以直接将Java的限定方法作为普通函数在值上下文中使用--编译器将自动生成包装函数.
;; 新加入的功能是: 当一个限定方法因为重载而无法解析时, 编译器将生成一个反射调用.
;; 开发者可以在限定方法上提供`:param-tags`元数据, 以指定单个所需方法的签名, 从而解决这个问题.

;; 如果在限定方法上没有提供`:param-tags`元数据, 且该方法因重载而无法解析, 新版本中编译器同样会生成一个反射调用.
;; 这一改进极大简化了在Clojure中使用Java方法的复杂度, 提高了代码的简洁性和执行效率.

;; 静态方法
(map #(Long/toBinaryString  %) (range 8))

;; 实例方法(toUpperCase函数有重载, 此处有反射).
(map #(.toUpperCase %) ["hi" "there"])

;; 构造函数(此处有反射).
(map #(java.util.Date. %) [1707771694522 1707771780922])
;; (#inst "2024-02-12T21:01:34.522-00:00" #inst "2024-02-12T21:03:00.922-00:00")
;;
;; 在此版本中, Clojure 允许直接引用 Java 方法作为 Clojure 值, 这简化了从 Clojure 调用 Java 方法的语法. 例如, 你现在可以直接将 `String/length` 作为函数引用, 而不需要通过额外的封装来调用:

;; 静态函数可以直接作为值来传递

(map Long/toBinaryString (range 8))
;; `:param-tags`指定为不需要参数的重载函数.
(map ^[]  String/.toUpperCase ["hi" "there"])
;; `:param-tags`指定为`long`类型.
(map ^[long] java.util.Date/new [1707771694522 1707771780922])



;; 更多举例
(map String/.length ["Clojure", "Java", "Interop"])

(map String/.toUpperCase ["hi" "there"])

(map ^[long] Math/abs [-1 0 3])


;; ## 在clojure tools.deps中增加动态依赖的函数


;; Clojure 1.12引入了在运行时动态添加依赖的功能，使得开发者可以在不重启REPL的情况下添加新的库和依赖。
;; 注意: 这些功能需要额外的依赖库 `org.clojure/tools.deps.alpha`。

;; - `add-libs`
;; - `add-lib`
;; - `sync-deps`

;; `add-libs` - 添加多个库依赖
;;
;; 注意: 需要先添加tools.deps.alpha依赖才能使用这些函数
;; 在deps.edn中添加: org.clojure/tools.deps.alpha {:mvn/version "0.12.13"}
;;
;; 在REPL中，我们可以使用`add-libs`来动态添加依赖库。例如，如果我们需要使用cheshire库来处理JSON：
(comment
  ;; 首先需要添加tools.deps.alpha依赖 (通常在deps.edn中配置)
  (require '[clojure.tools.deps.alpha.repl :refer [add-libs]])
  (add-libs '{cheshire/cheshire {:mvn/version "5.11.0"}})
  (require '[cheshire.core :as json])
  (json/encode {:name "Clojure" :version "1.12"}))

;; `add-lib` - 添加单个库依赖
;;
;; 如果我们只需要添加单个库，可以使用`add-lib`：
(comment
  (require '[clojure.tools.deps.alpha.repl :refer [add-lib]])
  (add-lib 'metosin/reitit {:mvn/version "0.5.18"})
  (require '[reitit.core :as r]))

;; `sync-deps` - 同步依赖，确保所有依赖都已加载
;;
;; 在某些情况下，我们可能需要确保所有依赖都已正确加载和同步：
(comment
  (require '[clojure.tools.deps.alpha.repl :refer [sync-deps]])
  (sync-deps))

;; 这些功能大大提升了开发体验，特别是在原型设计和实验阶段，开发者无需重启REPL就可以快速引入新的库。

;; ## 更高效的vector分组函数

;; Clojure 1.12引入了新的基于vector的分组函数，这些函数比原有的基于seq的函数更高效，
;; 因为它们直接返回向量而不是惰性序列，避免了重复计算和转换开销。
;; 这些函数特别适用于需要多次访问结果或对结果进行随机访问的场景。

;; - `partitionv` - 将序列分割成指定大小的向量块
;; - `partitionv-all` - 和partitionv类似，但会保留不完整的末尾块
;; - `splitv-at` - 在指定位置分割向量

;; `partitionv` - 将序列分割成指定大小的向量块
;;
;; 与partition类似，但返回向量而不是序列：
(partitionv 3 (range 10))
;; => [[0 1 2] [3 4 5] [6 7 8]]

;; `partitionv-all` - 不丢弃不完整的末尾块
;;
;; 与partition-all类似，但返回向量：
(partitionv-all 3 (range 10))
;; => [[0 1 2] [3 4 5] [6 7 8] [9]]

;; `splitv-at` - 在指定位置分割向量
;;
;; 将向量在指定位置分割成两部分：
(splitv-at 3 [1 2 3 4 5 6])
;; => [[1 2 3] [4 5 6]]

;; 性能优势示例
;;
;; 这些函数返回向量，可以直接通过索引访问元素，比基于seq的函数更高效：
(def pv (partitionv 2 (range 10)))
(println "First partition:" (first pv))
;; => First partition: [0 1]

;; 由于返回的是向量，我们可以高效地随机访问元素：
(nth pv 2)
;; => [4 5]

;; 这些函数在需要多次访问结果或对结果进行进一步处理的场景中特别有用。

;; ## 更简洁的多维数组
;; Clojure此前对数组的类型提示的方式是Java内部类名
;; - `[Ljava.lang.String;` - 一维字符串数组
;; - `[[D` - 2D 原始double类型数组
;; 这种写法非常之繁琐, 可读性不高, 且容易出错, 因为需要写完整的类名, 且很容易出错.

;; 在1.12版本以后, 我们可以这么写 `ComponentClass/#dimensions`

;; - java.lang.String/1 or String/1 - 一维字符串数组
;; - double/2 - 二维原始double数组.


;; 多种维度的字符串数组
(make-array String/1 100)
(make-array String/3 100)

;; 类型提示
(defn process-array [^String/1 arr]
  (map str arr))

;; ### 和java的stream深度互动
;; Java API越来越多的采用stream来处理异步, 我们需要有一个更高效的方法来和java的strea来做interop

;; 函数`stream-seq!`, `stream-reduce!`, `stream-transduce!`, 和 `stream-into!` 被引入用来和streanm做交互


(import [java.util.stream Stream])

;; `stream-seq!`
(->> (.stream (range 10))
     (stream-seq!)
     (map inc))


;; `stream-reduce`
(->> (range 10)
     (.stream)
     (stream-reduce! +))

;; `stream-transduce!`
(def xf (comp (filter even?) (map #(* % 2))))
(->> (.stream (range 10))
     (stream-transduce! xf + 0))

;; `stream-into!`
(->> (.stream (range 10))
     (stream-into! []))

;; `stream-into!`支持`transducer`
(def xf (comp (map +) (filter even?)))
(->> (.stream (range 10))
     (stream-into! [] xf))


;; ### 参考资料

;; https://insideclojure.org/2022/06/15/partitioning/
;; https://insideclojure.org/2024/02/12/method-values/




(defonce _a (clerk/serve! {:browse? true}))
(clerk/show! "notebooks/clojure112/clojure112_features.clj")
(comment
  (clerk/clear-cache!)
  (clerk/build! {:paths ["clojrue112_features.clj"]})
  (prof/serve-ui 9999)
  (prof/start)
  (prof/stop)
  )
