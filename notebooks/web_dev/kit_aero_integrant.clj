;; # Kit 基础设施实践: Aero + Integrant

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns web-dev.kit-aero-integrant
  "展示 Kit 风格应用中的集中配置与统一系统状态管理."
  (:require [aero.core :as aero]
            [integrant.core :as ig]
            [nextjournal.clerk :as clerk])
  (:import [java.io StringReader]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md
 "# Kit 应用中的 Aero + Integrant 实战

这份 notebook 聚焦两个目标:

- 用 Aero 管理集中配置, 支持 profile/env/include/自定义 tag.
- 用 Integrant 管理系统状态, 支持依赖图初始化, 局部启动, suspend/resume 热重载.

示例风格贴近 Kit 项目中的 `resources/config.edn + integrant` 思路, 但为了保证 notebook 可独立运行, 这里使用内存中的配置文本模拟文件.")


;; ## 1. Aero 统一配置入口

;; 我们先定义一份根配置, 再通过 `#include` 拆分子配置.
;; 这个模式和 Kit 常见的多文件配置一致, 但更利于 notebook 演示.

^{::clerk/visibility {:code :show :result :hide}}
(def root-config-edn
  "{:app {:name \"kit-demo\"
          :env #profile {:dev :dev :test :test :prod :prod}}
    :logging #include \"logging.edn\"
    :http #include \"http.edn\"
    :db #include \"db.edn\"
    :cache {:ttl-ms #long #or [#env KIT_CACHE_TTL \"30000\"]}
    :features {:tracing? #profile {:dev true :test false :prod true}}
    :secrets {:payment-token #secret \"KIT_PAYMENT_TOKEN\"}}")

^{::clerk/visibility {:code :show :result :hide}}
(def logging-config-edn
  "{:level #profile {:dev :debug :test :warn :prod :info}
    :json? #profile {:dev false :test false :prod true}}")

^{::clerk/visibility {:code :show :result :hide}}
(def http-config-edn
  "{:host #or [#env KIT_HOST \"0.0.0.0\"]
    :port #long #or [#env KIT_HTTP_PORT \"3000\"]
    :join? false}")

^{::clerk/visibility {:code :show :result :hide}}
(def db-config-edn
  "{:jdbc-url #profile {:dev \"jdbc:h2:mem:kit_dev;DB_CLOSE_DELAY=-1\"
                        :test \"jdbc:h2:mem:kit_test;DB_CLOSE_DELAY=-1\"
                        :prod #or [#env KIT_JDBC_URL \"jdbc:postgresql://db/prod\"]}
    :pool-size #long #or [#env KIT_DB_POOL \"12\"]}")

^{::clerk/visibility {:code :show :result :hide}}
(defn ->reader
  "把字符串包装成 StringReader, 方便作为 Aero include 的来源."
  [s]
  (StringReader. s))

^{::clerk/visibility {:code :show :result :hide}}
(def aero-include-sources
  {"logging.edn" logging-config-edn
   "http.edn" http-config-edn
   "db.edn" db-config-edn})

^{::clerk/visibility {:code :show :result :hide}}
(defn in-memory-resolver
  [_ include]
  ;; 每次都返回新的 Reader, 避免 include 流被关闭后无法复用.
  (if-let [content (get aero-include-sources include)]
    (->reader content)
    (->reader (pr-str {:aero/missing-include include}))))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod aero/reader 'secret
  [_ _ env-key]
  ;; 演示用. 真实项目应接入密钥服务, 不要把密钥明文放在配置中.
  (or (System/getenv (str env-key))
      (str "masked://" env-key)))

^{::clerk/visibility {:code :show :result :hide}}
(defn deep-merge
  "递归合并 map. 后者覆盖前者, 用于 overlay override."
  [& ms]
  (letfn [(merge-entry [a b]
            (cond
              (and (map? a) (map? b)) (merge-with merge-entry a b)
              :else b))]
    (reduce merge-entry {} ms)))

^{::clerk/visibility {:code :show :result :hide}}
(defn read-aero-config
  "按 profile 读取 Aero 配置."
  [profile]
  (aero/read-config (->reader root-config-edn)
                    {:profile profile
                     :resolver in-memory-resolver}))

^{::clerk/visibility {:code :show :result :hide}}
(defn load-settings
  "读取 profile 配置并叠加运行时 override."
  ([profile] (load-settings profile {}))
  ([profile overrides]
   (deep-merge (read-aero-config profile) overrides)))

^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true}
(let [dev (load-settings :dev)
      prod (load-settings :prod {:http {:port 18080}})
      pick (fn [m]
             (select-keys m [:app :logging :http :db :cache :features :secrets]))]
  {:dev (pick dev)
   :prod-with-override (pick prod)})

;; 上面的结果对应了集中配置的常见三层:

;; 1. 基础默认值(配置文件).
;; 2. profile 差异(`:dev` / `:prod`).
;; 3. 运行时覆盖(例如 REPL 调试或容器参数注入).


;; ## 2. Integrant 组件图与生命周期


;; 这里构建一个简化的 Kit 组件图:

;; `config -> db/cache/interceptors -> handler -> http-server`

;; 重点展示:

;; - `ig/ref` 声明依赖.
;; - `ig/refset + derive` 聚合同类组件.
;; - `ig/prep-key` 提供默认值.
;; - `ig/init` / `ig/halt!` / `ig/suspend!` / `ig/resume` 生命周期控制.

^{::clerk/visibility {:code :show :result :hide}}
(derive :kit.demo.interceptor/logging :kit.demo/interceptor)

^{::clerk/visibility {:code :show :result :hide}}
(derive :kit.demo.interceptor/tracing :kit.demo/interceptor)

^{::clerk/visibility {:code :show :result :hide}}
(defonce lifecycle-events* (atom []))

^{::clerk/visibility {:code :show :result :hide}}
(defn now-ms [] (System/currentTimeMillis))

^{::clerk/visibility {:code :show :result :hide}}
(defn record-lifecycle!
  [event key data]
  (swap! lifecycle-events*
         conj
         (merge {:ts (now-ms)
                 :event event
                 :key key}
                data)))

^{::clerk/visibility {:code :show :result :hide}}
(defn reset-lifecycle! []
  (reset! lifecycle-events* [])
  :ok)

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/prep-key :kit.demo/cache
  [_ {:keys [ttl-ms] :as opts}]
  (merge {:ttl-ms (or ttl-ms 30000)
          :max-entries 1000}
         opts))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/prep-key :kit.demo/http-server
  [_ opts]
  (merge {:host "0.0.0.0"
          :join? false}
         opts))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo/config
  [_ cfg]
  (record-lifecycle! :init :kit.demo/config {:env (get-in cfg [:app :env])})
  cfg)

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo/config
  [_ cfg]
  (record-lifecycle! :halt :kit.demo/config {:env (get-in cfg [:app :env])}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo/db
  [_ {:keys [jdbc-url pool-size]}]
  (let [conn-id (str "conn-" (random-uuid))]
    (record-lifecycle! :init :kit.demo/db {:jdbc-url jdbc-url
                                           :pool-size pool-size
                                           :conn-id conn-id})
    {:jdbc-url jdbc-url
     :pool-size pool-size
     :conn-id conn-id}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo/db
  [_ db]
  (record-lifecycle! :halt :kit.demo/db {:conn-id (:conn-id db)}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo/cache
  [_ {:keys [ttl-ms max-entries]}]
  (record-lifecycle! :init :kit.demo/cache {:ttl-ms ttl-ms
                                            :max-entries max-entries})
  {:ttl-ms ttl-ms
   :max-entries max-entries
   :store (atom {})})

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo/cache
  [_ cache]
  (reset! (:store cache) {})
  (record-lifecycle! :halt :kit.demo/cache {:max-entries (:max-entries cache)}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo.interceptor/logging
  [_ {:keys [order level]}]
  (record-lifecycle! :init :kit.demo.interceptor/logging {:level level})
  {:name :logging
   :order order
   :level level})

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo.interceptor/logging
  [_ _]
  (record-lifecycle! :halt :kit.demo.interceptor/logging {}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo.interceptor/tracing
  [_ {:keys [order enabled?]}]
  (record-lifecycle! :init :kit.demo.interceptor/tracing {:enabled? enabled?})
  {:name :tracing
   :order order
   :enabled? enabled?})

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo.interceptor/tracing
  [_ _]
  (record-lifecycle! :halt :kit.demo.interceptor/tracing {}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo/handler
  [_ {:keys [app-config db cache interceptors]}]
  (let [ordered (->> interceptors (sort-by :order) vec)]
    (record-lifecycle! :init :kit.demo/handler {:interceptor-count (count ordered)})
    {:db db
     :cache cache
     :interceptors ordered
     :handle (fn [request]
               {:status 200
                :body {:app (get-in app-config [:app :name])
                       :env (get-in app-config [:app :env])
                       :path (:uri request)
                       :interceptors (mapv :name ordered)}})}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo/handler
  [_ _]
  (record-lifecycle! :halt :kit.demo/handler {}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/init-key :kit.demo/http-server
  [_ {:keys [host port join? handler]}]
  (record-lifecycle! :init :kit.demo/http-server {:host host
                                                  :port port
                                                  :join? join?})
  {:host host
   :port port
   :join? join?
   :handler handler
   :state :running
   :started-at (now-ms)})

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/suspend-key! :kit.demo/http-server
  [_ server]
  ;; 演示语义: 保持 server 资源, 只把状态切换为 suspended.
  (record-lifecycle! :suspend :kit.demo/http-server {:port (:port server)}))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/resume-key :kit.demo/http-server
  [_ new-value old-value old-impl]
  (let [same-binding? (= [(:host new-value) (:port new-value)]
                         [(:host old-value) (:port old-value)])]
    (record-lifecycle! :resume :kit.demo/http-server
                       {:same-binding? same-binding?
                        :old-port (:port old-value)
                        :new-port (:port new-value)})
    (if same-binding?
      (assoc old-impl
             :state :running
             :handler (:handler new-value)
             :resumed-at (now-ms))
      (ig/init-key :kit.demo/http-server new-value))))

^{::clerk/visibility {:code :show :result :hide}}
(defmethod ig/halt-key! :kit.demo/http-server
  [_ server]
  (record-lifecycle! :halt :kit.demo/http-server {:port (:port server)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn build-system-config
  "把 Aero settings 转换为 Integrant 配置图."
  [settings]
  {:kit.demo/config settings
   :kit.demo/db {:jdbc-url (get-in settings [:db :jdbc-url])
                 :pool-size (get-in settings [:db :pool-size])}
   :kit.demo/cache {:ttl-ms (get-in settings [:cache :ttl-ms])}
   :kit.demo.interceptor/logging {:order 10
                                  :level (get-in settings [:logging :level])}
   :kit.demo.interceptor/tracing {:order 20
                                  :enabled? (get-in settings [:features :tracing?])}
   :kit.demo/handler {:app-config (ig/ref :kit.demo/config)
                      :db (ig/ref :kit.demo/db)
                      :cache (ig/ref :kit.demo/cache)
                      :interceptors (ig/refset :kit.demo/interceptor)}
   :kit.demo/http-server {:host (get-in settings [:http :host])
                          :port (get-in settings [:http :port])
                          :join? (get-in settings [:http :join?] false)
                          :handler (ig/ref :kit.demo/handler)}})

^{::clerk/visibility {:code :show :result :hide}}
(defn prepared-config
  "完整配置流程: Aero -> Integrant 配置图 -> ig/prep."
  ([profile] (prepared-config profile {}))
  ([profile overrides]
   (-> (load-settings profile overrides)
       build-system-config
       ig/prep)))

^{::clerk/visibility {:code :show :result :show}}
(-> (prepared-config :dev)
    (select-keys [:kit.demo/cache :kit.demo/http-server :kit.demo/handler]))


;; ## 3. 统一系统状态管理函数

;; Kit 项目通常会保留一个系统状态容器(例如 `defonce system`), 并统一暴露 `start!/stop!/restart!`.
;; 这样可以做到:

;; - 所有组件生命周期都走 Integrant.
;; - 配置变化可以走 suspend/resume 热更新.
;; - 故障定位时有统一事件轨迹.

^{::clerk/visibility {:code :show :result :hide}}
(defonce runtime* (atom {:profile :dev
                         :overrides {}
                         :settings nil
                         :config nil
                         :system nil}))

^{::clerk/visibility {:code :show :result :hide}}
(defn start!
  "启动整个系统."
  ([] (start! :dev {}))
  ([profile overrides]
   (let [settings (load-settings profile overrides)
         config (-> settings build-system-config ig/prep)
         system (ig/init config)]
     (swap! runtime* assoc
            :profile profile
            :overrides overrides
            :settings settings
            :config config
            :system system)
     system)))

^{::clerk/visibility {:code :show :result :hide}}
(defn stop!
  "停止整个系统."
  []
  (when-let [system (:system @runtime*)]
    (ig/halt! system))
  (swap! runtime* assoc :system nil)
  :stopped)

^{::clerk/visibility {:code :show :result :hide}}
(defn reload!
  "热重载系统.

流程:
1. 新配置经 Aero + Integrant prep.
2. 对旧系统调用 suspend!.
3. 使用 resume 构建新系统."
  ([] (reload! (:overrides @runtime*)))
  ([overrides]
   (let [{:keys [profile system]} @runtime*
         active-profile (or profile :dev)
         settings (load-settings active-profile overrides)
         config (-> settings build-system-config ig/prep)
         new-system (if system
                      (do
                        (ig/suspend! system)
                        (ig/resume config system))
                      (ig/init config))]
     (swap! runtime* assoc
            :profile active-profile
            :overrides overrides
            :settings settings
            :config config
            :system new-system)
     new-system)))

^{::clerk/visibility {:code :show :result :hide}}
(defn status
  "读取当前系统关键状态."
  []
  (let [{:keys [profile system]} @runtime*]
    {:profile profile
     :running? (boolean system)
     :http (some-> (get system :kit.demo/http-server)
                   (select-keys [:host :port :state :started-at :resumed-at]))
     :db (some-> (get system :kit.demo/db)
                 (select-keys [:jdbc-url :pool-size :conn-id]))
     :interceptors (some->> (get-in system [:kit.demo/handler :interceptors])
                            (mapv :name))
     :lifecycle-events (count @lifecycle-events*)}))



;; ## 4. 高级用法演示

;; ### 4.1 局部启动: 只初始化 HTTP 子系统(含依赖)

^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true}
(do
  (reset-lifecycle!)
  (let [config (prepared-config :dev {:http {:port 19090}})
        system (ig/init config [:kit.demo/http-server])
        out {:started-keys (sort (keys system))
             :handler-interceptors (mapv :name (get-in system [:kit.demo/handler :interceptors]))
             :http (select-keys (get system :kit.demo/http-server) [:host :port :state])}]
    (ig/halt! system)
    {:result out
     :lifecycle @lifecycle-events*}))


;; ### 4.2 suspend/resume: 不改绑定端口时复用 server

^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true}
(do
  (reset-lifecycle!)
  (let [config-v1 (prepared-config :dev {:http {:port 20080}
                                      :cache {:ttl-ms 30000}})
        system-v1 (ig/init config-v1)
        config-v2 (prepared-config :dev {:http {:port 20080}
                                      :cache {:ttl-ms 90000}
                                      :features {:tracing? false}})
        _ (ig/suspend! system-v1)
        system-v2 (ig/resume config-v2 system-v1)
        out {:before (select-keys (get system-v1 :kit.demo/http-server) [:port :state :started-at])
             :after (select-keys (get system-v2 :kit.demo/http-server) [:port :state :started-at :resumed-at])
             :cache-ttl-before (get-in system-v1 [:kit.demo/cache :ttl-ms])
             :cache-ttl-after (get-in system-v2 [:kit.demo/cache :ttl-ms])}]
    (ig/halt! system-v2)
    {:result out
     :lifecycle @lifecycle-events*}))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "### 4.3 绑定变更时自动走重新初始化")

^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true}
(do
  (reset-lifecycle!)
  (let [config-v1 (prepared-config :dev {:http {:port 20100}})
        system-v1 (ig/init config-v1)
        old-started-at (get-in system-v1 [:kit.demo/http-server :started-at])
        config-v2 (prepared-config :dev {:http {:port 20200}})
        _ (ig/suspend! system-v1)
        system-v2 (ig/resume config-v2 system-v1)
        new-started-at (get-in system-v2 [:kit.demo/http-server :started-at])]
    (ig/halt! system-v2)
    {:old-port (get-in system-v1 [:kit.demo/http-server :port])
     :new-port (get-in system-v2 [:kit.demo/http-server :port])
     :started-at-changed? (not= old-started-at new-started-at)
     :resume-event (->> @lifecycle-events*
                        (filter #(= (:event %) :resume))
                        last)}))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "### 4.4 统一入口 start!/reload!/stop! 示例")

^{::clerk/visibility {:code :show :result :show}
  ::clerk/auto-expand-results? true}
(do
  (reset-lifecycle!)
  (start! :dev {:http {:port 20300}})
  (let [s1 (status)
        _ (reload! {:http {:port 20300}
                    :cache {:ttl-ms 120000}})
        s2 (status)
        _ (stop!)
        s3 (status)]
    {:after-start s1
     :after-reload s2
     :after-stop s3
     :lifecycle-tail (take-last 8 @lifecycle-events*)}))

;; ## 5. 实战建议

;; 1. 配置分层清晰化: `base + profile + override` 是最稳定的上线模型.
;; 2. Integrant key 要保持明确边界: `:kit.xxx/*` 前缀有助于定位和分组.
;; 3. 热重载优先走 `suspend!/resume`: 能复用资源时不要强制全停全起.
;; 4. `ig/refset` 适合插件化能力: 拦截器, 任务处理器, 消费者组都可以这样组织.
;; 5. 把状态入口收敛到一个 runtime 容器: 排障和运维操作都会更简单.

(comment
  ;; 手动查看 notebook.
  (clerk/serve! {:browse? true})
  (clerk/show! "notebooks/web_dev/kit_aero_integrant.clj"))
