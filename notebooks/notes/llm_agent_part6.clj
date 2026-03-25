;; # 构建一个编码 Agent: 第 6 部分 - 集成模型上下文协议 (MCP)
;;
;; 本笔记本演示如何把 MCP 服务器工具并入 Agent 的工具列表, 并使用 DeepSeek API 进行真实调用测试。
(ns notes.llm-agent-part6
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
(defn run-shell-local
  [{:keys [command]}]
  (let [{:keys [out err exit]} (clojure.java.shell/sh "bash" "-lc" command)]
    (if (zero? exit) out (str "Error: " err))))

^{::clerk/visibility {:code :show :result :hide}}
(def local-registry
  {"run_shell_command" run-shell-local
   "read_text_file" (fn [{:keys [path]}] (str "(MCP模拟) 读取 " path ": 文件内容..."))
   "list_directory" (fn [{:keys [path]}] (str "(MCP模拟) 列出 " path ": [file1.txt, dir1/]"))})

^{::clerk/visibility {:code :show :result :hide}}
(defn invoke-tool
  [name args]
  (if-let [f (get local-registry name)]
    (f args)
    (str "未知工具: " name)))

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  [assistant-message]
  (when-let [tool-calls (:tool_calls assistant-message)]
    (mapv (fn [tc]
            (let [name (get-in tc [:function :name])
                  args (json/read-str (get-in tc [:function :arguments]) :key-fn keyword)]
              {:role "tool"
               :tool_call_id (:id tc)
               :content (invoke-tool name args)}))
          tool-calls)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-invoke-local
  (invoke-tool "run_shell_command" {:command "echo hi"}))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-invoke-mcp
  (invoke-tool "read_text_file" {:path "deps.edn"}))

;; ## DeepSeek API 测试: 混合本地工具与 MCP 工具

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def llm-mcp-demo
  "使用 DeepSeek API 测试混合工具调用"
  (let [messages [{:role "system"
                   :content "You have access to shell commands and MCP filesystem tools. Use them as needed."}
                  {:role "user"
                   :content "List the current directory and then check what files are available using the appropriate tools."}]
        response1 (create-chat-completion messages deepseek-config :tools merged-tool-list)
        assistant-msg1 (-> response1 :choices first :message)
        tool-results (handle-tool-calls assistant-msg1)
        history-with-results (reduce add-message messages (concat [assistant-msg1] tool-results))
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-results deepseek-config :tools merged-tool-list))]
    {:available_tools (mapv #(get-in % [:function :name]) merged-tool-list)
     :llm_tool_calls (:tool_calls assistant-msg1)
     :tool_results tool-results
     :final_response (when response2 (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 小结
;; - mcp->openai-tool: 将 MCP 工具描述转为 LLM 接口可用的格式
;; - merged-tool-list: 本地工具 + MCP 工具组合
;; - invoke-tool: 简单路由, 本地找不到就委派 MCP
;; - DeepSeek API 调用展示了 LLM 在混合工具环境下的决策能力

(comment
  (clerk/serve! {})
  )
