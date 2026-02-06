^{:nextjournal.clerk/visibility {:code :show}}
(ns web-dev.ring-middleware-with-portal
  "使用 Portal 可视化 Ring 中间件的处理过程"
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]
            [portal.viewer :as pv]))

;; # 🔍 使用 Portal 可视化 Ring 中间件

;; 本 notebook 演示如何使用 Portal 来直观地展示每个中间件对请求和响应的修改。
;; Portal 的 diff 功能可以让我们清晰地看到数据在中间件链中的变化。

;; ## 启动 Portal

^{::clerk/visibility {:code :show :result :show}}
(defonce portal-instance (atom nil))

^{::clerk/visibility {:code :show :result :show}}
(defn ensure-portal!
  "确保 Portal 已打开并配置好 tap> 目标"
  []
  (when (or (nil? @portal-instance)
            (empty? (p/sessions)))
    (let [portal (p/open {:theme :portal.colors/nord
                          :window-title "Ring 中间件可视化"})]
      (reset! portal-instance portal)
      (add-tap #'p/submit)))
  @portal-instance)

^{::clerk/visibility {:code :show :result :show}}
(ensure-portal!)

^{::clerk/visibility {:code :show :result :show}}
(defn clear-portal!
  "清空 Portal 中的历史数据"
  []
  (when-let [portal @portal-instance]
    (p/clear portal)))

;; ## 中间件追踪工具

;; 我们创建一个工具函数，用于捕获中间件处理前后的数据并发送到 Portal

^{::clerk/visibility {:code :show :result :show}}
(defn tap-diff
  "发送处理前后的数据到 Portal，使用 diff 视图"
  [stage before after]
  (tap> (with-meta
          {:stage stage
           :before before
           :after after
           :diff (pv/diff before after)}
          {:portal.viewer/default :portal.viewer/diff})))

^{::clerk/visibility {:code :show :result :show}}
(defn tap-step
  "发送单个处理步骤到 Portal"
  [stage description data]
  (tap> (pv/log {:stage stage
                 :description description
                 :data data
                 :timestamp (java.time.Instant/now)})))

;; ## 创建可追踪的中间件

;; ### 1. CORS 中间件
;;
;; 这个中间件添加跨域资源共享（CORS）头部

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-cors
  "添加 CORS 头部到响应中"
  [handler]
  (fn [request]
    (let [response (handler request)
          modified-response (-> response
                               (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
                               (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
                               (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))]
      ;; 发送处理前后的对比到 Portal
      (tap-diff "CORS 中间件"
                {:response response}
                {:response modified-response})
      modified-response)))

;; ### 2. 请求 ID 中间件
;;
;; 这个中间件为每个请求添加唯一标识符

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-request-id
  "为请求添加唯一的 ID"
  [handler]
  (fn [request]
    (let [request-id (str "req-" (System/currentTimeMillis) "-" (rand-int 10000))
          modified-request (assoc request :request-id request-id)]
      ;; 发送处理前后的对比到 Portal
      (tap-diff "请求 ID 中间件"
                {:request (dissoc request :server-port :remote-addr)}
                {:request (dissoc modified-request :server-port :remote-addr)})
      (handler modified-request))))

;; ### 3. 计时中间件
;;
;; 这个中间件测量处理时间并添加到响应头

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-timing
  "测量请求处理时间"
  [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)
          modified-response (assoc-in response [:headers "X-Response-Time"] (str duration "ms"))]
      ;; 发送处理前后的对比到 Portal
      (tap-diff "计时中间件"
                {:response response
                 :duration-ms duration}
                {:response modified-response
                 :duration-ms duration})
      modified-response)))

;; ### 4. 身份认证中间件
;;
;; 这个中间件检查授权令牌并添加用户信息

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-auth
  "简单的身份认证中间件"
  [handler]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"])
          is-authenticated? (= auth-header "Bearer secret-token")
          modified-request (if is-authenticated?
                            (assoc request :user {:id 1 :name "张三" :role :admin})
                            request)]
      ;; 发送处理前后的对比到 Portal
      (tap-diff "身份认证中间件"
                {:request (select-keys request [:uri :headers :request-id])
                 :authenticated? false}
                {:request (select-keys modified-request [:uri :headers :request-id :user])
                 :authenticated? is-authenticated?})

      (if (and (= (:uri request) "/protected")
               (not is-authenticated?))
        ;; 返回未授权响应
        (do
          (tap-step "身份认证中间件" "访问被拒绝" {:uri (:uri request) :reason "未授权"})
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body "{\"error\": \"Unauthorized\"}"})
        ;; 继续处理
        (handler modified-request)))))

