^{:nextjournal.clerk/visibility {:code :hide}}
(ns coffi.api_guide
  "Coffi API 使用总览."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [coffi.ffi :as ffi]
            [coffi.mem :as mem]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# Coffi API 使用总览\n\n这份笔记完整展示 Coffi 的核心能力: 类型映射, 函数加载与调用, 以及 callback 中传入 Clojure 函数.\n\n前置条件:\n\n- JDK 22 或更高版本.\n- JVM 启动参数包含 `--enable-native-access ALL-UNNAMED`.\n- 本机可找到标准 C 动态库.")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 1. 加载标准 C 动态库\n\n优先使用 `LIBC_LIB_PATH` 指定动态库路径, 否则按平台常见路径尝试.")

^{::clerk/visibility {:code :show :result :hide}}
(defn libc-library-candidates
  "返回可能的 libc 动态库候选列表."
  []
  (let [os-name (System/getProperty "os.name")
        env-path (some-> (System/getenv "LIBC_LIB_PATH") str/trim not-empty)
        platform-paths (cond
                         (re-find #"Mac" os-name)
                         ["/usr/lib/libSystem.B.dylib"
                          "/usr/lib/libc.dylib"]

                         (re-find #"Linux" os-name)
                         ["/lib/x86_64-linux-gnu/libc.so.6"
                          "/usr/lib/x86_64-linux-gnu/libc.so.6"
                          "/usr/lib/libc.so.6"]

                         (re-find #"Windows" os-name)
                         ["msvcrt.dll"
                          "ucrtbase.dll"]

                         :else
                         [])
        system-names ["c" "System" "msvcrt"]]
    (->> (concat
          (when env-path [{:type :path :value env-path :source :env}])
          (map (fn [path] {:type :path :value path :source :platform}) platform-paths)
          (map (fn [name] {:type :system :value name :source :system}) system-names))
         (remove nil?)
         vec)))

^{::clerk/visibility {:code :show :result :hide}}
(defn- try-load-libc
  "尝试加载单个 libc 动态库候选项."
  [{:keys [type value] :as candidate}]
  (try
    (case type
      :path (ffi/load-library (io/file value))
      :system (ffi/load-system-library value))
    (assoc candidate :ok? true)
    (catch Throwable t
      (assoc candidate :ok? false :error (.getMessage t)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn load-libc!
  "加载 libc 动态库并返回结果描述."
  []
  (loop [remaining (libc-library-candidates)
         attempts []]
    (if-let [candidate (first remaining)]
      (let [result (try-load-libc candidate)
            attempts' (conj attempts result)]
        (if (:ok? result)
          {:status :loaded
           :source (dissoc result :ok?)
           :attempts attempts'}
          (recur (rest remaining) attempts')))
      {:status :missing
       :attempts attempts
       :hint "请设置 LIBC_LIB_PATH 指向 libc, 然后重新运行加载步骤."})))

^{::clerk/visibility {:code :show :result :show}}
(defonce libc-runtime (atom nil))

^{::clerk/visibility {:code :show :result :show}}
(defn libc-runtime!
  "返回当前 libc 加载状态, 如有需要会尝试加载."
  []
  (or @libc-runtime
      (reset! libc-runtime (load-libc!))))

^{::clerk/visibility {:code :show :result :show}}
(defn libc-loaded?
  "判断 libc 是否已加载."
  []
  (= :loaded (:status (libc-runtime!))))

^{::clerk/visibility {:code :show :result :show}}
(libc-runtime!)

;; ## 2. 类型映射速览
;;
;; 下面是常见 C 类型与 Coffi 类型的对应关系, 可作为后续示例的速查表.

;; | C 类型 | Coffi 类型 |
;; | --- | --- |
;; | int | ::mem/int |
;; | long | ::mem/long |
;; | double | ::mem/double |
;; | char* | ::mem/c-string |
;; | void* | ::mem/pointer |
;; | size_t | ::size-t |
;; | int[4] | [:coffi.mem/array ::mem/int 4] |
;; | struct | mem/defstruct |n
;; | callback | [:coffi.ffi/fn [arg-types] ret-type] |


;; ## 3. 基础类型与内存读写
;; 用 `mem/alloc-instance` 申请一块内存, 再用 `mem/read-*` 与 `mem/write-*` 读写.

^{::clerk/visibility {:code :show :result :show}}
(defn primitive-roundtrip
  "演示 int 类型的读写."
  []
  (with-open [arena (mem/confined-arena)]
    (let [segment (mem/alloc-instance ::mem/int arena)]
      (mem/write-int segment 42)
      {:raw (mem/read-int segment)
       :via-deserialize (mem/deserialize segment ::mem/int)})))

^{::clerk/visibility {:code :show :result :show}}
(primitive-roundtrip)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 4. 数组与指针\n\n`[:coffi.mem/array ::mem/int n]` 会被序列化为一段连续内存, 再用同样的类型反序列化回向量.")

^{::clerk/visibility {:code :show :result :show}}
(defn array-roundtrip
  "演示数组类型的序列化与反序列化."
  [values]
  (with-open [arena (mem/confined-arena)]
    (let [values (vec values)
          type [::mem/array ::mem/int (count values)]
          segment (mem/serialize values type arena)]
      {:segment-size (mem/size-of type)
       :roundtrip (mem/deserialize segment type)
       :first-via-pointer (mem/deserialize segment [::mem/pointer ::mem/int])})))

^{::clerk/visibility {:code :show :result :show}}
(array-roundtrip [8 3 5 1])

;; ## 5. 自定义 structnn使用 `mem/defstruct` 定义 C 结构体, Coffi 会自动生成序列化与反序列化逻辑.

^{::clerk/visibility {:code :show :result :hide}}
(mem/defstruct Point [x ::mem/int y ::mem/int])

^{::clerk/visibility {:code :show :result :show}}
(defn struct-roundtrip
  "演示 struct 的序列化与反序列化."
  []
  (with-open [arena (mem/confined-arena)]
    (let [value (->Point 7 9)
          segment (mem/serialize value :coffi.api_guide/Point arena)]
      {:value value
       :roundtrip (mem/deserialize segment :coffi.api_guide/Point)})))

^{::clerk/visibility {:code :show :result :show}}
(struct-roundtrip)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 6. 函数加载与调用\n\n这里用 libc 的 `strlen` 演示两种绑定方式: `defcfn` 与 `make-downcall`.")

^{::clerk/visibility {:code :show :result :hide}}
(mem/defalias ::size-t ::mem/long)

^{::clerk/visibility {:code :show :result :hide}}
(when (libc-loaded?)
  (ffi/defcfn c-strlen
    "读取字符串长度."
    "strlen"
    [::mem/c-string]
    ::size-t))

^{::clerk/visibility {:code :show :result :show}}
(defn strlen-demo
  "使用 defcfn 绑定的 strlen."
  [s]
  (if-let [f (ns-resolve *ns* 'c-strlen)]
    (f s)
    {:status :missing
     :runtime (libc-runtime!)}))

^{::clerk/visibility {:code :show :result :show}}
(strlen-demo "coffi")

^{::clerk/visibility {:code :show :result :show}}
(defn strlen-manual
  "使用 find-symbol + make-downcall 显式绑定 strlen."
  [s]
  (if (libc-loaded?)
    (let [symbol (ffi/find-symbol "strlen")
          downcall (ffi/make-downcall symbol [::mem/c-string] ::size-t)
          strlen (ffi/make-serde-wrapper downcall [::mem/c-string] ::size-t)]
      (strlen s))
    {:status :missing
     :runtime (libc-runtime!)}))

^{::clerk/visibility {:code :show :result :show}}
(strlen-manual "panama")


;; ## 7. callback 绑定

;;下面用 `qsort` 展示如何把 Clojure 函数作为比较器传给 C. 这里将 `size_t` 简化为 `long`, 适合演示.

^{::clerk/visibility {:code :show :result :hide}}
(defonce libc-fns
  (delay
    (when (libc-loaded?)
      {:qsort (ffi/cfn "qsort"
                       [::mem/pointer
                        ::size-t
                        ::size-t
                        [::ffi/fn [::mem/pointer ::mem/pointer] ::mem/int]]
                       ::mem/void)})))

^{::clerk/visibility {:code :show :result :hide}}
(defn libc-fns*
  "返回已绑定的 libc 函数表."
  []
  (force libc-fns))

^{::clerk/visibility {:code :show :result :show}}
(defn qsort-demo
  "使用 C 的 qsort 对 int 向量排序."
  [values]
  (if-let [fns (libc-fns*)]
    (with-open [arena (mem/confined-arena)]
      (let [values (vec values)
            count (count values)
            type [::mem/array ::mem/int count]
            segment (mem/serialize values type arena)
            elem-size (mem/size-of ::mem/int)
            comparator (fn [a b]
                         (compare (mem/deserialize a [::mem/pointer ::mem/int])
                                  (mem/deserialize b [::mem/pointer ::mem/int])))]
        ((:qsort fns) segment count elem-size comparator)
        (mem/deserialize segment type)))
    {:status :missing
     :runtime (libc-runtime!)}))

^{::clerk/visibility {:code :show :result :show}}
(qsort-demo [7 2 9 1 5])


;; ## 8. 小结
;; - `coffi.mem` 负责类型映射与内存操作.
;; - `coffi.ffi` 负责加载函数与 callback.
;; - 真实项目中建议抽象出模块化的函数表和类型定义, 便于管理和复用.
