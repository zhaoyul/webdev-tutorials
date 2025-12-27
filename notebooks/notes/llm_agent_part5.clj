;; # 构建一个编码 Agent: 第 5 部分 - 为 Agent 赋予更多能力 (run_shell_command)
;;
;; 本笔记本演示在工具集中加入 shell 命令执行, 让 Agent 具备自清理与自动化能力。
(ns notes.llm-agent-part5
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

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

;; ## 小结
;; 通过 run_shell_command:
;; - 可以让 LLM 探索目录、生成/删除文件
;; - 可以配合 list_files/edit_file 组合完成项目 scaffold 与清理
;; - 执行结果需要检查 exit code, 本示例返回 stdout 或错误信息
