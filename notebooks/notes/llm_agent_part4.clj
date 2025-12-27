;; # 构建一个编码 Agent: 第 4 部分 - 一个基本可用的编码 Agent
;;
;; 本笔记本用安全的临时目录模拟文件读/列/改工具, 展示编码 Agent 的基础能力。
(ns notes.llm-agent-part4
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.nio.file Files)))

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

;; ## 演示: 构建并修改 fizzbuzz.js
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

;; ## 结果说明
;; - create-file-step: 利用 edit_file 创建并写入 fizzbuzz.js
;; - list-after-create: 显示临时目录里的文件
;; - tighten-range: 再次 edit_file 将循环上限替换为 15
;; - read-final: 查看最终文件内容
;; 通过简单的搜索替换工具, 即可驱动 LLM 写/改代码。
