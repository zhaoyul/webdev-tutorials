;; # 构建一个编码 Agent: 第 8 部分 - 模型配置对比 (DeepSeek / OpenAI / Ollama)
;;
;; 本笔记展示如何配置不同模型提供商, 并使用 DeepSeek API 进行真实调用测试。
(ns notes.llm-agent-part8
  (:require [nextjournal.clerk :as clerk]
            [notes.llm-agent-common :as common]))

;; ## 配置示例

;; DeepSeek (从 llm-config.edn 读取):
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def deepseek-config
  common/deepseek-config)

;; 远端 OpenAI (示例):
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def openai-config
  {:api-key "sk-REMOTE-KEY"
   :api-endpoint "https://api.openai.com/v1/chat/completions"
   :model "gpt-4.1"})

;; 本地 Ollama (兼容 OpenAI 接口):
^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def ollama-config
  {:api-key "ollama-placeholder"
   :api-endpoint "http://localhost:11434/v1/chat/completions"
   :model "llama3.2:latest"})

;; ## 统一调用封装 (来自 notes.llm-agent-common)

^{::clerk/visibility {:code :show :result :hide}}
(def create-chat-completion
  "调用兼容 OpenAI 格式的 API"
  common/create-chat-completion)

;; ## DeepSeek API 真实测试

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def deepseek-test
  "使用 DeepSeek API 进行真实调用"
  (let [messages [{:role "system" :content "You are a helpful assistant. Reply in Chinese."}
                  {:role "user" :content "简要介绍函数式编程的特点"}]
        response (create-chat-completion messages deepseek-config)]
    {:model (:model response)
     :content (-> response :choices first :message :content)
     :usage (:usage response)}))

;; ## 模型对比

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def model-comparison
  "不同模型配置对比"
  [{:name "DeepSeek"
    :config deepseek-config
    :features ["OpenAI 兼容 API" "中文支持好" "工具调用支持"]}
   {:name "OpenAI"
    :config openai-config
    :features ["行业标准" "稳定可靠" "工具调用成熟"]}
   {:name "Ollama (本地)"
    :config ollama-config
    :features ["完全本地运行" "无需联网" "需要支持工具调用的模型"]}])

;; ## 配置切换示例

^{::clerk/visibility {:code :show :result :hide}}
(defn select-config
  "根据环境选择配置"
  [env]
  (case env
    :deepseek deepseek-config
    :openai openai-config
    :local ollama-config
    deepseek-config))

^{::clerk/auto-expand-results? true
  ::clerk/visibility {:code :show :result :show}}
(def current-config
  (select-config :deepseek))

;; ## 小结
;; - DeepSeek: 使用 llm-config.edn 配置，中文表现优秀，支持工具调用
;; - OpenAI: 行业标准，稳定可靠，工具调用生态成熟
;; - Ollama: 本地运行，隐私性好，需要选择支持工具调用的模型
;; - 所有配置均使用 OpenAI 兼容格式，代码可以无缝切换
;; - 共享函数 create-chat-completion 定义在 notes.llm-agent-common

(comment
  (clerk/serve! {})
  )