;; ### 5. JSON 处理中间件
;;
;; 这个中间件解析 JSON 请求体并格式化 JSON 响应

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-json
  "处理 JSON 请求和响应"
  [handler]
  (fn [request]
    ;; 解析请求体
    (let [body-str (when-let [body (:body request)]
                    (if (string? body) body nil))
          parsed-body (when body-str
                       (try
                         (clojure.data.json/read-str body-str :key-fn keyword)
                         (catch Exception _ nil)))
          modified-request (if parsed-body
                            (assoc request :parsed-body parsed-body)
                            request)]

      ;; 发送请求解析的对比到 Portal
      (when parsed-body
        (tap-diff "JSON 中间件 (请求)"
                  {:request (select-keys request [:uri :body])}
                  {:request (select-keys modified-request [:uri :parsed-body])}))

      ;; 处理响应
      (let [response (handler modified-request)
            ;; 如果响应体是 map，转换为 JSON 字符串
            modified-response (if (map? (:body response))
                               (-> response
                                   (assoc :body (clojure.data.json/write-str (:body response)))
                                   (assoc-in [:headers "Content-Type"] "application/json"))
                               response)]

        ;; 发送响应转换的对比到 Portal
        (when (map? (:body response))
          (tap-diff "JSON 中间件 (响应)"
                    {:response response}
                    {:response modified-response}))

        modified-response))))

;; ## 创建测试处理器

^{::clerk/visibility {:code :show :result :show}}
(defn api-handler
  "基础 API 处理器"
  [request]
  (tap-step "处理器" "处理请求" {:uri (:uri request)
                                :request-id (:request-id request)
                                :user (:user request)})
  (case (:uri request)
    "/" {:status 200
         :body {:message "欢迎访问 API"
                :request-id (:request-id request)
                :timestamp (str (java.time.Instant/now))}}

    "/user" {:status 200
             :body {:message "用户信息"
                    :user (:user request)
                    :request-id (:request-id request)}}

    "/protected" {:status 200
                  :body {:message "受保护的资源"
                         :user (:user request)
                         :request-id (:request-id request)
                         :secret-data "这是机密信息"}}

    {:status 404
     :body {:error "未找到"
            :uri (:uri request)}}))

;; ## 组合中间件链

^{::clerk/visibility {:code :show :result :show}}
(def app
  (-> api-handler
      wrap-json
      wrap-auth
      wrap-timing
      wrap-request-id
      wrap-cors))

;; ## 测试示例

;; 在运行这些示例之前，请确保 Portal 窗口已打开。
;; 你可以在 Portal 中：
;; 1. 选择两个相邻的数据项
;; 2. 使用 Cmd+D (Mac) 或 Ctrl+D (Windows/Linux) 来查看 diff

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 示例 1: 访问公开端点")

^{::clerk/visibility {:code :show :result :show}}
(do
  (tap> (pv/log {:message "=== 开始新请求 ==="
                :test "示例 1: 访问公开端点"}))
  (app {:request-method :get
        :uri "/"
        :headers {}
        :body nil}))

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 示例 2: 访问用户端点")

^{::clerk/visibility {:code :show :result :show}}
(do
  (tap> (pv/log {:message "=== 开始新请求 ==="
                :test "示例 2: 访问用户端点"}))
  (app {:request-method :get
        :uri "/user"
        :headers {"authorization" "Bearer secret-token"}
        :body nil}))

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 示例 3: 访问受保护端点（已认证）")

^{::clerk/visibility {:code :show :result :show}}
(do
  (tap> (pv/log {:message "=== 开始新请求 ==="
                :test "示例 3: 访问受保护端点（已认证）"}))
  (app {:request-method :get
        :uri "/protected"
        :headers {"authorization" "Bearer secret-token"}
        :body nil}))

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 示例 4: 访问受保护端点（未认证）")

^{::clerk/visibility {:code :show :result :show}}
(do
  (tap> (pv/log {:message "=== 开始新请求 ==="
                :test "示例 4: 访问受保护端点（未认证）"}))
  (app {:request-method :get
        :uri "/protected"
        :headers {}
        :body nil}))

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 示例 5: POST 请求带 JSON 数据")

