;; # 使用 ezzmq(ZeroMQ) 的端到端测试场景

^{:nextjournal.clerk/visibility {:code :hide}
  :nextjournal.clerk/toc true}
(ns web-dev.ezzmq-complete-test-scenario
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing]]
            [ezzmq.core :as zmq])
  (:import [java.net ServerSocket]
           [java.util UUID]
           [org.zeromq ZMQ$Error ZMQException]))

;; 本页面向需要在 JVM 侧做消息通信, 又希望测试用例能完整覆盖线程, 超时, 以及资源释放的同事.
;;
;; 本笔记使用 `ezzmq`(基于 JeroMQ 的 ZeroMQ 封装) 演示一个可重复运行的端到端场景:
;;
;; - 网关(gateway) 使用 `REQ/REP` 调用服务
;; - 服务(order-service) 在处理完成后通过 `PUB/SUB` 发出审计事件
;; - 网关同时通过 `PUSH/PULL` 上报指标(metrics)
;; - 消费端(audit-consumer) 使用 `poll` 同时监听审计事件与指标
;;
;; 关键点:
;;
;; - 所有阻塞接收都运行在 `zmq/worker-thread` 中, 便于在 context 关闭时安全终止
;; - 所有等待都有超时, 失败不会卡住 Clerk 求值

;; 说明: `ezzmq` 负责的是 ZeroMQ 通信与线程/中断友好封装, 不会替你做业务序列化.
;;
;; `zmq/send-msg` 的每一帧只接受 string 或 byte-array, 所以本文用 `pr-str` 把 Clojure 数据编码成 EDN 字符串进行传输.

(defn inproc
  "生成一个唯一的 inproc 地址. inproc 只在同一个 ZMQ context 内有效."
  [prefix]
  (str "inproc://" prefix "-" (UUID/randomUUID)))

(defn await!
  "等待 future 完成, 超时则抛异常, 避免 notebook 卡死."
  [f ms label]
  (let [v (deref f ms ::timeout)]
    (when (= v ::timeout)
      (throw (ex-info (str "等待超时: " label) {:timeout-ms ms})))
    (when (instance? Throwable v)
      (throw v))
    v))

