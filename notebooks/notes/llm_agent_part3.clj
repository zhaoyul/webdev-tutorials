;; # 构建一个编码 Agent: 第 3 部分 - 自动发现工具
;;
;; 本笔记本演示如何用元数据发现工具, 构建工具列表与注册表, 并使用 DeepSeek API 进行真实工具调用测试。
(ns notes.llm-agent-part3
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

;; ## 元数据标注工具

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "get_current_weather"
           :description "Retrieves the current weather information for a specified location"
           :parameters {:type "object"
                        :properties {:location {:type "string"}
                                     :unit {:type "string"
                                            :enum ["celsius" "fahrenheit"]
                                            :default "celsius"}}
                        :required ["location"]}}}
  get-current-weather
  [{:keys [location unit]}]
  (let [temps {"Beijing" 22 "Shanghai" 25 "Paris" 18 "London" 16}
        temp (get temps location 20)
        unit-str (if (= unit "fahrenheit") "F" "C")
        temp-val (if (= unit "fahrenheit") (+ 32 (* temp 1.8)) temp)]
    (str location " 当前温度: " (int temp-val) "°" unit-str)))

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "echo_back"
           :description "Echo user content back"
           :parameters {:type "object"
                        :properties {:content {:type "string"}}
                        :required ["content"]}}}
  echo-back
  [{:keys [content]}]
  (str "Echo: " content))

;; ## 发现工具列表

^{::clerk/visibility {:code :show :result :hide}}
(defn tool-metadata?
  [v]
  (some? (:tool (meta v))))

^{::clerk/visibility {:code :show :result :hide}}
(defn get-tool-list
  "扫描命名空间内带 :tool 元数据的函数, 生成 OpenAI 工具列表。"
  [ns-sym]
  (->> (ns-publics ns-sym)
       vals
       (filter tool-metadata?)
       (mapv #(hash-map :type "function" :function (:tool (meta %))))))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def discovered-tools
  (get-tool-list 'notes.llm-agent-part3))

;; ## 构建工具注册表

^{::clerk/visibility {:code :show :result :hide}}
(defn build-tool-registry
  "name -> fn 形式的工具注册表, 便于按名称调用。"
  [ns-sym]
  (->> (ns-publics ns-sym)
       vals
       (filter tool-metadata?)
       (reduce (fn [acc f]
                 (assoc acc (get-in (meta f) [:tool :name]) f))
               {})))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def tool-registry
  (build-tool-registry 'notes.llm-agent-part3))

;; ## 工具调用处理

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  "处理 LLM 返回的工具调用"
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

;; ## 真实 API 测试: 自动发现工具并调用

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def auto-tool-demo
  "使用自动发现的工具列表进行 DeepSeek API 调用"
  (let [tools discovered-tools
        messages [{:role "system"
                   :content "You have access to weather and echo tools. Use them when appropriate."}
                  {:role "user"
                   :content "What's the weather in Paris?"}]
        ;; 第一次调用
        response1 (create-chat-completion messages deepseek-config :tools tools)
        assistant-msg1 (-> response1 :choices first :message)
        ;; 执行工具
        tool-results (handle-tool-calls assistant-msg1 tool-registry)
        ;; 第二次调用获取最终回复
        history-with-tools (reduce add-message messages (concat [assistant-msg1] tool-results))
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-tools deepseek-config :tools tools))]
    {:discovered_tools (mapv #(get-in % [:function :name]) tools)
     :assistant_decision (:tool_calls assistant-msg1)
     :tool_results tool-results
     :final_answer (when response2 (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 小结
;; - 使用 :tool 元数据统一描述与实现
;; - get-tool-list 自动扫描提供给 LLM 的工具列表
;; - build-tool-registry 让工具调用映射到 Clojure 函数
;; - 真实 API 调用展示工具自动发现与执行流程

(comment
  (clerk/serve! {})
  )
