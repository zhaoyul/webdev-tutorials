^{:nextjournal.clerk/visibility {:code :hide}}
(ns coffi.sqlite_deep_dive
  "Coffi + SQLite 深入绑定."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [coffi.ffi :as ffi]
            [coffi.mem :as mem]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "# Coffi 与 SQLite 深入绑定\n\n本笔记演示如何使用 Coffi 绑定 SQLite C API, 并基于 `chinook.db` 执行一次只读查询.\n\n前置条件:\n\n- JDK 22 或更高版本.\n- JVM 启动参数包含 `--enable-native-access ALL-UNNAMED`.\n- 本机可以找到 SQLite 动态库.")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 1. 加载 SQLite 动态库\n\n和基础篇一致, 先加载 SQLite 动态库, 再定义绑定函数. 如果加载失败, 请设置 `SQLITE3_LIB_PATH` 后重新运行该单元.")

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

^{::clerk/visibility {:code :show :result :hide}}
(defn sqlite-loaded?
  "判断 SQLite 动态库是否已加载."
  []
  (= :loaded (:status (sqlite-runtime!))))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 2. 绑定关键函数\n\n这里使用 `cfn` 手动创建函数表, 方便统一管理与复用.")

^{::clerk/visibility {:code :show :result :hide}}
(defonce sqlite-fns
  (delay
    (when (sqlite-loaded?)
      {:open-v2 (ffi/cfn "sqlite3_open_v2"
                         [::mem/c-string ::mem/pointer ::mem/int ::mem/c-string]
                         ::mem/int)
       :close-v2 (ffi/cfn "sqlite3_close_v2"
                          [::mem/pointer]
                          ::mem/int)
       :errmsg (ffi/cfn "sqlite3_errmsg"
                        [::mem/pointer]
                        ::mem/c-string)
       :prepare-v2 (ffi/cfn "sqlite3_prepare_v2"
                            [::mem/pointer ::mem/c-string ::mem/int ::mem/pointer ::mem/pointer]
                            ::mem/int)
       :step (ffi/cfn "sqlite3_step"
                      [::mem/pointer]
                      ::mem/int)
       :finalize (ffi/cfn "sqlite3_finalize"
                          [::mem/pointer]
                          ::mem/int)
       :column-int (ffi/cfn "sqlite3_column_int"
                            [::mem/pointer ::mem/int]
                            ::mem/int)
       :column-text (ffi/cfn "sqlite3_column_text"
                             [::mem/pointer ::mem/int]
                             ::mem/c-string)})))

^{::clerk/visibility {:code :show :result :hide}}
(defn sqlite-fns*
  "返回已绑定的 SQLite 函数表."
  []
  (force sqlite-fns))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 3. 常用常量\n\nSQLite 的返回码与打开标志均来自 C 头文件, 这里保留最常用的一组.")

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_OK 0)

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_ROW 100)

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_DONE 101)

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_OPEN_READONLY 0x00000001)

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_OPEN_READWRITE 0x00000002)

^{::clerk/visibility {:code :show :result :hide}}
(def SQLITE_OPEN_CREATE 0x00000004)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 4. 打开数据库与错误处理\n\n`sqlite3_open_v2` 使用 `sqlite3**` 输出参数, 需要显式分配一个指针槽位并读取其中的结果.")

^{::clerk/visibility {:code :show :result :hide}}
(defn sqlite-errmsg
  "读取 SQLite 错误信息."
  [db]
  (when-let [f (:errmsg (sqlite-fns*))]
    (f db)))

^{::clerk/visibility {:code :show :result :hide}}
(defn open-db
  "打开数据库, 返回包含状态与句柄的结果."
  [db-path]
  (if-let [fns (sqlite-fns*)]
    (let [out (mem/alloc-instance ::mem/pointer)
          flags SQLITE_OPEN_READONLY
          rc ((:open-v2 fns) db-path out flags nil)
          db (mem/read-address out)]
      (if (= SQLITE_OK rc)
        {:status :ok
         :db db
         :path db-path}
        {:status :error
         :code rc
         :message (sqlite-errmsg db)
         :path db-path}))
    {:status :missing
     :runtime (sqlite-runtime!)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn close-db
  "关闭数据库句柄."
  [db]
  (if-let [fns (sqlite-fns*)]
    (let [rc ((:close-v2 fns) db)]
      (if (= SQLITE_OK rc)
        {:status :ok}
        {:status :error
         :code rc
         :message (sqlite-errmsg db)}))
    {:status :missing
     :runtime (sqlite-runtime!)}))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 5. 编译与执行 SQL\n\n本节演示 `sqlite3_prepare_v2` 与 `sqlite3_step` 的基本流程. 其中 `sqlite3_column_text` 会返回 UTF-8 字符串指针, 这里直接映射为 `::mem/c-string`.")

^{::clerk/visibility {:code :show :result :hide}}
(defn prepare-statement
  "编译 SQL 并返回 statement 句柄."
  [db sql]
  (if-let [fns (sqlite-fns*)]
    (let [out (mem/alloc-instance ::mem/pointer)
          rc ((:prepare-v2 fns) db sql -1 out mem/null)
          stmt (mem/read-address out)]
      (if (= SQLITE_OK rc)
        {:status :ok
         :stmt stmt}
        {:status :error
         :code rc
         :message (sqlite-errmsg db)}))
    {:status :missing
     :runtime (sqlite-runtime!)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn fetch-rows
  "执行查询并读取结果."
  [stmt]
  (if-let [fns (sqlite-fns*)]
    (loop [rows []]
      (let [rc ((:step fns) stmt)]
        (cond
          (= SQLITE_ROW rc)
          (recur (conj rows {:artist-id ((:column-int fns) stmt 0)
                             :name ((:column-text fns) stmt 1)}))

          (= SQLITE_DONE rc)
          {:status :ok
           :rows rows}

          :else
          {:status :error
           :code rc})))
    {:status :missing
     :runtime (sqlite-runtime!)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn finalize-statement
  "释放 statement 句柄."
  [stmt]
  (when-let [fns (sqlite-fns*)]
    ((:finalize fns) stmt)))

^{::clerk/visibility {:code :show :result :hide}}
(defn query-artists
  "查询示例数据, 返回前几条艺术家信息."
  [db]
  (let [sql "select ArtistId, Name from artists order by ArtistId limit 5"
        prepared (prepare-statement db sql)]
    (if (= :ok (:status prepared))
      (try
        (fetch-rows (:stmt prepared))
        (finally
          (finalize-statement (:stmt prepared))))
      prepared)))

^{::clerk/visibility {:code :show :result :hide}}
(defn chinook-path
  "返回 chinook.db 的绝对路径."
  []
  (.getAbsolutePath (io/file "chinook.db")))

^{::clerk/visibility {:code :show :result :show}}
(when (sqlite-loaded?)
  (let [opened (open-db (chinook-path))]
    (if (= :ok (:status opened))
      (try
        (query-artists (:db opened))
        (finally
          (close-db (:db opened))))
      opened)))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "## 6. 延伸建议\n\n- 如果需要绑定更多函数, 建议把函数表抽到单独命名空间统一管理.\n- 当涉及结构体或回调, 可以使用 `coffi.mem/defstruct` 与 `coffi.ffi/defcfn` 的包装能力.")
