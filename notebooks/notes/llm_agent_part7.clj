;; # 构建一个编码 Agent: 第 7 部分 - 支持多个 MCP 服务器
;;
;; 本笔记用模拟数据演示如何在多 MCP 服务器场景下构建工具注册表并路由调用。
(ns notes.llm-agent-part7
  (:require [nextjournal.clerk :as clerk]))

;; ## 模拟两个 MCP 服务器
^{::clerk/visibility {:code :show :result :hide}}
(def mcp-servers
  {:filesystem [{:name "list_directory"
                 :description "List directory entries"
                 :inputSchema {:type "object" :properties {:path {:type "string"}}}}
                {:name "read_file"
                 :description "Read file content"
                 :inputSchema {:type "object" :properties {:path {:type "string"}}}}]
   :sequential-thinking [{:name "sequentialthinking"
                          :description "Generate numbered thoughts"
                          :inputSchema {:type "object"
                                        :properties {:prompt {:type "string"}
                                                     :count {:type "integer"}}
                                        :required ["prompt"]}}]})

;; ## 转换工具描述
^{::clerk/visibility {:code :show :result :hide}}
(defn mcp->openai-tool
  [tool]
  {:type "function"
   :function {:name (:name tool)
              :description (:description tool)
              :parameters (:inputSchema tool)}})

^{::clerk/visibility {:code :show :result :hide}}
(defn build-tool-list
  "合并所有服务器的工具为一个列表。"
  [servers]
  (->> servers
       vals
       (apply concat)
       (mapv mcp->openai-tool)))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def merged-tools
  (build-tool-list mcp-servers))

;; ## 构建注册表: 工具名 -> 调用函数 (已绑定对应服务器)
^{::clerk/visibility {:code :show :result :hide}}
(defn make-dispatch-fn
  [server-name tool-name]
  (fn [args]
    (str "调用 " (name server-name) "/" tool-name " 参数 " args)))

^{::clerk/visibility {:code :show :result :hide}}
(defn build-tool-registry
  [servers]
  (reduce-kv
   (fn [acc server-name tools]
     (reduce (fn [m {:keys [name]}]
               (assoc m name (make-dispatch-fn server-name name)))
             acc tools))
   {} servers))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def registry
  (build-tool-registry mcp-servers))

;; ## 演示: 思考工具 + 文件系统工具
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def call-thinking
  ((registry "sequentialthinking") {:prompt "写 CLJS 阶乘前先思考" :count 3}))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def call-read-file
  ((registry "read_file") {:path "deps.edn"}))

;; ## 小结
;; - build-tool-list 组合多服务器工具供 LLM 选择
;; - build-tool-registry 绑定调用函数以便按名称路由到对应服务器
;; - make-dispatch-fn 示例中仅返回字符串; 真实场景需调用具体 MCP 客户端
