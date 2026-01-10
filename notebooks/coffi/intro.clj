^{:nextjournal.clerk/visibility {:code :hide}}
(ns coffi.intro
  "Coffi 基础介绍."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [coffi.ffi :as ffi]
            [coffi.mem :as mem]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# Coffi 基础介绍\n\n这份笔记聚焦 Coffi 的基础概念: 如何加载动态库, 如何绑定 C 函数, 以及如何处理最常见的参数类型.\n\n前置条件:\n\n- JDK 22 或更高版本.\n- JVM 需要开启 `--enable-native-access ALL-UNNAMED`.\n- 本机可找到 `libsqlite3` 动态库.")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 1. 发现并加载 SQLite 动态库\n\n优先通过 `SQLITE3_LIB_PATH` 指定动态库路径, 否则按平台常见路径尝试. 这个步骤成功后才能绑定后续函数.")

^{::clerk/visibility {:code :show :result :hide}}
(defn sqlite-library-candidates
  "返回可能的 SQLite 动态库候选列表."
  []
  (let [os-name (System/getProperty "os.name")
        env-path (some-> (System/getenv "SQLITE3_LIB_PATH") str/trim not-empty)
        platform-paths (cond
                         (re-find #"Mac" os-name)
                         ["/usr/lib/libsqlite3.dylib"
                          "/opt/homebrew/opt/sqlite/lib/libsqlite3.dylib"
                          "/usr/local/opt/sqlite/lib/libsqlite3.dylib"]

                         (re-find #"Linux" os-name)
                         ["/usr/lib/x86_64-linux-gnu/libsqlite3.so"
                          "/usr/lib/libsqlite3.so"
                          "/lib/x86_64-linux-gnu/libsqlite3.so"]

                         (re-find #"Windows" os-name)
                         ["sqlite3.dll"]

                         :else
                         [])
        system-names ["sqlite3"]]
    (->> (concat
          (when env-path [{:type :path :value env-path :source :env}])
          (map (fn [path] {:type :path :value path :source :platform}) platform-paths)
          (map (fn [name] {:type :system :value name :source :system}) system-names))
         (remove nil?)
         vec)))

^{::clerk/visibility {:code :show :result :hide}}
(defn- try-load-sqlite
  "尝试加载单个 SQLite 动态库候选项."
  [{:keys [type value] :as candidate}]
  (try
    (case type
      :path (ffi/load-library (io/file value))
      :system (ffi/load-system-library value))
    (assoc candidate :ok? true)
    (catch Throwable t
      (assoc candidate :ok? false :error (.getMessage t)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn load-sqlite!
  "加载 SQLite 动态库并返回结果描述."
  []
  (loop [remaining (sqlite-library-candidates)
         attempts []]
    (if-let [candidate (first remaining)]
      (let [result (try-load-sqlite candidate)
            attempts' (conj attempts result)]
        (if (:ok? result)
          {:status :loaded
           :source (dissoc result :ok?)
           :attempts attempts'}
          (recur (rest remaining) attempts')))
      {:status :missing
       :attempts attempts
       :hint "请设置 SQLITE3_LIB_PATH 指向 libsqlite3, 然后重新运行加载步骤."})))

^{::clerk/visibility {:code :show :result :show}}
(defonce sqlite-runtime (atom nil))

^{::clerk/visibility {:code :show :result :show}}
(defn sqlite-runtime!
  "返回当前 SQLite 加载状态, 如有需要会尝试加载."
  []
  (or @sqlite-runtime
      (reset! sqlite-runtime (load-sqlite!))))

^{::clerk/visibility {:code :show :result :show}}
(sqlite-runtime!)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 2. 绑定最小函数集\n\n这里用 `sqlite3_libversion` 系列函数演示 `defcfn` 的基本写法. 该函数无需创建数据库连接, 只要动态库已加载即可调用.")

^{::clerk/visibility {:code :show :result :hide}}
(defn sqlite-loaded?
  "判断 SQLite 动态库是否已加载."
  []
  (= :loaded (:status (sqlite-runtime!))))

^{::clerk/visibility {:code :show :result :hide}}
(when (sqlite-loaded?)
  (ffi/defcfn sqlite3-libversion
    "读取 SQLite 运行时版本字符串."
    "sqlite3_libversion"
    []
    ::mem/c-string)
  (ffi/defcfn sqlite3-libversion-number
    "读取 SQLite 运行时版本号."
    "sqlite3_libversion_number"
    []
    ::mem/int)
  (ffi/defcfn sqlite3-sourceid
    "读取 SQLite 源码标识."
    "sqlite3_sourceid"
    []
    ::mem/c-string))

^{::clerk/visibility {:code :show :result :show}}
(defn sqlite-version
  "返回 SQLite 运行时版本信息."
  []
  (if-not (sqlite-loaded?)
    {:status :missing
     :runtime (sqlite-runtime!)}
    (let [libversion (ns-resolve *ns* 'sqlite3-libversion)
          version-number (ns-resolve *ns* 'sqlite3-libversion-number)
          sourceid (ns-resolve *ns* 'sqlite3-sourceid)]
      (if (and libversion version-number sourceid)
        {:status :ok
         :version (libversion)
         :version-number (version-number)
         :source-id (sourceid)}
        {:status :missing
         :runtime (sqlite-runtime!)}))))

^{::clerk/visibility {:code :show :result :show}}
(sqlite-version)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 3. 下一步\n\n- 在更复杂的绑定中, 需要处理 `sqlite3*` 等指针类型与错误码.\n- 下一份笔记将演示 `sqlite3_open_v2` / `sqlite3_prepare_v2` / `sqlite3_step` 等函数, 并给出查询 `chinook.db` 的完整示例.")
