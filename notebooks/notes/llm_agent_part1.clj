;; # 构建一个编码 Agent: 第 1 部分 - 一个基本的 LLM 聊天循环
;;
;; 本笔记本将 llm-agent.org 第一部分拆分为可执行片段, 用可运行的简化循环演示聊天历史的生成。
(ns notes.llm-agent-part1
  (:require [nextjournal.clerk :as clerk]))

;; ## 为什么要写这个 Agent?
;; 作者希望用 Clojure 复现 Thorsten Ball 的编码 Agent 教程, 目标是:
;; - 只用一个 LLM 聊天接口, 快速搭出可以回复消息的 Agent
;; - 代码保持最小可行, 先跑通对话, 再逐步增强
;; LLM 选择 gpt-5-mini, 客户端库是 openai-clojure (依赖未在本仓库开启, 此处用模拟实现演示调用形态)。

;; ## 聊天补全的工作方式
;; LLM 需要完整对话历史, 每条消息带有角色:
;; {:role "user" :content "..."} / {:role "assistant" :content "..."}
;; 将新输入追加进历史, 送入 chat completion API, 即可得到连续回复。

;; ## 依赖与调用形态 (原文片段)
;; deps.edn 片段:
;; {:deps {org.clojure/clojure {:mvn/version "1.12.3"}
;;         net.clojars.wkok/openai-clojure {:mvn/version "0.23.0"}}}
;; 调用形态:
;; (openai/create-chat-completion {:model (:model config)
;;                                 :messages messages}
;;                                (select-keys config [:api-key :api-endpoint :impl]))

;; ## 主循环概念 (原文思路)
;; 读取用户输入 → 追加到历史 → 调用 LLM → 把回复写回历史 → 打印结果。
;; 下面用可执行的假 LLM 复现消息流, 便于在笔记本中直接运行。

;; 为了演示, 用一个可预测的假 LLM, 避免依赖真实 API。
^{::clerk/visibility {:code :show :result :hide}}
(defn mock-llm-response
  "根据用户消息生成固定回复, 方便在笔记本里演示循环。"
  [messages]
  (let [latest (-> messages last :content)]
    {:role "assistant"
     :content (str "LLM 回声: " latest)}))

^{::clerk/visibility {:code :show :result :hide}}
(defn add-message
  "向历史追加一条消息。"
  [history role content]
  (conj (or history []) {:role role :content content}))

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
  "演示对话历史的生成。"
  (-> []
      (run-once "你好, 这是第一轮")
      (run-once "再来一轮吧")))

;; ## 配置文件格式
;; llm.edn 示例:
;; {:api-key "REPLACE-YOUR-KEY"
;;  :api-endpoint "https://<ENDPOINT>.openai.azure.com"
;;  :impl :azure
;;  :model "gpt-5-mini"}

;; ## 交互示例
;; 原文展示了询问巴黎天气的对话。此处用示例文本记录:
;; LLM => Howdy! How can I help you today?
;; User => How is the weather in Paris today?
;; LLM => ... (模型解释没有实时天气数据, 给出十月大致天气)
;; User => quit
;; 完成最小聊天循环后, 下一步将接入工具调用。

(comment
  (clerk/serve! {})
  )
