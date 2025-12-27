;; # 构建一个编码 Agent: 第 6 部分 - 集成模型上下文协议 (MCP)
;;
;; 本笔记本用模拟数据演示如何把 MCP 服务器工具并入 Agent 的工具列表, 并在调用时路由到远端。
(ns notes.llm-agent-part6
  (:require [nextjournal.clerk :as clerk]))

;; ## 模拟 MCP 服务器返回的工具描述
^{::clerk/visibility {:code :show :result :hide}}
(def mock-mcp-tools
  [{:name "read_text_file"
    :description "Read a UTF-8 text file"
    :inputSchema {:type "object" :properties {:path {:type "string"}} :required ["path"]}}
   {:name "list_directory"
    :description "List entries in a directory"
    :inputSchema {:type "object" :properties {:path {:type "string"}}}}])

;; ## 将 MCP 工具转为 OpenAI 兼容格式
^{::clerk/visibility {:code :show :result :hide}}
(defn mcp->openai-tool
  [{:keys [name description inputSchema]}]
  {:type "function"
   :function {:name name
              :description description
              :parameters inputSchema}})

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def openai-tools-from-mcp
  (mapv mcp->openai-tool mock-mcp-tools))

;; ## 本地工具 (保留一个 shell 执行工具)
^{::clerk/visibility {:code :show :result :hide}}
(def local-tools
  [{:type "function"
    :function {:name "run_shell_command"
               :description "Run bash command"
               :parameters {:type "object"
                            :properties {:command {:type "string"}}
                            :required ["command"]}}}])

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def merged-tool-list
  (vec (concat local-tools openai-tools-from-mcp)))

;; ## 调用路由: 先本地注册表, 再 MCP 客户端
^{::clerk/visibility {:code :show :result :hide}}
(def local-registry
  {"run_shell_command" (fn [{:keys [command]}] (str "本地执行: " command))})

^{::clerk/visibility {:code :show :result :hide}}
(defn mock-mcp-call
  "模拟 MCP 客户端调用远端工具。"
  [tool-name args]
  (str "MCP 调用 " tool-name " 参数 " args))

^{::clerk/visibility {:code :show :result :hide}}
(defn invoke-tool
  [name args]
  (if-let [f (get local-registry name)]
    (f args)
    (mock-mcp-call name args)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-invoke-local
  (invoke-tool "run_shell_command" {:command "echo hi"}))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-invoke-mcp
  (invoke-tool "read_text_file" {:path "deps.edn"}))

;; ## 小结
;; - mcp->openai-tool: 将 MCP 工具描述转为 LLM 接口可用的格式
;; - merged-tool-list: 本地工具 + MCP 工具组合
;; - invoke-tool: 简单路由, 本地找不到就委派 MCP
;; 真实项目中需用官方 mcp-java-sdk 创建 transport/client; 本笔记使用纯模拟保证可执行。
