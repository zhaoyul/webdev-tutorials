;; # 构建一个编码 Agent: 第 7 部分 - 支持多个 MCP 服务器
;;
;; 本笔记演示如何在多 MCP 服务器场景下构建工具注册表并路由调用, 使用 DeepSeek API 进行真实测试。
(ns notes.llm-agent-part7
  (:require [nextjournal.clerk :as clerk]
            [notes.llm-agent-common :as common]
            [clojure.data.json :as json]))

;; ## 共享配置

^{::clerk/visibility {:code :show :result :hide}}
(def deepseek-config common/deepseek-config)

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion common/create-chat-completion)

^{::clerk/visibility {:code :show :result :hide}}
(def add-message common/add-message)

;; ## 模拟两个 MCP 服务器

^{::clerk/visibility {:code :show :result :hide}}
(def mcp-servers
  {:filesystem [{:name "list_directory"
                 :description "List directory entries"
                 :inputSchema {:type "object" :properties {:path {:type "string"}}}}
                {:name "read_file"
                 :description "Read file content"
                 :inputSchema {:type "object" :properties {:path {:type "string"}}}}]
   :sequential-thinking [{:name "sequentialthinking"
                          :description "Generate numbered thoughts for problem solving"
                          :inputSchema {:type "object"
                                        :properties {:prompt {:type "string"}
                                                     :count {:type "integer"}}
                                        :required ["prompt"]}}]})

;; ## 转换工具描述

^{::clerk/visibility {:code :show :result :hide}}
(defn mcp->openai-tool
  [tool]
  {:type "function"
   :function {:name (:name tool)
              :description (:description tool)
              :parameters (:inputSchema tool)}})

^{::clerk/visibility {:code :show :result :hide}}
(defn build-tool-list
  "合并所有服务器的工具为一个列表。"
  [servers]
  (->> servers
       vals
       (apply concat)
       (mapv mcp->openai-tool)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def merged-tools
  (build-tool-list mcp-servers))

;; ## 构建注册表: 工具名 -> 调用函数 (已绑定对应服务器)

^{::clerk/visibility {:code :show :result :hide}}
(defn make-dispatch-fn
  [server-name tool-name]
  (fn [args]
    (case server-name
      :filesystem (case tool-name
                    "list_directory" (str "Listed: " (:path args))
                    "read_file" (str "Content of " (:path args) ": ..."))
      :sequential-thinking (str "Thoughts about " (:prompt args) ": 1... 2... 3...")
      (str "Unknown server: " server-name))))

^{::clerk/visibility {:code :show :result :hide}}
(defn build-tool-registry
  [servers]
  (reduce-kv
   (fn [acc server-name tools]
     (reduce (fn [m {:keys [name]}]
               (assoc m name (make-dispatch-fn server-name name)))
             acc tools))
   {} servers))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def registry
  (build-tool-registry mcp-servers))

;; ## 演示: 思考工具 + 文件系统工具

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def call-thinking
  ((registry "sequentialthinking") {:prompt "写 CLJS 阶乘前先思考" :count 3}))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def call-read-file
  ((registry "read_file") {:path "deps.edn"}))

;; ## DeepSeek API 测试: 多服务器工具决策

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  [assistant-message registry]
  (when-let [tool-calls (:tool_calls assistant-message)]
    (mapv (fn [tc]
            (let [name (get-in tc [:function :name])
                  args (json/read-str (get-in tc [:function :arguments]) :key-fn keyword)
                  f (get registry name)]
              {:role "tool"
               :tool_call_id (:id tc)
               :content (if f (f args) (str "未知工具: " name))}))
          tool-calls)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def llm-multi-server-demo
  "使用 DeepSeek API 测试多服务器工具调用"
  (let [messages [{:role "system"
                   :content "You have access to filesystem and thinking tools from different servers. Use the right tool for the task."}
                  {:role "user"
                   :content "I need to read a file and think about its contents. First list the directory, then use thinking to analyze what you find."}]
        response1 (create-chat-completion messages deepseek-config :tools merged-tools)
        assistant-msg1 (-> response1 :choices first :message)
        tool-results (handle-tool-calls assistant-msg1 registry)
        history-with-results (reduce add-message messages (concat [assistant-msg1] tool-results))
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-results deepseek-config :tools merged-tools))]
    {:servers (keys mcp-servers)
     :all_tools (mapv #(get-in % [:function :name]) merged-tools)
     :llm_first_call (:tool_calls assistant-msg1)
     :tool_outputs tool-results
     :final_response (when response2 (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 小结
;; - build-tool-list 组合多服务器工具供 LLM 选择
;; - build-tool-registry 绑定调用函数以便按名称路由到对应服务器
;; - DeepSeek API 调用展示了 LLM 在多服务器环境下选择合适工具的能力

(comment
  (clerk/serve! {})
  )