;; 说明: ezzmq 默认使用 ZContext, 在某些环境下 JVM 退出可能会被 socket 的 linger 行为拖慢.
;; 这里统一切换到 :zmq.context, 其 destroy 会把所有 socket 的 linger 设为 0 再 term, 便于测试快速退出.
(defmacro with-fast-context
  [& body]
  `(binding [ezzmq.context/*context-type* :zmq.context]
     (ezzmq.context/with-new-context ~@body)))

;; 说明: `zmq/worker-thread` 基于 `future`, 会启动非 daemon 线程池, 在脚本模式下可能导致 JVM 退出延迟.
;; 这里用 daemon 线程 + promise 承载结果, 保持 notebook 交互体验同时避免脚本模式等待线程池回收.
(defmacro worker-daemon
  [{:keys [on-interrupt] :as _opts} & body]
  `(let [p# (promise)
         run# (bound-fn []
                (try
                  (deliver p# (do ~@body))
                  (catch ZMQException e#
                    (let [eterm# (.getCode ZMQ$Error/ETERM)]
                      (if (= eterm# (.getErrorCode e#))
                        (deliver p# ((or ~on-interrupt (fn [] nil))))
                        (deliver p# e#))))
                  (catch Throwable t#
                    (deliver p# t#))))
         t# (doto (Thread. run#)
              (.setDaemon true)
              (.setName (str "ezzmq-daemon-" (UUID/randomUUID))))]
     (.start t#)
     p#))

;; ## 1. 生命周期钩子: before-shutdown / after-shutdown

(deftest shutdown-hooks-test
  (let [events (atom [])]
    (with-fast-context
      (zmq/before-shutdown (swap! events conj :before))
      (zmq/after-shutdown (swap! events conj :after)))
    (is (= [:before :after] @events))))

;; ## 2. 端到端场景: 网关 -> 服务 -> 审计与指标

;; 这个场景特意增加了一个 `pub-ready` 同步通道, 避免 PUB 在 SUB 建立订阅前就发消息导致测试偶发丢消息.
;;
;; 同步方式:
;;
;; - audit-consumer 在 SUB 订阅完成后, 通过 `PUSH` 发一个 `READY` 给 order-service
;; - order-service 在收到 `READY` 后才开始处理请求并发布审计事件

(defn start-order-service!
  "启动一个只处理一次请求的服务线程.
  返回 future, 最终值为 {:request .. :reply .. :published ..}."
  [{:keys [req-rep-addr pub-addr pub-ready-addr bound]}]
  (worker-daemon {}
                     (let [rep (zmq/socket :rep {:bind req-rep-addr})
                           pub (zmq/socket :pub {:bind pub-addr})
                           ready-pull (zmq/socket :pull {:bind pub-ready-addr})]
                       (when bound (deliver bound :bound))
      ;; 等 consumer 完成订阅再开始
                       (or (zmq/receive-msg ready-pull {:stringify true :timeout 1000})
                           (throw (ex-info "order-service 等待 READY 超时" {})))
                       (let [request (or (zmq/receive-msg rep {:stringify true :timeout 1000})
                                         (throw (ex-info "order-service 等待请求超时" {})))
                             reply ["OK" (pr-str {:service "order-service" :request request})]
                             published ["audit" (pr-str {:event :order/created :request request})]]
                         (zmq/send-msg rep reply)
                         (zmq/send-msg pub published)
                         {:request request :reply reply :published published}))))

(defn start-audit-consumer!
  "启动 audit-consumer 线程, 通过 poll 同时接收审计事件与指标.
  返回 future, 最终值为 {:audit .. :metric ..}."
  [{:keys [pub-addr pub-ready-addr metrics-addr bound]}]
  (worker-daemon {}
                     (let [sub (zmq/socket :sub {:connect pub-addr
                                                 :subscribe "audit"})
                           pull (zmq/socket :pull {:bind metrics-addr})
                           ready-push (zmq/socket :push {:connect pub-ready-addr})
                           _ (when bound (deliver bound :bound))
                           _ (zmq/send-msg ready-push "READY")
                           received (atom {:audit nil :metric nil})]
      ;; 轮询直到两个消息都收到, 或超时退出
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [sub  :pollin [msg] (swap! received assoc :audit msg)
                                     pull :pollin [msg] (swap! received assoc :metric msg)]
                                    (loop [deadline (+ (System/currentTimeMillis) 1500)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "consumer 超时: 未收到预期消息" @received)))
                                      (zmq/poll {:timeout 100})
                                      (if (and (:audit @received) (:metric @received))
                                        @received
                                        (recur deadline)))))))

(defn run-end-to-end!
  "运行一次端到端场景, 返回 {:service .. :consumer .. :gateway ..}."
  []
  (let [req-rep-addr (inproc "req-rep")
        pub-addr (inproc "pub")
        pub-ready-addr (inproc "pub-ready")
        metrics-addr (inproc "metrics")
        svc-bound (promise)
        consumer-bound (promise)
        svc-f (start-order-service! {:req-rep-addr req-rep-addr
                                     :pub-addr pub-addr
                                     :pub-ready-addr pub-ready-addr
                                     :bound svc-bound})
        _ (await! svc-bound 1500 "order-service bind")
        consumer-f (start-audit-consumer! {:pub-addr pub-addr
                                           :pub-ready-addr pub-ready-addr
                                           :metrics-addr metrics-addr
                                           :bound consumer-bound})
        _ (await! consumer-bound 1500 "audit-consumer bind")
        gateway-result
        (let [req (zmq/socket :req {:connect req-rep-addr})
              push (zmq/socket :push {:connect metrics-addr})
              request ["CREATE_ORDER" (pr-str {:order/id 42 :amount 100})]
              _ (zmq/send-msg req request)
              reply (or (zmq/receive-msg req {:stringify true :timeout 1000})
                        (throw (ex-info "gateway 等待 reply 超时" {})))
              _ (zmq/send-msg push ["metric" (pr-str {:name :order/create :ok true})])]
          {:request request :reply reply})]
    {:service (await! svc-f 1500 "order-service")
     :consumer (await! consumer-f 1500 "audit-consumer")
     :gateway gateway-result}))

(deftest end-to-end-test
  (with-fast-context
    (let [{:keys [service consumer gateway]} (run-end-to-end!)]
      (testing "gateway 收到 REP 回复"
        (is (= "OK" (first (:reply gateway)))))
      (testing "consumer 收到 audit 事件"
        (is (= "audit" (first (:audit consumer)))))
      (testing "consumer 收到 metric 上报"
        (is (= "metric" (first (:metric consumer))))))))

;; ## 3. Multipart: 多帧消息的测试

(deftest multipart-test
  (with-fast-context
    (let [addr (inproc "multipart")
          bound (promise)
          server-f
          (worker-daemon {}
                             (let [rep (zmq/socket :rep {:bind addr})
                                   _ (deliver bound :bound)
                                   msg (or (zmq/receive-msg rep {:stringify true :timeout 1000})
                                           (throw (ex-info "multipart server 等待请求超时" {})))]
                               (zmq/send-msg rep ["ACK" (pr-str msg)])
                               msg))
          _ (await! bound 1500 "multipart bind")
          client (zmq/socket :req {:connect addr})
          _ (zmq/send-msg client ["A" "B" "C"])
          reply (or (zmq/receive-msg client {:stringify true :timeout 1000})
                    (throw (ex-info "multipart client 等待 reply 超时" {})))
          server-msg (await! server-f 1500 "multipart-server")]
      (is (= ["A" "B" "C"] server-msg))
      (is (= "ACK" (first reply))))))

;; ## 4. Pipeline: PUSH/PULL 的任务分发与结果汇聚

;; 典型应用场景: 批处理任务, 图片/音频转码, 报表生成, 以及任何需要把一批 job 均匀分发给多个 worker 的场景.
;;
;; 模式:
;;
;; - ventilator: `PUSH` 发送 job
;; - workers: `PULL` 收 job, 处理后用 `PUSH` 发结果
;; - sink: `PULL` 收结果并汇总
;;
;; 注意: ZeroMQ socket 不是线程安全的, 每个线程各自创建/使用自己的 socket.

(defn start-pipeline-worker!
  "启动一个 pipeline worker. 收到 \"STOP\" 后退出."
  [{:keys [jobs-addr results-addr worker-id]}]
  (worker-daemon {}
                     (let [pull (zmq/socket :pull {:connect jobs-addr})
                           push (zmq/socket :push {:connect results-addr})]
                       (loop []
                         (if-let [[job] (zmq/receive-msg pull {:stringify true :timeout 100})]
                           (if (= job "STOP")
                             :stopped
                             (do
                               (zmq/send-msg push ["result" (pr-str {:worker worker-id :job job})])
                               (recur)))
                           (recur))))))

(defn start-pipeline-sink!
  "启动 sink, 收到指定数量的结果后返回."
  [{:keys [results-addr ready-addr total]}]
  (worker-daemon {}
                     (let [pull (zmq/socket :pull {:bind results-addr})
                           ready-push (zmq/socket :push {:connect ready-addr})
                           _ (zmq/send-msg ready-push "READY")
                           results (atom [])]
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [pull :pollin [msg] (swap! results conj msg)]
                                    (loop [deadline (+ (System/currentTimeMillis) 2000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "pipeline sink 超时" {:received (count @results)
                                                                            :expected total})))
                                      (zmq/poll {:timeout 50})
                                      (if (= (count @results) total)
                                        @results
                                        (recur deadline)))))))

(deftest pipeline-test
  (with-fast-context
    (let [jobs-addr (inproc "jobs")
          results-addr (inproc "results")
          sink-ready-addr (inproc "sink-ready")
          total-jobs 10
          worker-count 3
          ;; inproc 要求 bind 先于 connect, 所以 ventilator 先 bind
          vent (zmq/socket :push {:bind jobs-addr})
          ;; 先 bind READY 的 pull, 避免 inproc connect 在 bind 之前发生
          sink-ready-pull (zmq/socket :pull {:bind sink-ready-addr})
          sink-f (start-pipeline-sink! {:results-addr results-addr
                                        :ready-addr sink-ready-addr
                                        :total total-jobs})
          ;; 等 sink bind 完成再启动 workers
          _ (or (zmq/receive-msg sink-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 pipeline sink READY 超时" {})))
          workers (doall (mapv (fn [i]
                                 (start-pipeline-worker! {:jobs-addr jobs-addr
                                                          :results-addr results-addr
                                                          :worker-id i}))
                               (range worker-count)))
          _ (doseq [i (range total-jobs)]
              (zmq/send-msg vent (str "job-" i)))
          _ (doseq [_ (range worker-count)]
              (zmq/send-msg vent "STOP"))
          results (await! sink-f 2500 "pipeline-sink")]
      (is (= total-jobs (count results)))
      (is (every? #(= "result" (first %)) results))
      (is (<= 1 (count (set (map (fn [msg]
                                   (let [[_ payload] msg
                                         m (read-string payload)]
                                     (:worker m)))
                                 results))))))))

;; ## 5. ROUTER/REQ: 识别调用方身份(多租户网关, 会话路由)

;; 典型应用场景: 网关层需要区分调用方身份, 做限流/鉴权/灰度, 或把消息路由到指定后端实例.
;;
;; 要点:
;;
;; - `ROUTER` 侧收到的消息会带一个 envelope: `[identity "" payload...]`
;; - 回复时必须把 `identity` 和空分隔帧一起带回去

(deftest router-identity-test
  (with-fast-context
    (let [addr (inproc "router-identity")
          bound (promise)
          server-f
          (worker-daemon {}
                             (let [router (zmq/socket :router {:bind addr})
                                   _ (deliver bound :bound)
                                   msg (or (zmq/receive-msg router {:stringify true :timeout 1000})
                                           (throw (ex-info "router server 等待请求超时" {})))
                                   identity (nth msg 0)
                                   empty (nth msg 1)
                                   payload (subvec msg 2)]
                               (zmq/send-msg router [identity "" "OK"])
                               {:identity identity :empty empty :payload payload}))
          _ (await! bound 1500 "router bind")
          client (zmq/socket :req {:connect addr
                                   :identity "client-A"})
          _ (zmq/send-msg client "HELLO")
          reply (or (zmq/receive-msg client {:stringify true :timeout 1000})
                    (throw (ex-info "router client 等待 reply 超时" {})))
          srv (await! server-f 1500 "router-server")]
      (is (= ["OK"] reply))
      (is (= "client-A" (:identity srv)))
      (is (= "" (:empty srv)))
      (is (= ["HELLO"] (:payload srv))))))

;; ## 6. DEALER/ROUTER: 并发 RPC 与乱序回复(需要相关 ID)

;; 典型应用场景: 一个客户端想并发发起多个请求, 不希望像 `REQ/REP` 那样必须严格一发一收.
;;
;; 做法:
;;
;; - client 用 `DEALER` 发送 `request-id` + `payload`
;; - server 用 `ROUTER` 读取 `[identity request-id payload]`
;; - server 回复时带回 `request-id`, client 自己做相关性匹配
;;
;; 这个模式非常适合网关扇出调用多个后端, 或者同一连接上并发多个 in-flight 请求.

(deftest dealer-router-concurrent-test
  (with-fast-context
    (let [addr (inproc "dealer-router")
          bound (promise)
          server-f
          (worker-daemon {}
                             (let [router (zmq/socket :router {:bind addr})
                                   _ (deliver bound :bound)
                                   m1 (or (zmq/receive-msg router {:stringify true :timeout 1000})
                                          (throw (ex-info "dealer/router server 等待请求1超时" {})))
                                   m2 (or (zmq/receive-msg router {:stringify true :timeout 1000})
                                          (throw (ex-info "dealer/router server 等待请求2超时" {})))
                  ;; ROUTER 收到的消息格式: [identity req-id payload]
                                   [id1 reqid1 payload1] m1
                                   [id2 reqid2 payload2] m2
                  ;; 故意反向回复, 演示乱序
                                   _ (zmq/send-msg router [id2 reqid2 (str "REPLY:" payload2)])
                                   _ (zmq/send-msg router [id1 reqid1 (str "REPLY:" payload1)])]
                               {:received [m1 m2]}))
          _ (await! bound 1500 "dealer/router bind")
          client (zmq/socket :dealer {:connect addr
                                      :identity "client-D"})
          _ (zmq/send-msg client ["1" "HELLO"])
          _ (zmq/send-msg client ["2" "WORLD"])
          r1 (or (zmq/receive-msg client {:stringify true :timeout 1000})
                 (throw (ex-info "dealer client 等待 reply1 超时" {})))
          r2 (or (zmq/receive-msg client {:stringify true :timeout 1000})
                 (throw (ex-info "dealer client 等待 reply2 超时" {})))
          replies (set [r1 r2])]
      (is (= #{["1" "REPLY:HELLO"]
               ["2" "REPLY:WORLD"]}
             replies))
      (await! server-f 1500 "dealer-router-server"))))

;; ## 7. Broker: ROUTER/DEALER 做负载均衡与解耦

;; 典型应用场景: 你希望把客户端与一组后端 worker 解耦, 支持水平扩展, 并让 broker 承担路由与负载均衡.
;;
;; 模式:
;;
;; - frontend: `ROUTER` 接客户端 `REQ`
;; - backend: `DEALER` 把请求分发到多个 `REP` worker
;;
;; 关键点:
;;
;; - broker 必须转发 *完整消息帧*(包含 identity envelope), 不能丢 frame
;; - 对 `inproc://` 必须先 bind 后 connect, 所以用 READY 同步保证启动顺序

(defn start-broker-proxy!
  "启动 broker proxy, 通过 ctrl 停止, 并在 ready-addr 上发送 READY."
  [{:keys [frontend-addr backend-addr ready-addr ctrl-addr]}]
  (worker-daemon {}
                     (let [frontend (zmq/socket :router {:bind frontend-addr})
                           backend (zmq/socket :dealer {:bind backend-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [frontend :pollin [msg] (zmq/send-msg backend msg)
                                     backend  :pollin [msg] (zmq/send-msg frontend msg)
                                     ctrl     :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 3000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "broker proxy 超时退出" {})))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       :stopped)))

(defn start-broker-worker!
  "启动一个 broker worker(backend REP). 通过 ctrl 停止, 并在 ready-addr 上发送 READY."
  [{:keys [backend-addr ready-addr ctrl-addr worker-id]}]
  (worker-daemon {}
                     (let [rep (zmq/socket :rep {:connect backend-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [rep  :pollin [msg]
         ;; 在这个 ROUTER/DEALER + REQ/REP 组合里, worker 通常只收到 payload,
         ;; 不会看到 client identity, 由 broker 负责把回复路由回正确 client.
                                     (zmq/send-msg rep (into ["OK" (str worker-id)] msg))
                                     ctrl :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 3000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "broker worker 超时退出" {:worker worker-id})))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       :stopped)))

(deftest broker-load-balancer-test
  (with-fast-context
    (let [frontend-addr (inproc "lb-frontend")
          backend-addr (inproc "lb-backend")
          proxy-ready-addr (inproc "lb-proxy-ready")
          proxy-ctrl-addr (inproc "lb-proxy-ctrl")
          worker-ready-addr (inproc "lb-worker-ready")
          w1-ctrl-addr (inproc "lb-w1-ctrl")
          w2-ctrl-addr (inproc "lb-w2-ctrl")
          proxy-ready-pull (zmq/socket :pull {:bind proxy-ready-addr})
          worker-ready-pull (zmq/socket :pull {:bind worker-ready-addr})
          proxy-f (start-broker-proxy! {:frontend-addr frontend-addr
                                        :backend-addr backend-addr
                                        :ready-addr proxy-ready-addr
                                        :ctrl-addr proxy-ctrl-addr})
          _ (or (zmq/receive-msg proxy-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 broker proxy READY 超时" {})))
          w1-f (start-broker-worker! {:backend-addr backend-addr
                                      :ready-addr worker-ready-addr
                                      :ctrl-addr w1-ctrl-addr
                                      :worker-id 1})
          w2-f (start-broker-worker! {:backend-addr backend-addr
                                      :ready-addr worker-ready-addr
                                      :ctrl-addr w2-ctrl-addr
                                      :worker-id 2})
          ;; 等两个 worker ready
          _ (or (zmq/receive-msg worker-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 broker worker-1 READY 超时" {})))
          _ (or (zmq/receive-msg worker-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 broker worker-2 READY 超时" {})))
          client (zmq/socket :req {:connect frontend-addr
                                   :identity "client-lb"})
          replies (vec (for [i (range 12)]
                         (do
                           (zmq/send-msg client ["REQ" (str i)])
                           (or (zmq/receive-msg client {:stringify true :timeout 1000})
                               (throw (ex-info "broker client 等待 reply 超时" {:i i}))))))
          worker-ids (set (map second replies))
          stop! (fn [addr]
                  (let [p (zmq/socket :push {:connect addr})]
                    (zmq/send-msg p "STOP")))]
      (is (= 12 (count replies)))
      (is (every? #(= "OK" (first %)) replies))
      (is (<= 2 (count worker-ids)))
      ;; 清理: 先停 worker, 再停 proxy
      (stop! w1-ctrl-addr)
      (stop! w2-ctrl-addr)
      (stop! proxy-ctrl-addr)
      (await! w1-f 1500 "broker-worker-1")
      (await! w2-f 1500 "broker-worker-2")
      (await! proxy-f 1500 "broker-proxy"))))

;; ## 8. 超时与重试: receive-msg 不阻塞的基础构件

;; 典型应用场景: 你希望以确定的超时来等待消息, 并在超时后做重试/降级/退出.
;;
;; `zmq/receive-msg` 支持 `:timeout`(毫秒):
;;
;; - `:timeout 0` 立即返回
;; - 超时未收到消息时返回 `nil`
;;
;; 这对实现心跳检测, 软实时循环, 以及可中断的关闭逻辑非常关键.

(deftest receive-timeout-test
  (with-fast-context
    (let [addr (inproc "receive-timeout")
          pull (zmq/socket :pull {:bind addr})
          push (zmq/socket :push {:connect addr})]
      (is (nil? (zmq/receive-msg pull {:stringify true :timeout 0})))
      (is (nil? (zmq/receive-msg pull {:stringify true :timeout 10})))
      (zmq/send-msg push "hi")
      (is (= ["hi"] (zmq/receive-msg pull {:stringify true :timeout 100})))
      (testing "简单重试循环"
        (let [deadline (+ (System/currentTimeMillis) 200)
              got (loop []
                    (or (zmq/receive-msg pull {:stringify true :timeout 10})
                        (when (< (System/currentTimeMillis) deadline)
                          (recur))))]
          (is (nil? got)))))))

;; ## 9. XPUB/XSUB: Pub/Sub 代理与分层广播

;; 典型应用场景: 你希望把多个 publisher 和多个 subscriber 通过一个中间层连接起来, 形成分层的事件总线.
;;
;; 模式:
;;
;; - proxy 前端: `XSUB`(连接 publisher, 接收数据, 接收订阅)
;; - proxy 后端: `XPUB`(连接 subscriber, 发送数据, 发出订阅事件)
;;
;; 这里用一个最小的可测 proxy 实现, 同时转发:
;;
;; - 数据: XSUB -> XPUB
;; - 订阅控制: XPUB -> XSUB
;;
;; 注意: PUB/SUB 存在 slow joiner, 测试里会用重试发送确保收到.

(defn start-xpub-xsub-proxy!
  "启动一个 XPUB/XSUB proxy, 通过 ctrl 停止."
  [{:keys [xsub-addr xpub-addr ready-addr ctrl-addr]}]
  (worker-daemon {}
                     (let [xsub (zmq/socket :xsub {:bind xsub-addr})
                           xpub (zmq/socket :xpub {:bind xpub-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify false}}
                                    [xsub :pollin [msg] (zmq/send-msg xpub msg)
                                     xpub :pollin [msg] (zmq/send-msg xsub msg)
                                     ctrl :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 3000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "xpub/xsub proxy 超时退出" {})))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       :stopped)))

(deftest xpub-xsub-proxy-test
  (with-fast-context
    (let [xsub-addr (inproc "xsub")
          xpub-addr (inproc "xpub")
          ready-addr (inproc "xproxy-ready")
          ctrl-addr (inproc "xproxy-ctrl")
          ready-pull (zmq/socket :pull {:bind ready-addr})
          proxy-f (start-xpub-xsub-proxy! {:xsub-addr xsub-addr
                                           :xpub-addr xpub-addr
                                           :ready-addr ready-addr
                                           :ctrl-addr ctrl-addr})
          _ (or (zmq/receive-msg ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 xpub/xsub proxy READY 超时" {})))
          sub (zmq/socket :sub {:connect xpub-addr
                                :subscribe "topic1"})
          pub (zmq/socket :pub {:connect xsub-addr})
          got (loop [i 0]
                (when (>= i 30)
                  (throw (ex-info "pub/sub slow joiner: 重试后仍未收到消息" {})))
                (zmq/send-msg pub ["topic1" (str "hello-" i)])
                (or (zmq/receive-msg sub {:stringify true :timeout 50})
                    (recur (inc i))))
          stop! (fn []
                  (let [p (zmq/socket :push {:connect ctrl-addr})]
                    (zmq/send-msg p "STOP")))]
      (is (= "topic1" (first got)))
      (stop!)
      (await! proxy-f 1500 "xpub-xsub-proxy"))))

;; ## 10. 服务发现与控制面: 注册, 事件通知, 快照拉取

;; 典型应用场景: 你希望把服务节点的上线/下线做成一个小型控制面:
;;
;; - 写: 节点通过 `REQ/REP` 向 registry 注册或注销
;; - 读: 客户端通过 `REQ/REP` 拉取快照
;; - 订阅: 客户端通过 `PUB/SUB` 订阅变更事件
;;
;; 这类能力经常出现在网关, 调度器, 多实例 worker 池中.

(defn start-registry-service!
  "启动一个 registry 服务, 返回 future. 通过 ctrl 停止."
  [{:keys [rpc-addr events-addr ready-addr ctrl-addr]}]
  (worker-daemon {}
                     (let [rep (zmq/socket :rep {:bind rpc-addr})
                           pub (zmq/socket :pub {:bind events-addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           registry (atom {})
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [rep  :pollin [msg]
                                     (let [[cmd payload] msg
                                           data (when (and payload (not (str/blank? payload)))
                                                  (read-string payload))]
                                       (case cmd
                                         "REGISTER"
                                         (let [{:keys [id] :as node} data]
                                           (swap! registry assoc id node)
                                           (zmq/send-msg rep ["OK"])
                                           (zmq/send-msg pub ["registry" (pr-str {:type :node/up :node node})]))

                                         "DEREGISTER"
                                         (let [{:keys [id]} data
                                               old (get @registry id)]
                                           (swap! registry dissoc id)
                                           (zmq/send-msg rep ["OK"])
                                           (zmq/send-msg pub ["registry" (pr-str {:type :node/down :node old})]))

                                         "SNAPSHOT"
                                         (zmq/send-msg rep ["OK" (pr-str @registry)])

                                         (zmq/send-msg rep ["ERR" (pr-str {:unknown-cmd cmd})])))
                                     ctrl :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 3000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "registry 服务超时退出" {})))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       {:registry @registry :stopped true})))

(deftest service-discovery-control-plane-test
  (with-fast-context
    (let [rpc-addr (inproc "registry-rpc")
          events-addr (inproc "registry-events")
          ready-addr (inproc "registry-ready")
          ctrl-addr (inproc "registry-ctrl")
          ready-pull (zmq/socket :pull {:bind ready-addr})
          svc-f (start-registry-service! {:rpc-addr rpc-addr
                                          :events-addr events-addr
                                          :ready-addr ready-addr
                                          :ctrl-addr ctrl-addr})
          _ (or (zmq/receive-msg ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 registry READY 超时" {})))
          sub (zmq/socket :sub {:connect events-addr
                                :subscribe "registry"})
          req (zmq/socket :req {:connect rpc-addr})
          node {:id "n1" :addr "tcp://127.0.0.1:5555"}
          ;; 先注册, 再等待事件
          _ (zmq/send-msg req ["REGISTER" (pr-str node)])
          _ (is (= ["OK"] (zmq/receive-msg req {:stringify true :timeout 500})))
          event (loop [i 0]
                  (when (>= i 30)
                    (throw (ex-info "等待 registry 事件超时" {})))
                  (or (zmq/receive-msg sub {:stringify true :timeout 50})
                      (recur (inc i))))
          _ (zmq/send-msg req "SNAPSHOT")
          [status snapshot] (or (zmq/receive-msg req {:stringify true :timeout 500})
                                (throw (ex-info "等待 registry snapshot 超时" {})))
          stop! (fn []
                  (let [p (zmq/socket :push {:connect ctrl-addr})]
                    (zmq/send-msg p "STOP")))]
      (is (= "registry" (first event)))
      (is (= :node/up (:type (read-string (second event)))))
      (is (= "OK" status))
      (is (= node (get (read-string snapshot) "n1")))
      (stop!)
      (await! svc-f 1500 "registry-service"))))

;; ## 11. Lazy Pirate(简化版): 客户端超时重试与重连退避

;; 典型应用场景: 客户端要调用一个可能抖动的服务, 希望能超时重试并最终失败可控.
;;
;; ZeroMQ 的 `REQ/REP` 在同一个 socket 上要求严格交替 send/recv, 所以常见做法是:
;;
;; - 每次重试都创建新 socket(或使用 `DEALER/ROUTER`)
;; - 以超时作为失败边界, 做退避重试
;;
;; 这里用 `DEALER/ROUTER` 做一个最小可测的重试实现.

(defn start-unreliable-router!
  "启动一个不可靠的 ROUTER: 每个 req-id 第一次收到时丢弃, 第二次才回复."
  [{:keys [addr ready-addr ctrl-addr bound]}]
  (worker-daemon {}
                     (let [router (zmq/socket :router {:bind addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           attempts (atom {})
                           stop? (atom false)]
                       (when bound (deliver bound :bound))
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [router :pollin [msg]
                                     (let [client-id (nth msg 0)
                                           reqid (nth msg 1)
                                           payload (nth msg 2)
                                           n (inc (get @attempts reqid 0))]
                                       (swap! attempts assoc reqid n)
                                       (when (>= n 2)
                                         (zmq/send-msg router [client-id reqid (str "OK:" payload)])))
                                     ctrl :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 3000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "unreliable router 超时退出" {})))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       :stopped)))

(defn dealer-request-with-retry!
  "使用 DEALER 发送 [reqid payload], 超时就重试(每次新建 socket). 成功返回 reply."
  [{:keys [addr reqid payload retries timeout-ms backoff-ms]}]
  (loop [attempt 1]
    (let [dealer (zmq/socket :dealer {:connect addr
                                      :identity (str "lazy-client-" attempt)
                                      :linger 0})
          _ (zmq/send-msg dealer [reqid payload])
          reply (zmq/receive-msg dealer {:stringify true :timeout timeout-ms})]
      (cond
        reply reply
        (>= attempt retries) nil
        :else (do
                (Thread/sleep backoff-ms)
                (recur (inc attempt)))))))

(deftest lazy-pirate-retry-test
  (with-fast-context
    (let [addr (inproc "lazy-router")
          ready-addr (inproc "lazy-ready")
          ctrl-addr (inproc "lazy-ctrl")
          bound (promise)
          ready-pull (zmq/socket :pull {:bind ready-addr})
          srv-f (start-unreliable-router! {:addr addr :ready-addr ready-addr :ctrl-addr ctrl-addr :bound bound})
          _ (await! bound 1500 "unreliable-router bind")
          _ (or (zmq/receive-msg ready-pull {:stringify true :timeout 1500})
                (throw (ex-info "等待 unreliable-router READY 超时" {})))
          reply (dealer-request-with-retry! {:addr addr
                                             :reqid "r1"
                                             :payload "HELLO"
                                             :retries 5
                                             :timeout-ms 40
                                             :backoff-ms 10})
          stop! (fn []
                  (let [p (zmq/socket :push {:connect ctrl-addr})]
                    (zmq/send-msg p "STOP")))]
      (is (= ["r1" "OK:HELLO"] reply))
      (stop!)
      (await! srv-f 1500 "unreliable-router"))))

;; ## 12. Paranoid Pirate(简化版): worker 心跳, 失活剔除, 自动切换

;; 典型应用场景: broker 管理一组 worker, 希望能检测 worker 失活, 并把后续请求自动切到存活 worker.
;;
;; 这里实现一个最小版本:
;;
;; - workers 使用 `DEALER` 连接 broker backend(`ROUTER`), 周期性发送 `HEARTBEAT`
;; - broker 维护 last-seen, 超时则剔除
;; - client 通过 broker frontend(`ROUTER`) 发送请求, broker 从可用 worker 队列里取一个分发
;;
;; 为了让测试稳定, 我们让 worker-1 处理一次请求后停止心跳, 然后验证下一次请求会走 worker-2.

(defn start-pp-broker!
  [{:keys [frontend-addr backend-addr ready-addr ctrl-addr hb-timeout-ms]}]
  (worker-daemon {}
                     (let [frontend (zmq/socket :router {:bind frontend-addr})
                           backend (zmq/socket :router {:bind backend-addr})
                           ready (zmq/socket :push {:connect ready-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
          ;; worker-id -> last-seen-ms
                           last-seen (atom {})
                           available (atom [])
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/polling {:receive-opts {:stringify true}}
                                    [backend  :pollin [msg]
                                     (let [worker-id (nth msg 0)
                                           cmd (nth msg 1 nil)]
                                       (swap! last-seen assoc worker-id (System/currentTimeMillis))
                                       (cond
                                         (= cmd "READY") (swap! available conj worker-id)
                                         (= cmd "HEARTBEAT") nil
                                         :else
                                         (let [client-id (nth msg 1)
                                               payload (nth msg 2)]
                                           (zmq/send-msg frontend [client-id "" payload])
                                           (swap! available conj worker-id))))
                                     frontend :pollin [msg]
                                     (let [client-id (nth msg 0)
                                           payload (nth msg 2)
                                           now (System/currentTimeMillis)
                                           live? (fn [wid]
                                                   (let [t (get @last-seen wid 0)]
                                                     (< (- now t) hb-timeout-ms)))
               ;; 清理死 worker
                                           _ (swap! available (fn [q] (vec (filter live? q))))
                                           worker-id (first @available)]
                                       (when-not worker-id
                                         (throw (ex-info "没有可用 worker" {:last-seen @last-seen})))
                                       (swap! available subvec 1)
                                       (zmq/send-msg backend [worker-id client-id payload]))
                                     ctrl :pollin [_] (reset! stop? true)]
                                    (loop [deadline (+ (System/currentTimeMillis) 4000)]
                                      (when (>= (System/currentTimeMillis) deadline)
                                        (throw (ex-info "pp-broker 超时退出" {})))
                                      (let [now (System/currentTimeMillis)]
                                        (swap! available (fn [q]
                                                           (vec (filter (fn [wid]
                                                                          (let [t (get @last-seen wid 0)]
                                                                            (< (- now t) hb-timeout-ms)))
                                                                        q)))))
                                      (zmq/poll {:timeout 50})
                                      (when-not @stop?
                                        (recur deadline))))
                       :stopped)))

(defn start-pp-worker!
  [{:keys [backend-addr ready-addr ctrl-addr worker-id hb-interval-ms stop-after-requests]}]
  (worker-daemon {}
                     (let [dealer (zmq/socket :dealer {:connect backend-addr
                                                       :identity worker-id})
                           ready (zmq/socket :push {:connect ready-addr})
                           ctrl (zmq/socket :pull {:bind ctrl-addr})
                           stop? (atom false)]
                       (zmq/send-msg ready "READY")
                       (zmq/send-msg dealer ["READY"])
                       (loop [last-hb 0
                              handled 0]
                         (if @stop?
                           :stopped
                           (let [now (System/currentTimeMillis)
                                 last-hb'
                                 (if (>= (- now last-hb) hb-interval-ms)
                                   (do (zmq/send-msg dealer ["HEARTBEAT"]) now)
                                   last-hb)
                                 msg (zmq/receive-msg dealer {:stringify true :timeout 30})
                                 handled'
                                 (if msg
                                   (let [client-id (nth msg 0)
                                         payload (nth msg 1)
                                         handled'' (inc handled)]
                                     (zmq/send-msg dealer [client-id (str "OK:" worker-id ":" payload)])
                                     (when (and stop-after-requests (>= handled'' stop-after-requests))
                                       (reset! stop? true))
                                     handled'')
                                   handled)
                                 _ (when (zmq/receive-msg ctrl {:stringify true :timeout 0})
                                     (reset! stop? true))]
                             (recur last-hb' handled'))))
                       :stopped)))

(deftest paranoid-pirate-failover-test
  (with-fast-context
    (let [frontend-addr (inproc "pp-frontend")
          backend-addr (inproc "pp-backend")
          broker-ready-addr (inproc "pp-broker-ready")
          broker-ctrl-addr (inproc "pp-broker-ctrl")
          w-ready-addr (inproc "pp-worker-ready")
          w1-ctrl-addr (inproc "pp-w1-ctrl")
          w2-ctrl-addr (inproc "pp-w2-ctrl")
          broker-ready-pull (zmq/socket :pull {:bind broker-ready-addr})
          w-ready-pull (zmq/socket :pull {:bind w-ready-addr})
          broker-f (start-pp-broker! {:frontend-addr frontend-addr
                                      :backend-addr backend-addr
                                      :ready-addr broker-ready-addr
                                      :ctrl-addr broker-ctrl-addr
                                      :hb-timeout-ms 200})
          _ (or (zmq/receive-msg broker-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 pp-broker READY 超时" {})))
          ;; 让 w1 先 ready, 这样第一条请求大概率落到 w1
          w1-f (start-pp-worker! {:backend-addr backend-addr
                                  :ready-addr w-ready-addr
                                  :ctrl-addr w1-ctrl-addr
                                  :worker-id "w1"
                                  :hb-interval-ms 50
                                  :stop-after-requests 1})
          _ (or (zmq/receive-msg w-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 pp-worker-1 READY 超时" {})))
          w2-f (start-pp-worker! {:backend-addr backend-addr
                                  :ready-addr w-ready-addr
                                  :ctrl-addr w2-ctrl-addr
                                  :worker-id "w2"
                                  :hb-interval-ms 50
                                  :stop-after-requests nil})
          _ (or (zmq/receive-msg w-ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 pp-worker-2 READY 超时" {})))
          client (zmq/socket :req {:connect frontend-addr
                                   :identity "pp-client"})
          _ (zmq/send-msg client "PING1")
          r1 (zmq/receive-msg client {:stringify true :timeout 1000})
          _ (Thread/sleep 350)
          _ (zmq/send-msg client "PING2")
          r2 (zmq/receive-msg client {:stringify true :timeout 1000})
          stop! (fn [addr]
                  (let [p (zmq/socket :push {:connect addr})]
                    (zmq/send-msg p "STOP")))]
      (is (some? r1))
      (is (some? r2))
      ;; 第二次必须走 w2(因为 w1 停止心跳后被剔除)
      (is (= true (str/includes? (first r2) "w2")))
      (stop! w1-ctrl-addr)
      (stop! w2-ctrl-addr)
      (stop! broker-ctrl-addr)
      (await! w1-f 1500 "pp-worker-1")
      (await! w2-f 1500 "pp-worker-2")
      (await! broker-f 1500 "pp-broker"))))

;; ## 13. 背压与流控: HWM, linger, 非阻塞发送

;; 典型应用场景: 生产者发送速度高于消费者处理速度时, 你需要一个明确的策略:
;;
;; - 限制队列长度(HWM)
;; - 非阻塞发送(超时为 0)
;; - 拥塞时丢弃/降级/计数
;;
;; 这个测试用一个极小 HWM 来强制出现 send 失败, 以便观察行为.

(deftest backpressure-hwm-test
  (with-fast-context
    (let [addr (inproc "hwm")
          pull (zmq/socket :pull {:bind addr :receive-hwm 1})
          push (zmq/socket :push {:connect addr :send-hwm 1 :linger 0})
          results (doall (for [i (range 2000)]
                           (zmq/send-msg push (str "m" i) {:timeout 0})))
          sent (count (filter true? results))
          failed (count (filter false? results))]
      (is (pos? sent))
      (is (pos? failed)))))

;; ## 14. 进程间通信: tcp:// 的部署形态(非 inproc)

;; 典型应用场景: 跨进程或跨机器通信, 需要走 `tcp://` 或 `ipc://`.
;;
;; 本测试用 `tcp://127.0.0.1:<port>` 演示, 并用 READY 同步避免连接时序问题.

(defn free-tcp-port
  []
  (with-open [ss (ServerSocket. 0)]
    (.getLocalPort ss)))

(deftest tcp-transport-test
  (with-fast-context
    (let [port (free-tcp-port)
          addr (str "tcp://127.0.0.1:" port)
          ready-addr (inproc "tcp-ready")
          ready-pull (zmq/socket :pull {:bind ready-addr})
          server-f
          (worker-daemon {}
                             (let [rep (zmq/socket :rep {:bind addr})
                                   ready (zmq/socket :push {:connect ready-addr})
                                   _ (zmq/send-msg ready "READY")
                                   req (or (zmq/receive-msg rep {:stringify true :timeout 1000})
                                           (throw (ex-info "tcp server 等待请求超时" {:addr addr})))]
                               (zmq/send-msg rep ["OK"])
                               req))
          _ (or (zmq/receive-msg ready-pull {:stringify true :timeout 1000})
                (throw (ex-info "等待 tcp server READY 超时" {:addr addr})))
          client (zmq/socket :req {:connect addr})
          _ (zmq/send-msg client "HELLO")
          reply (zmq/receive-msg client {:stringify true :timeout 1000})
          req (await! server-f 1500 "tcp-server")]
      (is (= ["OK"] reply))
      (is (= ["HELLO"] req)))))

;; ## 15. 更多 ZeroMQ 应用场景速查

;; 下面是更常见的落地场景, 以及推荐的 socket 组合:
;;
;; - 微服务内部 RPC: `REQ/REP` 或 `DEALER/ROUTER`(需要并发与更复杂路由时)
;; - Broker/负载均衡: `ROUTER/DEALER` + `REP` workers(本文第 7 节)
;; - Pub/Sub 分层广播: `XSUB/XPUB` proxy(本文第 9 节)
;; - 控制面/服务发现: `REQ/REP` 写入与拉取, `PUB/SUB` 订阅变更(本文第 10 节)
;; - 可靠性: Lazy Pirate 重试, Paranoid Pirate 失活剔除(本文第 11/12 节)
;; - 背压与流控: HWM + 非阻塞发送 + 拥塞策略(本文第 13 节)
;; - 跨进程/跨机器: `tcp://` 或 `ipc://`(本文第 14 节)
;;
;; 工程建议:
;;
;; - 把消息格式看作协议: 定义帧结构与版本字段, 不要靠隐式约定
;; - 对 `PUB/SUB` 需要考虑订阅尚未就绪导致的丢消息, 用本文的 READY 同步或引入应用层确认
;; - 设置超时与中断策略: 避免线程永久阻塞导致关闭困难
;; - socket 不是线程安全的, 每个线程独立创建与使用自己的 socket

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(run-tests)
