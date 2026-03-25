;; # 构建一个编码 Agent: 第 1 部分 - 一个基本的 LLM 聊天循环
;;
;; 本笔记本将 llm-agent.org 第一部分拆分为可执行片段, 使用 DeepSeek API 进行真实测试。
(ns notes.llm-agent-part1
  (:require [nextjournal.clerk :as clerk]
            [notes.llm-agent-common :as common]))

;; ## 为什么要写这个 Agent?
;; 作者希望用 Clojure 复现 Thorsten Ball 的编码 Agent 教程, 目标是:
;; - 只用一个 LLM 聊天接口, 快速搭出可以回复消息的 Agent
;; - 代码保持最小可行, 先跑通对话, 再逐步增强
;; - 使用 DeepSeek API 进行真实测试 (OpenAI 兼容格式)

;; ## 聊天补全的工作方式
;; LLM 需要完整对话历史, 每条消息带有角色:
;; {:role "user" :content "..."} / {:role "assistant" :content "..."}
;; 将新输入追加进历史, 送入 chat completion API, 即可得到连续回复。

;; ## 配置
;; API 配置从 llm-config.edn 文件读取 (该文件被 .gitignore 忽略)
;; 共享函数定义在 notes.llm-agent-common 命名空间

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def deepseek-config
  "DeepSeek API 配置 (从 llm-config.edn 读取)"
  common/deepseek-config)

;; ## 调用真实 DeepSeek API

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion
  "调用 DeepSeek API"
  common/create-chat-completion)

^{::clerk/visibility {:code :show :result :hide}}
(def extract-assistant-message
  "从 API 响应中提取 assistant 消息"
  common/extract-assistant-message)

^{::clerk/visibility {:code :show :result :hide}}
(def add-message
  "向历史追加一条消息"
  common/add-message)

;; ## 测试: 调用真实 DeepSeek API

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def real-api-test
  "使用 DeepSeek API 进行真实调用测试"
  (let [messages [(add-message [] "system" "You are a helpful assistant. Reply in Chinese.")
                  (add-message [] "user" "你好, 请用一句话介绍 Clojure 语言")]
        messages (vec (remove empty? messages))
        messages [{:role "system" :content "You are a helpful assistant. Reply in Chinese."}
                  {:role "user" :content "你好, 请用一句话介绍 Clojure 语言"}]
        response (create-chat-completion messages deepseek-config)]
    {:request messages
     :response (extract-assistant-message response)
     :usage (:usage response)}))

;; ## 模拟实现 (用于离线演示)

^{::clerk/visibility {:code :show :result :hide}}
(defn mock-llm-response
  "根据用户消息生成固定回复, 方便在笔记本里演示循环。"
  [messages]
  (let [latest (-> messages last :content)]
    {:role "assistant"
     :content (str "LLM 回声: " latest)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn run-once
  "模拟读取一条用户消息, 得到 LLM 回复, 返回新的历史。"
  [history user-input]
  (let [messages (add-message history "user" user-input)
        assistant (mock-llm-response messages)]
    (add-message messages (:role assistant) (:content assistant))))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def demo-history
  "演示对话历史的生成 (使用 mock LLM)。"
  (-> []
      (run-once "你好, 这是第一轮")
      (run-once "再来一轮吧")))

;; ## 多轮对话测试 (真实 API)

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def multi-turn-test
  "测试多轮对话 (使用 DeepSeek API)"
  (let [history [{:role "system" :content "You are a helpful assistant. Reply in Chinese."}]
        ;; 第一轮
        history-with-q1 (add-message history "user" "什么是函数式编程?")
        response1 (create-chat-completion history-with-q1 deepseek-config)
        assistant1 (extract-assistant-message response1)
        history-with-a1 (add-message history-with-q1 (:role assistant1) (:content assistant1))
        ;; 第二轮
        history-with-q2 (add-message history-with-a1 "user" "Clojure 是纯函数式语言吗?")
        response2 (create-chat-completion history-with-q2 deepseek-config)
        assistant2 (extract-assistant-message response2)]
    {:conversation history-with-q2
     :final-response assistant2
     :usage (:usage response2)}))

;; ## 与 llm-agent.org 原文的对比

;; 原文使用 openai-clojure 库:
;; (openai/create-chat-completion
;;   {:model (:model config)
;;    :messages messages}
;;   (select-keys config [:api-key :api-endpoint :impl]))

;; 本笔记本使用 notes.llm-agent-common 提供的封装函数调用 DeepSeek API:
;; (common/create-chat-completion messages config)

;; 两者本质相同, 都是发送 HTTP POST 请求到兼容 OpenAI 的端点.

;; ## 交互示例 (原文)

;; LLM => Howdy! How can I help you today?
;; User => How is the weather in Paris today?
;; LLM => ... (模型解释没有实时天气数据, 给出十月大致天气)
;; User => quit

;; 完成最小聊天循环后, 下一步将接入工具调用 (见 part2)。

(comment
  (clerk/serve! {})
  )
