;; # 构建一个编码 Agent: 第 2 部分 - 为 Agent 添加工具
;;
;; 本笔记本继续 llm-agent.org 第二部分, 展示如何在聊天循环中接入工具调用, 以天气查询为例。
(ns notes.llm-agent-part2
  (:require [nextjournal.clerk :as clerk]))

;; ## 引入工具的动机
;; 仅靠 LLM 记忆, 模型无法访问外部世界。我们给 Agent 提供 `get_current_weather` 工具, 让 LLM 能请求外部数据。

;; ## OpenAI 工具元数据 (原文示例)
;; {
;;   "type": "function",
;;   "name": "get_current_weather",
;;   "description": "Retrieves current weather for the given location.",
;;   "parameters": {
;;     "type": "object",
;;     "properties": {
;;       "location": {"type": "string", "description": "City and country e.g. Bogotá, Colombia"},
;;       "units": {"type": "string", "enum": ["celsius", "fahrenheit"], "description": "Units the temperature will be returned in."}
;;     },
;;     "required": ["location", "unit"],
;;     "additionalProperties": false
;;   },
;;   "strict": true
;; }
;; 这些信息会和 messages 一起传给 chat completion 接口。

;; ## 工具调用的消息结构
;; 当 LLM 决定调用工具时会给出:
;; {"id":"fc_67890abc","call_id":"call_67890abc","type":"function_call","name":"get_current_weather","arguments":"{\"location\":\"Bogotá, Colombia\"}"}
;; 客户端运行工具后写回:
;; {"type":"function_call_output","call_id":"call_67890abc","content":"-26 C","role":"tool"}

;; ## 调用形态 (原文代码片段)
;; (openai/create-chat-completion
;;  {:model (:model config)
;;   :messages messages
;;   :tools [{:type "function"
;;            :function {:name "get_current_weather"
;;                       :description "Get the current weather in a given location"
;;                       :parameters {:type "object"
;;                                    :properties {:location {:type "string"}
;;                                                 :unit {:type "string"
;;                                                        :enum ["celsius" "fahrenheit"]}}}}}]
;;   :tool_choice "auto"}
;;  (select-keys config [:api-key :api-endpoint :impl]))
;; 下面用可运行的简化版演示消息流, 代码可直接执行。

;; 演示专用的工具规格与假回复
^{::clerk/visibility {:code :show :result :hide}}
(def tool-spec
  "用于展示的工具元数据, 仅在文档中显示。"
  {:type "function"
   :function {:name "get_current_weather"
              :description "Get the current weather in a given location"
              :parameters {:type "object"
                           :properties {:location {:type "string"}
                                        :unit {:type "string"
                                               :enum ["celsius" "fahrenheit"]}}}}})

^{::clerk/visibility {:code :show :result :hide}}
(defn mock-tool-run
  "模拟工具运行, 根据 call id 返回固定温度。"
  [{:keys [id arguments]}]
  {:type "function_call_output"
   :tool_call_id id
   :role "tool"
   :content (str (:location arguments) " 当前温度 -26 C")})

^{::clerk/visibility {:code :show :result :hide}}
(defn mock-llm-with-tool
  "模拟 LLM: 如果历史中存在需要工具的数据, 返回包含工具调用的消息。"
  [messages]
  (let [last-user (-> messages last :content)]
    (if (re-find #"天气" last-user)
      {:role "assistant"
       :content "调用工具获取实时天气..."
       :tool_calls [{:id "call_001"
                     :type "function_call"
                     :name "get_current_weather"
                     :arguments {:location last-user :unit "celsius"}}]}
      {:role "assistant"
       :content (str "纯文本回复: " last-user)})))

^{::clerk/visibility {:code :show :result :hide}}
(defn add-msg
  "追加一条消息到历史。"
  [history message]
  (conj (or history []) message))

^{::clerk/visibility {:code :show :result :hide}}
(defn dispatch-tools
  "将工具调用转换为工具输出消息列表。"
  [assistant-message]
  (mapv mock-tool-run (:tool_calls assistant-message)))

^{::clerk/visibility {:code :show :result :hide}}
(defn step-with-tools
  "处理一条用户输入, 如有工具调用则把结果并回历史。"
  [history user-input]
  (let [with-user (add-msg history {:role "user" :content user-input})
        assistant (mock-llm-with-tool with-user)
        history+assistant (add-msg with-user assistant)
        tool-messages (dispatch-tools assistant)]
    (reduce add-msg history+assistant tool-messages)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def tool-demo-history
  "演示工具调用的历史演进。"
  (-> []
      (step-with-tools "帮我看看巴黎的天气")
      (step-with-tools "谢谢, 再见")))

;; ## 完整循环思路
;; 真实代码会:
;; 1. 把工具元数据 (`tool-spec`) 放入 LLM 请求。
;; 2. 收到含 `:tool_calls` 的 assistant 消息后, 逐个运行工具, 生成 `:role \"tool\"` 的输出。
;; 3. 把工具输出写入历史, 再次调用 LLM, 获得最终回答。
;; 上面的 `tool-demo-history` 是这一流程的可运行模拟, 结构与真实交互一致。
