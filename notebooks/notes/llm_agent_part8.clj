;; # 构建一个编码 Agent: 第 8 部分 - 使用本地模型 (Ollama 接口示例)
;;
;; 本笔记展示如何通过配置切换到本地兼容 OpenAI 接口的模型 (如 Ollama), 代码保持可运行的模拟。
(ns notes.llm-agent-part8
  (:require [nextjournal.clerk :as clerk]))

;; ## 配置示例
;; 远端 OpenAI/Azure:
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def remote-config
  {:api-key "sk-REMOTE"
   :api-endpoint "https://api.openai.com/v1"
   :impl :openai
   :model "gpt-4.1"})

;; 本地 Ollama (兼容 OpenAI 接口):
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def local-config
  {:api-key "ollama-placeholder"
   :api-endpoint "http://localhost:11434/v1"
   :impl :openai
   :model "llama3.2:latest"})

;; ## 模拟调用封装
^{::clerk/visibility {:code :show :result :hide}}
(defn call-llm
  "模拟 chat-completion 调用; 真实环境将替换为 openai/create-chat-completion."
  [config messages]
  {:endpoint (:api-endpoint config)
   :model (:model config)
   :messages messages
   :mock-response (str "使用模型 " (:model config) " 返回示例答案")})

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-remote
  (call-llm remote-config [{:role "user" :content "Hello?"}]))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-local
  (call-llm local-config [{:role "user" :content "Hello?"}]))

;; ## 小结与注意
;; - 将 :api-endpoint 指向 Ollama 的 11434 端口即可复用同一套代码
;; - 选择支持工具调用的模型 (如 llama3.2 工具版) 才能驱动 Agent 流程
;; - 本笔记用模拟返回确保可执行; 接入真实模型时替换 call-llm 为 openai-clojure 调用
