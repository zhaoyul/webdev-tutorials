;; # 构建一个编码 Agent: 第 4 部分 - 一个基本可用的编码 Agent
;;
;; 本笔记本用安全的临时目录模拟文件读/列/改工具, 并使用 DeepSeek API 测试编码 Agent 能力。
(ns notes.llm-agent-part4
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [notes.llm-agent-common :as common]
            [clojure.data.json :as json])
  (:import (java.nio.file Files)))

;; ## 共享配置

^{::clerk/visibility {:code :show :result :hide}}
(def deepseek-config common/deepseek-config)

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion common/create-chat-completion)

^{::clerk/visibility {:code :show :result :hide}}
(def add-message common/add-message)

;; ## 准备一个临时工作区

^{::clerk/visibility {:code :show :result :hide}}
(def temp-root
  (Files/createTempDirectory "agent-demo" (into-array java.nio.file.attribute.FileAttribute [])))

^{::clerk/visibility {:code :show :result :hide}}
(defn temp-path
  [& parts]
  (->> parts
       (cons (.toString temp-root))
       (str/join java.io.File/separator)))

;; ## 工具实现 (含 :tool 元数据)

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "read_file"
           :description "Read file content from a safe temp workspace"
           :parameters {:type "object"
                        :properties {:path {:type "string"}}
                        :required ["path"]}}}
  read-file*
  [{:keys [path]}]
  (try
    (slurp path)
    (catch Exception e (str "读取失败: " (.getMessage e)))))

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "list_files"
           :description "List files and directories at given path"
           :parameters {:type "object"
                        :properties {:path {:type "string"}}
                        :required ["path"]}}}
  list-files*
  [{:keys [path]}]
  (let [dir (io/file path)]
    (if (.isDirectory dir)
      (mapv (fn [f]
              {:name (.getName f)
               :type (if (.isDirectory f) "directory" "file")})
            (.listFiles dir))
      [])))

^{::clerk/visibility {:code :show :result :hide}}
(defn
  ^{:tool {:name "edit_file"
           :description "Search & replace in a file (creates if missing)"
           :parameters {:type "object"
                        :properties {:path {:type "string"}
                                     :old_str {:type "string"}
                                     :new_str {:type "string"}}
                        :required ["path" "old_str" "new_str"]}}}
  edit-file*
  [{:keys [path old_str new_str]}]
  (if (= old_str new_str)
    "old_str 与 new_str 不可相同"
    (let [file (io/file path)]
      (when-not (.exists file)
        (spit file ""))
      (let [content (slurp file)]
        (if (not (str/includes? content old_str))
          (str "未找到片段: " old_str)
          (let [updated (str/replace content old_str new_str)]
            (spit file updated)
            (str "已替换: " old_str " -> " new_str)))))))

;; ## 注册表 & 调度

^{::clerk/visibility {:code :show :result :hide}}
(def tool-registry
  {"read_file" read-file*
   "list_files" list-files*
   "edit_file" edit-file*})

^{::clerk/visibility {:code :show :result :hide}}
(defn invoke-tool
  [tool-name args]
  (if-let [f (get tool-registry tool-name)]
    (f args)
    (str "未知工具: " tool-name)))

^{::clerk/visibility {:code :show :result :hide}}
(defn handle-tool-calls
  "处理 LLM 返回的工具调用"
  [assistant-message]
  (when-let [tool-calls (:tool_calls assistant-message)]
    (mapv (fn [tc]
            (let [name (get-in tc [:function :name])
                  args (json/read-str (get-in tc [:function :arguments]) :key-fn keyword)]
              {:role "tool"
               :tool_call_id (:id tc)
               :content (invoke-tool name args)}))
          tool-calls)))

;; ## 工具定义列表 (用于 API)

^{::clerk/visibility {:code :show :result :hide}}
(def file-tools
  [{:type "function"
    :function {:name "read_file"
               :description "Read file content"
               :parameters {:type "object"
                            :properties {:path {:type "string"}}
                            :required ["path"]}}}
   {:type "function"
    :function {:name "list_files"
               :description "List files in directory"
               :parameters {:type "object"
                            :properties {:path {:type "string"}}
                            :required ["path"]}}}
   {:type "function"
    :function {:name "edit_file"
               :description "Edit file by search and replace"
               :parameters {:type "object"
                            :properties {:path {:type "string"}
                                         :old_str {:type "string"}
                                         :new_str {:type "string"}}
                            :required ["path" "old_str" "new_str"]}}}])

;; ## 演示: 构建并修改 fizzbuzz.js (本地工具执行)

^{::clerk/visibility {:code :show :result :hide}}
(def fizz-path (temp-path "fizzbuzz.js"))

^{::clerk/visibility {:code :show :result :show}}
(def create-file-step
  (invoke-tool "edit_file" {:path fizz-path
                            :old_str ""
                            :new_str "const run=()=>{for(let i=1;i<=20;i++){const fb=(i%15===0)?\"FizzBuzz\":(i%3===0)?\"Fizz\":(i%5===0)?\"Buzz\":i;console.log(fb);}};run();"}))

^{::clerk/visibility {:code :show :result :show}}
(def list-after-create
  (invoke-tool "list_files" {:path (.toString temp-root)}))

^{::clerk/visibility {:code :show :result :show}}
(def tighten-range
  (invoke-tool "edit_file" {:path fizz-path
                            :old_str "i<=20"
                            :new_str "i<=15"}))

^{::clerk/visibility {:code :show :result :show}}
(def read-final
  (invoke-tool "read_file" {:path fizz-path}))

;; ## 真实 DeepSeek API 测试: 让 LLM 使用文件工具

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def llm-file-demo
  "测试 DeepSeek API 使用文件工具"
  (let [messages [{:role "system"
                   :content (str "You have access to file tools. Working directory: " (.toString temp-root))}
                  {:role "user"
                   :content "Read the file fizzbuzz.js and tell me what it does"}]
        ;; 第一次调用 - LLM 决定使用 read_file
        response1 (create-chat-completion messages deepseek-config :tools file-tools)
        assistant-msg1 (-> response1 :choices first :message)
        ;; 执行工具
        tool-results (handle-tool-calls assistant-msg1)
        ;; 构建包含工具结果的历史
        history-with-results (reduce add-message messages (concat [assistant-msg1] tool-results))
        ;; 第二次调用获取最终回复
        response2 (when (seq tool-results)
                    (create-chat-completion history-with-results deepseek-config :tools file-tools))]
    {:llm_tool_calls (:tool_calls assistant-msg1)
     :tool_execution_results tool-results
     :final_response (when response2 (-> response2 :choices first :message :content))
     :usage (:usage response1)}))

;; ## 结果说明
;; - create-file-step: 利用 edit_file 创建并写入 fizzbuzz.js
;; - list-after-create: 显示临时目录里的文件
;; - tighten-range: 再次 edit_file 将循环上限替换为 15
;; - read-final: 查看最终文件内容
;; - llm-file-demo: DeepSeek API 调用展示 LLM 自主使用文件工具

(comment
  (clerk/serve! {})
  )
