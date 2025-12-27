;; # 构建一个编码 Agent: 第 3 部分 - 自动发现工具
;;
;; 本笔记本演示如何用元数据发现工具, 构建工具列表与注册表, 并模拟工具调用流程。
(ns notes.llm-agent-part3
  (:require [nextjournal.clerk :as clerk]))

;; ## 元数据标注工具
;; 给函数增加 :tool 元数据, 保持描述与实现靠近, 便于统一给 LLM。
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
  (str "模拟天气: " location " -25.0 " (or unit "celsius")))

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "echo_back"
           :description "Echo user content"
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

;; ## 模拟 LLM 工具调用并调度执行
^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-call
  "给定工具调用描述与注册表, 返回 tool 消息。"
  [tool-call registry]
  (let [name (get-in tool-call [:function :name])
        args (get-in tool-call [:function :arguments])
        f (get registry name)]
    {:tool_call_id (:id tool-call)
     :role "tool"
     :content (if f
                (f args)
                (str "未知工具: " name))}))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def sample-tool-flow
  "模拟 LLM 触发工具调用的消息流。"
  (let [call {:id "call-123"
              :function {:name "get_current_weather"
                         :arguments {:location "Paris, France"
                                     :unit "celsius"}}}]
    (handle-tool-call call tool-registry)))

;; ## 小结
;; - 使用 :tool 元数据统一描述与实现
;; - get-tool-list 提供给 LLM 的工具列表
;; - build-tool-registry 让工具调用映射到 Clojure 函数
;; - handle-tool-call 模拟调度调用, 方便本地验证消息结构
