;; # LLM Agent 共享配置
;;
;; 本命名空间提供 LLM API 配置的公共函数, 被所有 llm_agent_part 笔记本共享.
(ns notes.llm-agent-common
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [hato.client :as hc]
            [clojure.data.json :as json]))

(defn load-config
  "从外部 edn 文件加载配置.
   默认读取项目根目录的 llm-config.edn 文件.
   如果文件不存在, 返回 MISSING 占位符."
  ([] (load-config "llm-config.edn"))
  ([path]
   (if-let [file (io/file path)]
     (if (.exists file)
       (edn/read-string (slurp file))
       {:api-key "MISSING" :api-endpoint "" :model "unknown"})
     {:api-key "MISSING" :api-endpoint "" :model "unknown"})))

(def deepseek-config
  "DeepSeek API 配置 (从 llm-config.edn 读取)."
  (load-config))

(defn create-chat-completion
  "调用兼容 OpenAI 格式的 API 进行聊天补全.
   支持可选的 tools 和 tool_choice 参数."
  [messages config & {:keys [tools tool_choice]}]
  (let [{:keys [api-key api-endpoint model]} config
        body (cond-> {:model model
                      :messages messages
                      :temperature 0.7
                      :max_tokens 1024}
               tools (assoc :tools tools)
               tool_choice (assoc :tool_choice tool_choice))
        response (hc/post api-endpoint
                          {:headers {"Authorization" (str "Bearer " api-key)
                                     "Content-Type" "application/json"}
                           :body (json/write-str body)
                           :timeout 60000})]
    (when (= 200 (:status response))
      (json/read-str (:body response) :key-fn keyword))))

(defn extract-assistant-message
  "从 API 响应中提取第一条 assistant 消息."
  [response]
  (let [choice (first (:choices response))
        message (:message choice)]
    {:role (:role message)
     :content (:content message)
     :tool_calls (:tool_calls message)}))

(defn add-message
  "向历史追加一条消息.
   支持两种调用方式:
   - (add-message history role content) - 展开参数
   - (add-message history {:role ... :content ...}) - 传入消息 map"
  ([history role content]
   (conj (or history []) {:role role :content content}))
  ([history message]
   (conj (or history []) message)))

(defn handle-tool-calls
  "处理 LLM 返回的工具调用.
   registry 是一个 map: 工具名 -> 执行函数.
   返回 tool 消息列表."
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
