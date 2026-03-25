;; # 构建一个编码 Agent: 第 5 部分 - 为 Agent 赋予更多能力 (run_shell_command)
;;
;; 本笔记本演示在工具集中加入 shell 命令执行, 并使用 DeepSeek API 测试 Agent 的自清理与自动化能力。
(ns notes.llm-agent-part5
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [notes.llm-agent-common :as common]
            [clojure.data.json :as json]))

;; ## 共享配置

^{::clerk/visibility {:code :show :result :hide}}
(def deepseek-config common/deepseek-config)

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion common/create-chat-completion)

^{::clerk/visibility {:code :show :result :hide}}
(def add-message common/add-message)

;; ## 工具定义

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "run_shell_command"
           :description "Run bash command and return stdout/stderr"
           :parameters {:type "object"
                        :properties {:command {:type "string"}}
                        :required ["command"]}}}
  run-shell-command*
  [{:keys [command]}]
  (try
    (let [{:keys [out err exit]} (shell/sh "bash" "-lc" command)]
      (if (zero? exit)
        out
        (str "Error: " err)))
    (catch Exception e
      (str "Exception: " (.getMessage e)))))

;; ## 演示 1: 用 shell 创建与查看目录

^{::clerk/visibility {:code :show :result :show}}
(def demo-mkdir-ls
  (run-shell-command* {:command "mkdir -p /tmp/agent-shell-demo && ls -la /tmp/agent-shell-demo"}))

;; ## 演示 2: 生成文件并查看内容

^{::clerk/visibility {:code :show :result :show}}
(def demo-write-cat
  (run-shell-command* {:command "echo 'Hello from run_shell_command' > /tmp/agent-shell-demo/msg.txt && cat /tmp/agent-shell-demo/msg.txt"}))

;; ## 演示 3: 自清理

^{::clerk/visibility {:code :show :result :show}}
(def demo-clean
  (run-shell-command* {:command "rm -rf /tmp/agent-shell-demo && ls -la /tmp/agent-shell-demo 2>/dev/null || echo 'cleaned'"}))

;; ## DeepSeek API 测试: 让 LLM 使用 shell 工具

^{::clerk/visibility {:code :show :result :hide}}
(def shell-tool
  {:type "function"
   :function {:name "run_shell_command"
              :description "Execute bash commands to explore system, create files, or run programs"
              :parameters {:type "object"
                           :properties {:command {:type "string" :description "Bash command to execute"}}
                           :required ["command"]}}})

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  [assistant-message]
  (when-let [tool-calls (:tool_calls assistant-message)]
    (mapv (fn [tc]
            (let [name (get-in tc [:function :name])
                  args (json/read-str (get-in tc [:function :arguments]) :key-fn keyword)]
              {:role "tool"
               :tool_call_id (:id tc)
               :content (when (= name "run_shell_command")
                          (run-shell-command* args))}))
          tool-calls)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def llm-shell-demo
  "使用 DeepSeek API 测试 shell 工具调用"
  (let [messages [{:role "system"
                   :content "You can use shell commands to explore the system. Always explain what command you will run."}
                  {:role "user"
                   :content "What files are in the current directory? Use the shell tool to find out."}]
        response1 (create-chat-completion messages deepseek-config :tools [shell-tool])
        assistant-msg1 (-> response1 :choices first :message)
        tool-results (handle-tool-calls assistant-msg1)
        history-with-results (reduce add-message messages (concat [assistant-msg1] tool-results))
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-results deepseek-config :tools [shell-tool]))]
    {:llm_decision (:tool_calls assistant-msg1)
     :command_output tool-results
     :final_response (when response2 (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 小结
;; 通过 run_shell_command:
;; - 可以让 LLM 探索目录、生成/删除文件
;; - 可以配合 list_files/edit_file 组合完成项目 scaffold 与清理
;; - 执行结果需要检查 exit code, 本示例返回 stdout 或错误信息
;; - DeepSeek API 调用展示了 LLM 自主使用 shell 工具的能力

(comment
  (clerk/serve! {})
  )