^{::clerk/visibility {:code :show :result :show}}
(do
  (tap> (pv/log {:message "=== 开始新请求 ==="
                :test "示例 5: POST 请求带 JSON 数据"}))
  (app {:request-method :post
        :uri "/user"
        :headers {"authorization" "Bearer secret-token"
                  "content-type" "application/json"}
        :body "{\"name\": \"李四\", \"age\": 30}"}))

;; ## 可视化中间件流程

;; 创建一个完整的追踪示例，显示数据如何流经整个中间件栈

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 完整流程追踪")

^{::clerk/visibility {:code :show :result :show}}
(defn wrap-tracer
  "为整个中间件链添加入口/出口追踪"
  [handler stage]
  (fn [request]
    (tap-step "中间件流程" (str "进入: " stage)
              {:request (select-keys request [:uri :request-method :request-id])})
    (let [response (handler request)]
      (tap-step "中间件流程" (str "离开: " stage)
                {:response (select-keys response [:status :headers])})
      response)))

^{::clerk/visibility {:code :show :result :show}}
(def traced-app
  (-> api-handler
      (wrap-tracer "处理器")
      wrap-json
      (wrap-tracer "JSON")
      wrap-auth
      (wrap-tracer "认证")
      wrap-timing
      (wrap-tracer "计时")
      wrap-request-id
      (wrap-tracer "请求ID")
      wrap-cors
      (wrap-tracer "CORS")))

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "### 追踪完整请求流程")

^{::clerk/visibility {:code :show :result :show}}
(do
  (clear-portal!)
  (tap> (pv/log {:message "=== 完整流程追踪 ==="
                :description "观察请求如何流经每个中间件层"}))
  (traced-app {:request-method :get
               :uri "/protected"
               :headers {"authorization" "Bearer secret-token"}
               :body nil}))

;; ## 使用说明

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "
## 📖 如何使用 Portal 查看中间件处理过程

### 1. 查看 Diff
在 Portal 窗口中：
- 选择同一个中间件的 `:before` 和 `:after` 数据
- 按 `Cmd+D` (Mac) 或 `Ctrl+D` (Windows/Linux)
- Portal 会高亮显示差异部分

### 2. 查看时间线
- 按时间顺序查看每个中间件的处理步骤
- 观察数据如何在中间件链中流动

### 3. 过滤数据
- 使用 Portal 的搜索功能过滤特定的中间件
- 例如：搜索 `stage` 字段来查看特定中间件的操作

### 4. 比较不同请求
- 运行多个测试示例
- 在 Portal 中比较不同请求的处理过程

### 5. 清空历史
- 调用 `(clear-portal!)` 清空 Portal 中的历史数据
- 便于开始新的测试会话
")

;; ## 总结

^{::clerk/visibility {:code :show :result :show}}
(clerk/md "
## 🎯 总结

本 notebook 演示了如何使用 Portal 来可视化 Ring 中间件的处理过程：

1. **实时追踪**: 使用 `tap>` 实时发送中间件处理的数据到 Portal
2. **差异对比**: 使用 `portal.viewer/diff` 直观展示处理前后的变化
3. **流程可视化**: 追踪请求在中间件链中的完整流程
4. **数据探查**: 利用 Portal 的交互功能深入探查数据结构

### 优势

- ✅ 直观展示每个中间件的影响
- ✅ 快速定位问题和调试
- ✅ 理解中间件的执行顺序
- ✅ 比较不同场景下的行为差异

### 下一步

尝试：
- 添加自己的中间件并观察其影响
- 修改中间件的顺序，观察结果变化
- 使用 Portal 的其他视图功能探查数据
")

;; ## 辅助函数

^{::clerk/visibility {:code :show :result :show}}
(comment
  ;; 打开 Portal
  (ensure-portal!)

  ;; 清空 Portal
  (clear-portal!)

  ;; 运行单个测试
  (app {:request-method :get
        :uri "/"
        :headers {}})

  ;; 运行追踪测试
  (traced-app {:request-method :get
               :uri "/protected"
               :headers {"authorization" "Bearer secret-token"}})

  ;; 关闭 Portal
  (when-let [portal @portal-instance]
    (remove-tap #'p/submit)
    (p/close portal)
    (reset! portal-instance nil))
  )
