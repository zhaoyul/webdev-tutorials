;; # 构建一个编码 Agent: 第 2 部分 - 为 Agent 添加工具
;;
;; 本笔记本继续 llm-agent.org 第二部分, 展示如何在聊天循环中接入工具调用, 使用 DeepSeek API 进行真实测试。
(ns notes.llm-agent-part2
  (:require [nextjournal.clerk :as clerk]
            [notes.llm-agent-common :as common]
            [clojure.data.json :as json]))

;; ## 引入工具的动机
;; 仅靠 LLM 记忆, 模型无法访问外部世界。我们给 Agent 提供 `get_current_weather` 工具, 让 LLM 能请求外部数据。

;; ## 共享配置
;; 从 notes.llm-agent-common 导入配置和函数

^{::clerk/visibility {:code :show :result :hide}}
(def deepseek-config common/deepseek-config)

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion common/create-chat-completion)

^{::clerk/visibility {:code :show :result :hide}}
(def add-message common/add-message)

;; ## 工具元数据定义

^{::clerk/visibility {:code :show :result :hide}}
(def weather-tool
  "天气查询工具定义"
  {:type "function"
   :function {:name "get_current_weather"
              :description "Get the current weather in a given location"
              :parameters {:type "object"
                           :properties {:location {:type "string"
                                                   :description "City name, e.g. Beijing"}
                                        :unit {:type "string"
                                               :enum ["celsius" "fahrenheit"]
                                               :default "celsius"}}
                           :required ["location"]}}})

^{::clerk/visibility {:code :show :result :hide}}
(defn get-current-weather
  "实际执行天气查询的工具函数"
  [{:keys [location unit]}]
  (let [temps {"Beijing" 22 "Shanghai" 25 "Paris" 18 "New York" 15}
        temp (get temps location 20)
        unit-str (if (= unit "fahrenheit") "F" "C")
        temp-val (if (= unit "fahrenheit") (+ 32 (* temp 1.8)) temp)]
    (str location " 当前温度: " (int temp-val) "°" unit-str)))

;; ## 工具调用处理

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  "处理工具调用并返回工具结果消息"
  [assistant-message]
  (when-let [tool-calls (:tool_calls assistant-message)]
    (mapv (fn [tc]
            (let [name (get-in tc [:function :name])
                  args (json/read-str (get-in tc [:function :arguments]) :key-fn keyword)
                  result (when (= name "get_current_weather")
                           (get-current-weather args))]
              {:role "tool"
               :tool_call_id (:id tc)
               :content (str result)}))
          tool-calls)))

;; ## 真实 API 测试: 工具调用流程

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def tool-call-demo
  "使用 DeepSeek API 测试工具调用流程"
  (let [messages [{:role "system"
                   :content "You are a helpful assistant. Use the weather tool when asked about weather."}
                  {:role "user"
                   :content "What's the weather in Beijing?"}]
        ;; 第一次调用: LLM 决定使用工具
        response1 (create-chat-completion messages deepseek-config
                                          :tools [weather-tool]
                                          :tool_choice "auto")
        assistant-msg1 (-> response1 :choices first :message)
        ;; 如果有工具调用，执行工具并再次调用
        tool-results (handle-tool-calls assistant-msg1)
        ;; 构建包含工具结果的历史
        history-with-tool (reduce add-message messages
                                  (concat [assistant-msg1] tool-results))
        ;; 第二次调用: 获取最终回复
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-tool deepseek-config
                                            :tools [weather-tool]
                                            :tool_choice "auto"))]
    {:first_response {:content (:content assistant-msg1)
                      :tool_calls (:tool_calls assistant-msg1)}
     :tool_results tool-results
     :final_response (when response2
                       (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 对比: 不使用工具的纯文本回复

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def no-tool-demo
  "不使用工具时的纯文本回复"
  (let [messages [{:role "system"
                   :content "You are a helpful assistant."}
                  {:role "user"
                   :content "What's the weather in Beijing?"}]
        response (create-chat-completion messages deepseek-config)]
    {:response (-> response :choices first :message :content)
     :usage (:usage response)}))

;; ## 与 llm-agent.org 原文对比

;; 原文使用 openai-clojure 库:
;; (openai/create-chat-completion
;;   {:model (:model config)
;;    :messages messages
;;    :tools [...]
;;    :tool_choice "auto"}
;;   {:api-key ... :api-endpoint ...})

;; 本笔记使用 notes.llm-agent-common/create-chat-completion，参数格式相同。

;; ## 工具调用消息结构说明

;; 当 LLM 决定调用工具时，响应中的 :tool_calls 格式:
;; [{:id "call_xxx"
;;   :type "function"
;;   :function {:name "get_current_weather"
;;              :arguments "{\"location\":\"Beijing\",\"unit\":\"celsius\"}"}}]

;; 工具执行后返回的消息:
;; {:role "tool"
;;  :tool_call_id "call_xxx"
;;  :content "Beijing 当前温度: 22°C"}

(comment
  (clerk/serve! {})
  )
