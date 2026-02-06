;; # 📖 Clerk 之书
^{:nextjournal.clerk/visibility {:code :hide}}
(ns nextjournal.clerk.book
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/open-graph
   {:url "[https://book.clerk.vision](https://book.clerk.vision)"
    :title "Clerk 之书"
    :description "Clerk 的官方文档. "
    }}
  (:require [clojure.string :as str]
            [emmy.env :as emmy]
            [emmy.expression]
            [next.jdbc :as jdbc]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.analyzer :as ana]
            [nextjournal.clerk.eval :as eval]
            [nextjournal.clerk.parser :as parser]
            [nextjournal.clerk.viewer :as v]
            [weavejester.dependency :as dep])
  (:import (javax.imageio ImageIO)
           (java.net URL)))

;; ## ⚖️ 原理

;; 计算笔记本允许通过混合散文与可执行代码来从证据中论证. 有关用户在传统笔记本(如 Jupyter)中遇到的问题的良好概述, 请参阅[我不喜欢笔记本](https://www.youtube.com/watch?v=7jiPeIFXb6U)和[计算笔记本有什么问题? 痛点, 需求和设计机会](https://www.microsoft.com/en-us/research/uploads/prod/2020/03/chi20c-sub8173-cam-i16.pdf).

;; Clerk 特别希望解决以下问题:

;; * 不如我的编辑器好用
;; * 笔记本代码难以重用
;; * 乱序执行导致的再现问题
;; * 存档和将笔记本放入版本控制的问题

;; Clerk 是一个 Clojure 笔记本库, 旨在通过减少功能来解决这些问题, 具体如下:

;; * **没有编辑环境**: 用户可以继续使用他们熟悉和喜爱的编辑器.
;; * **没有新格式**: Clerk 笔记本要么是常规的 Clojure 命名空间(夹杂着 Markdown 注释), 要么是常规的 Markdown 文件(夹杂着 Clojure 代码块). 这也意味着 Clerk 笔记本旨在存储在版本控制中.
;; * **没有乱序执行**: Clerk 笔记本总是从上到下评估. Clerk 构建 Clojure 变量的依赖图, 并且只重新计算所需的更改以保持快速的反馈循环.
;; * **没有外部进程**: Clerk Clojure 进程中运行, 使 Clerk 可以访问类路径上的所有代码.

;; ## 🚀 入门

;; Clerk 需要 Java 11 或更高版本, 并已安装 [`clojure`](https://www.google.com/search?q=%5Bhttps://clojure.org/guides/install_clojure%5D\(https://clojure.org/guides/install_clojure\)).

;; ### 🤹 Clerk 演示

;; 如果不熟悉 Clerk, 建议克隆并使用 [nextjournal/clerk-demo](https://github.com/nextjournal/clerk-demo) 仓库.
;; ```sh
;; git clone git@github.com:nextjournal/clerk-demo.git
;; cd clerk-demo
;; ```

;; 然后在编辑器中打开项目中的 `dev/user.clj` 并启动一个 REPL 进入项目. 有关编辑器特定说明, 请参阅:
;; * [Emacs & Cider](https://docs.cider.mx/cider/basics/up_and_running.html#launch-an-nrepl-server-from-emacs)
;; * [Calva](https://calva.io/jack-in-guide/)
;; * [Cursive](https://cursive-ide.com/userguide/repl.html)
;; * [Vim & Neovim](https://github.com/clojure-vim/vim-jack-in)

;; ### 🔌 在现有项目中

;; 要在项目中使用 Clerk, 将以下依赖项添加到`deps.edn`:k

;; ```clojure
;; {:deps {io.github.nextjournal/clerk {:mvn/version "0.18.1150"}}}
;; ```

;; 在系统启动时, 例如在 `user.clj` 中, 要求并启动 Clerk:

;; ```clojure
;; (require '[nextjournal.clerk :as clerk])

;; ;; 在默认端口 7777 上启动 Clerk 的内置 Web 服务器, 完成后打开浏览器
;; (clerk/serve! {:browse? true})

;; ;; 要么显式调用 `clerk/show!` 来显示给定的笔记本, 要么使用下面描述的文件监视器.
;; (clerk/show! "notebooks/rule_30.clj")
;; ```

;; 然后可以通过 [http://localhost:7777](https://www.google.com/search?q=http://localhost:7777) 访问 Clerk.

;; ### 🕙文件监视器

;; 可以使用 `clerk/show!` 函数加载, 求值和呈现文件, 但在大多数情况下, 使用类似以下代码启动文件监视器会更容易:

;; ``` clojure
;; (clerk/serve! {:watch-paths ["notebooks" "src"]})
;; ```
;; ... 这将自动重新加载并重新求值任何更改的 Clojure (clj) 或 Markdown (md) 文件, 并在浏览器中显示最新更改的文件.

;; 为了使其足够高效以提供良好的体验, Clerk 会缓存它在评估每个文件时执行的计算. 同样, 为了确保它不会一次向浏览器发送太多数据, Clerk 会在交互式查看器中对数据结构进行分页.

;; ### 🔪 编辑器集成

;; 文件监视器的一种推荐替代方法是在编辑器中设置一个热键来保存并 `clerk/show!` 当前活动文件.

;; **Emacs**

;; 在 Emacs 中, 将以下内容添加到配置中:

;; ``` el
;; (defun clerk-show ()
;;  (interactive)
;;  (when-let
;;    ((filename
;;     (buffer-file-name)))
;;   (save-buffer)
;;   (cider-interactive-eval
;;   (concat "(nextjournal.clerk/show! \"" filename "\")"))))
;;
;; (define-key clojure-mode-map (kbd "<M-return>") 'clerk-show)
;;  ```

;; **IntelliJ/Cursive**

;; 在 IntelliJ/Cursive 中, 可以通过以下方式[设置 REPL 命令](https://cursive-ide.com/userguide/repl.html#repl-commands):

;; * 转到 `Tools→REPL→Add New REPL Command`, 然后
;; * 添加以下命令: `(show! "file-path")`;
;; * 确保该命令在 `nextjournal.clerk` 命名空间中执行;
;; * 最后通过 `Settings→Keymap` 分配选择的快捷方式

;; **Neovim + Conjure**

;; 使用 [neovim](https://neovim.io/) + [conjure](https://github.com/Olical/conjure/), 可以使用以下 vimscript 函数保存文件并用 Clerk 显示:

;; ```vimscript
;; function! ClerkShow()
;; exe "w"
;; exe "ConjureEval (nextjournal.clerk/show! "" . expand("%:p") . "")"
;; endfunction

;; nmap <silent> <localleader>cs :execute ClerkShow()<CR>
;; ```

;; ## 🔍 查看器(viewer)

;; Clerk 内置了许多有用的查看器, 例如用于 Clojure 数据, HTML 和 Hiccup, 表格, 图表等.

;; 当显示大型数据结构时, Clerk 的默认查看器将对结果进行分页.

;; ### 🧩 Clojure 数据
;; 默认的查看器集能够渲染 Clojure 数据.
(def clojure-data
  {:你好 "世界 👋"
   :蛋糕 (map #(repeat % '🍰) (range 1 30))
   :思路 "可视化的\n目的是\n洞察力, \n而不是\n图片. "})

;; 查看器可以处理惰性无限序列, 默认情况下部分加载数据, 并能够根据请求加载更多数据.
(range)

(def fib (lazy-cat [0 1] (map + fib (rest fib))))

;; 此外, 还有许多可以通过函数显式调用的内置查看器.

;; ### 🌐 Hiccup, HTML 和 SVG

;; `html` 查看器在传递向量时会解释 `hiccup`.
(clerk/html [:div "作为 Clojurians, 我们" [:em "真的"] "很喜欢 Hiccup"])

;; 或者, 可以传递一个 HTML 字符串.
(clerk/html "永远不要<strong>忘记</strong>. ")

;; 可以使用 [Tailwind CSS](https://tailwindcss.com/docs/utility-first) 来样式化元素.
(clerk/html [:button.bg-sky-500.hover:bg-sky-700.text-white.rounded-xl.px-2.py-1 "✨ Tailwind CSS"])

;; `html` 查看器能够显示 SVG, 接受 Hiccup 向量或 SVG 字符串.
(clerk/html [:svg {:width 500 :height 100}
             [:circle {:cx 25 :cy 50 :r 25 :fill "blue"}]
             [:circle {:cx 100 :cy 75 :r 25 :fill "red"}]])

;; 还可以在 Hiccup 中嵌入其他查看器.

(clerk/html [:div.flex.justify-center.space-x-60
             [:p "旁边有一个表格"]
             (clerk/table [[1 2] [3 4]])])

;; ### 🔢 表格

;; Clerk 提供了一个内置的数据表查看器, 开箱即用地支持三种最常见的表格数据形状:
;; 映射序列(其中每个映射的键是列名); 序列的序列(即一个值网格, 带有一个可选的标题);
;; 映射的序列(其中键是列名, 行是该列的值).

(clerk/table [[1 2]
              [3 4]]) ;; 序列的序列

(clerk/table (clerk/use-headers [["奇数" "偶数"]
                                 [1 2]
                                 [3 4]])) ;; 带标题的序列的序列

(clerk/table [{"奇数" 1 "偶数" 2}
              {"奇数" 3 "偶数" 4}]) ;; 映射序列

(clerk/table {"奇数" [1 3]
              "偶数" [2 4]}) ;; 序列的映射

;; 在内部, 表格查看器会将上述所有内容标准化为带有 `:rows` 和可选 `:head` 键的映射,
;; 也可以控制列的顺序.
(clerk/table {:head ["奇数" "偶数"]
              :rows [[1 2] [3 4]]}) ;; 带有 `:rows` 和可选 `:head` 键的映射

;; 要自定义表格查看器中的行数, 设置 `::clerk/page-size`. 使用 `nil` 值显示所有行.
(clerk/table {::clerk/page-size 7} (map (comp vector (partial str "行 #")) (range 1 31)))

;; 内置的表格查看器在其 `:add-viewers` 键上添加了一些子查看器.
;; 这些子查看器控制表格的标记和字符串的显示(以关闭表格单元格内的引用).
(:add-viewers v/table-viewer)

;; 修改 `:add-viewers` 键允许我们创建一个自定义表格查看器, 以不同方式显示缺失值.
(def table-viewer-custom-missing-values
  (update v/table-viewer :add-viewers v/add-viewers [(assoc v/table-missing-viewer :render-fn '(fn [x] [:span.red "N/A"]))]))

^{::clerk/viewer table-viewer-custom-missing-values}
{:A [1 2 3] :B [1 3] :C [1 2]}

;; ### 🧮 TeX

;; 如我们所见, 所有注释块都可以包含 TeX(我们内部使用 [KaTeX](https://katex.org/)).
;; 此外, 可以以编程方式调用 TeX 查看器. 例如, 这里是麦克斯韦方程组的微分形式:
(clerk/tex "
\\begin{alignedat}{2}
 \\nabla\\cdot\\vec{E} = \\frac{\\rho}{\\varepsilon_0} & \\qquad \\text{高斯定律} \\\\
 \\nabla\\cdot\\vec{B} = 0 & \\qquad \\text{高斯定律(
B

  场)} \\\\
 \\nabla\\times\\vec{E} = -\\frac{\\partial \\vec{B}}{\\partial t} & \\qquad \\text{法拉第定律} \\\\
 \\nabla\\times\\vec{B} = \\mu_0\\vec{J}+\\mu_0\\varepsilon_0\\frac{\\partial\\vec{E}}{\\partial t} & \\qquad \\text{安培定律}
\\end{alignedat}
")
;; ### 📊 Plotly

;; Clerk 还内置了对 Plotly 简洁绘图的支持.
;; 请参阅 [Plotly 的 JavaScript 文档](https://plotly.com/javascript/)以获取更多示例和[选项](https://plotly.com/javascript/configuration-options/).
(clerk/plotly {:data [{:z [[1 2 3] [3 2 1]] :type "surface"}]
               :layout {:margin {:l 20 :r 0 :b 20 :t 20}
                        :paper_bgcolor "transparent"
                        :plot_bgcolor "transparent"}
               :config {:displayModeBar false
                        :displayLogo false}})

;; ### 🗺 Vega Lite

;; 但对于那些喜欢该语法的用户, Clerk 也支持 Vega Lite.
(clerk/vl {:width 650 :height 400 :data {:url "https://vega.github.io/vega-datasets/data/us-10m.json"
                                         :format {:type "topojson" :feature "counties"}}
           :transform [{:lookup "id" :from {:data {:url "https://vega.github.io/vega-datasets/data/unemployment.tsv"}
                                            :key "id" :fields ["rate"]}}]
           :projection {:type "albersUsa"} :mark "geoshape" :encoding {:color {:field "rate" :type "quantitative"}}
           :background "transparent"
           :embed/opts {:actions false}})

;; 可以通过 `:embed/opts` 键向 Vega 查看器提供一个 [embed options](https://github.com/vega/vega-embed#embed) 映射.
;;
;; Clerk 会处理从 EDN 到 JSON 的转换.
;; 官方的 Vega-Lite 示例是 JSON 格式的, 但也有 Clojure/EDN 版本可用:
;; [Carsten Behring 的 EDN 格式 Vega 画廊](https://vlgalleryedn.happytree-bf95e0f8.westeurope.azurecontainerapps.io/).

;; ### 🎼 代码

;; 默认情况下, 代码查看器使用 [clojure-mode](https://nextjournal.github.io/clojure-mode/) 进行语法高亮.
(clerk/code (macroexpand '(when test
                            expression-1
                            expression-2)))

(clerk/code '(ns foo "一个很棒的命名空间" (:require [clojure.string :as str])))

(clerk/code "(defn my-fn\n \"这是一个文档字符串\"\n [args]\n 42)")

;; 可以通过 `::clerk/opts` 指定语法高亮语言.
(clerk/code {::clerk/opts {:language "python"}} "
class Foo(object):
  def **init**(self):
    pass
  def do_this(self):
    return 1")

;; 或者在 Markdown 中使用带语言的代码围栏.

(clerk/md "```c++
#include <iostream>
int main() {
  std::cout << \" Hello, world! \" << std::endl
  return 0
}

```")

;; ### 🏞 图片

;; Clerk 提供 `clerk/image` 查看器, 用于从字符串或任何 `javax.imageio.ImageIO/read` 可接受的(URL, 文件或 InputStream)创建缓冲图像.
;;
;; 例如, 我们可以像这样从 Wiki Commons 获取文森特·梵高著名画作<播种者>(描绘一位农民播种田地)的照片:

#_(clerk/image "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/The_Sower.jpg/1510px-The_Sower.jpg")

;; 我们在使默认图像渲染令人愉悦方面做了一些努力. 查看器使用每张图像的尺寸和纵横比, 以经典的 DWIM 方式猜测最佳显示方式. 例如, 宽度大于 900 像素且纵横比大于二的图像将全宽显示:

(clerk/image "https://images.unsplash.com/photo-1532879311112-62b7188d28ce?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8")

;; 另一方面, 较小的图像会居中并按其固有尺寸显示:

(clerk/image "https://nextjournal.com/data/QmSJ6eu6kUFeWrqXyYaiWRgJxAVQt2ivaoNWc1dtTEADCf?filename=thermo.png&content-type=image/png")

;; 可以将 `clerk/image` 与 `clerk/caption` 一起使用, 后者将在图像下方渲染一个简单的标题:

(clerk/caption
 "造纸工业的工具"
 (clerk/image "https://nextjournal.com/data/QmX99isUndwqBz7nj8fdG7UoDakNDSH1TZcvY2Y6NUTe6o?filename=image.gif&content-type=image/gif"))

;; 标题不限于图像, 可以与提供的任何任意内容一起使用, 例如表格:

^{::clerk/visibility {:code :fold}}
(clerk/caption
 "Solresol 语中的现代对称一元 (7)"
 (clerk/table {:head ["唱名" "法语 IPA" "英语 IPA" "含义"]
               :rows [["Do"	"/do/" "/doʊ/" "不"]
                      ["Re" "/ʁɛ/" "/ɹeɪ/" "和, 也"]
                      ["Mi" "/mi/" "/miː/" "或"]
                      ["Fa" "/fa/" "/fɑː/" "在, 到"]
                      ["Sol" "/sɔl/" "/soʊl/" "但是, 如果"]
                      ["La" "/la/" "/lɑː/" "这个, 然后"]
                      ["Si" "/si/" "/siː/" "是"]]}))

;; ### 📒 Markdown

;; Clerk 用于注释块的 Markdown 支持也可以通过编程方式使用:
(clerk/md (clojure.string/join "\n" (map #(str "* 项目 " (inc %)) (range 3))))

;; 有关摄取 Markdown 文件并将其内容转换为使用 Hiccup 的 HTML 的更高级示例,
;; 请参阅 clerk-demo 仓库中的 [notebooks/markdown.md](https://github.com/nextjournal/clerk-demo/blob/47e95fdc38dd5321632f73bb50a049da4055e041/notebooks/markdown.md).

;; ### 🔠 网格布局

;; 布局可以通过 `row` 和 `col` 进行组合.
;;
;; 将 `:width`, `:height` 或任何其他样式属性传递给 `::clerk/opts` 将把它们分配给包含您项目的行或列. 您可以使用此功能相应地调整容器的大小.

^{::clerk/visibility {:code :hide :result :hide}}
(def image-1 (ImageIO/read (URL. "https://nextjournal.com/data/QmU9dbBd89MUK631CoCtTwBi5fX4Hgx2tTPpiL4VStg8J7?filename=a.gif&content-type=image/gif")))

^{::clerk/visibility {:code :hide :result :hide}}
(def image-2 (ImageIO/read (URL. "https://nextjournal.com/data/QmfKZzHCBQKU7KKXQqcje5cgR6zLge3CcxeuZe8moUkJxf?filename=b.gif&content-type=image/gif")))

^{::clerk/visibility {:code :hide :result :hide}}
(def image-3 (ImageIO/read (URL. "https://nextjournal.com/data/QmXALbNeDD6NSudgVfHE5SvY1Xjzbj7TSWnARqcZrvXsss?filename=c.gif&content-type=image/gif")))


(clerk/row image-1 image-2 image-3)

(clerk/col {::clerk/opts {:width 150}} image-1 image-2 image-3)

;; 布局不限于图像. 您可以使用它来布局任何 Clerk 查看器. 例如, 将其与 HTML 查看器结合使用以渲染漂亮的标题:

(defn caption [text]
  (clerk/html [:figcaption.text-center.mt-1 text]))

(clerk/row
 (clerk/col image-1 (caption "图 1: 装饰性 A"))
 (clerk/col image-2 (caption "图 2: 装饰性 B"))
 (clerk/col image-3 (caption "图 3: 装饰性 C")))

;; 注意: 上面的标题示例**完全**是 Clerk 中 `clerk/caption` 的实现方式.

;; **替代表示法**
;;
;; 默认情况下, `row` 和 `col` 对 `& rest` 进行操作, 因此您可以向函数传递任意数量的项. 但查看器足够智能, 可以接受任何连续的项列表.

(v/row [image-1 image-2 image-3])

;; ### 🍱 组合查看器

;; 查看器可以组合使用, 例如, 您可以使用 Clerk 的网格查看器布局多个独立的 Vega 图表:

^{::clerk/visibility {:code :fold}}
(do
  (def stock-colors
    {"AAPL" "#4c78a8" "AMZN" "#f58518" "GOOG" "#e45756" "IBM" "#72b7b2" "MSFT" "#54a24b"})
  (def combined-stocks-chart
    (clerk/vl {:width 600
               :height 200
               :data {:url "https://vega.github.io/vega-lite/examples/data/stocks.csv"}
               :mark "area"
               :encoding {:x {:timeUnit "yearmonth" :field "date" :axis {:format "%Y"}}
                          :y {:aggregate "sum" :field "price"}
                          :color {:field "symbol"
                                  :scale {:domain (keys stock-colors) :range (vals stock-colors)}}}
               :embed/opts {:actions false}}))
  (defn stock-chart [symbol]
    (clerk/vl {:title symbol
               :width 100
               :height 40
               :mark "area"
               :data {:url "https://vega.github.io/vega-lite/examples/data/stocks.csv"}
               :transform [{:filter (str "datum.symbol == '" symbol "'")}]
               :encoding {:x {:field "date" :type "temporal" :title nil :axis {:grid false}}
                          :y {:field "price" :type "quantitative" :title nil :axis {:grid false} :scale {:domain [0 700]}}
                          :color {:field "symbol" :type "nominal" :legend nil :scale {:domain [symbol]
                                                                                      :range [(get stock-colors symbol)]}}}
               :embed/opts {:actions false}})))

(clerk/col
 (clerk/row (stock-chart "AAPL")
            (stock-chart "AMZN")
            (stock-chart "GOOG")
            (stock-chart "IBM")
            (stock-chart "MSFT"))
 combined-stocks-chart)

;; 查看器也可以嵌入到 Hiccup 中. 下面的示例展示了如何使用它为 `clerk/image` 提供自定义标注.

(clerk/html
 [:div.relative
  (clerk/image "https://images.unsplash.com/photo-1608993659399-6508f918dfde?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2070&q=80")
  [:div.absolute
   {:class "left-[25%] top-[21%]"}
   [:div.border-4.border-emerald-400.rounded-full.shadow
    {:class "w-8 h-8"}]
   [:div.border-t-4.border-emerald-400.absolute
    {:class "w-[80px] rotate-[30deg] left-4 translate-x-[10px] translate-y-[10px]"}]
   [:div.border-4.border-emerald-400.absolute.text-white.font-sans.p-3.rounded-md
    {:class "bg-black bg-opacity-60 text-[13px] w-[280px] top-[66px]"}
    "猫的爪子适合攀爬, 跳跃, 行走和奔跑, 并有可伸缩的爪子用于自卫和狩猎. "]]])

#_(clerk/html [:div.flex.justify-around donut-chart donut-chart donut-chart])

;; ### 🤹🏻 应用查看器

;; **元数据表示法**

;; 在上面的示例中, 我们使用了 `clerk/html` 或 `clerk/plotly` 等便捷辅助函数来将值包装在查看器中.
;; 如果在 REPL 上调用此函数, 注意给定值被包装在一个映射中, 其 `:nextjournal/value` 键下是值,
;; 而查看器在 `:nextjournal/viewer` 键下. 我们称此映射为 `wrapped-value`.

;; 您还可以使用 Clojure 元数据选择查看器, 以避免 Clerk 干扰值.

^{::clerk/viewer clerk/table}
(def my-dataset
  [{:temperature 41.0 :date (java.time.LocalDate/parse "2022-08-01")}
   {:temperature 39.0 :date (java.time.LocalDate/parse "2022-08-01")}
   {:temperature 34.0 :date (java.time.LocalDate/parse "2022-08-01")}
   {:temperature 29.0 :date (java.time.LocalDate/parse "2022-08-01")}])

;; 如上所示, 表格查看器正在应用于 `my-dataset` 变量的值, 而不是变量本身.
;; 如果您希望查看器访问原始变量, 可以通过在查看器上设置真值 `:var-from-def?` 键来选择退出此功能.

^{::clerk/viewer (assoc v/fallback-viewer :var-from-def? true)}
(def raw-var :baz)


;; ### 👁 编写查看器

;; 让我们探讨 Clerk 查看器的工作原理以及如何创建自己的查看器, 以便更好地了解您手头的问题.

v/default-viewers

;; 这些是 Clerk 附带的默认查看器.

(into #{} (map type) v/default-viewers)

;; 每个查看器都是一个简单的 Clojure 映射.


(assoc (frequencies (mapcat keys v/default-viewers)) :total (count v/default-viewers))

;; 我们默认共有 43 个查看器. 让我们从一个简单的示例开始, 并解释查看器 API 中的不同扩展点.


;; #### 🎪 呈现

;; Clerk 的渲染发生在浏览器中. 在 Clojure 端, 给定文档被**呈现**.
;; 呈现操作接受一个值并对其进行转换, 以便 Clerk 可以将其发送到浏览器进行渲染.

;; 让我们从最简单的例子之一开始. 您可以看到 `present` 接受我们的值 `1` 并将其转换为一个映射,
;; 其中 `1` 位于 `:nextjournal/value` 键下, 数字查看器分配给 `:nextjournal/viewer` 键.
;; 我们将此映射称为 `wrapped-value`.

^{::clerk/viewer v/inspect-wrapped-values ::clerk/auto-expand-results? true}
(clerk/present 1)

;; 这个数据结构通过 Clerk 的 websocket 发送到浏览器,
;; 它将使用 `:nextjournal/viewer` 键中找到的 `:render-fn` 进行显示.

;; 现在来看一个稍微复杂一点的例子, `#{1 2 3}`.

^{::clerk/viewer v/inspect-wrapped-values ::clerk/auto-expand-results? true}
(clerk/present #{1 2 3})


;; 这里, 我们给它一个包含 1, 2, 3 的集合. 在其通用形式中, `present` 是一个对给定树进行深度优先遍历的函数,
;; 从根节点开始. 它将为该根节点选择一个查看器, 除非另有指示, 否则会进一步向下遍历树以呈现其子节点.
;;
;; 将此与上面的简单 `1` 示例进行比较! 您应该能够识别叶子值. 还要注意, 容器不再是集合,
;; 而是已转换为向量. 此转换旨在支持大型无序序列(如映射和集合)的分页,
;; 这样我们就可以使用 `get-in` 高效地访问此树中的值.

;; 您可能会问, 为什么我们不直接将未修改的值发送到浏览器.
;; 首先, 我们可能会轻易地用太多数据使浏览器超载. 其次, 我们将看到能够根据 Clojure 和 Java 类型选择查看器的示例,
;; 这些类型无法序列化并发送到浏览器.


;; #### ⚙️ 转换

;; 编写自己的查看器时, 您应该首先考虑的扩展点是 `:transform-fn`.

#_ "练习: 将其包装在 `clerk/present` 中并在 REPL 中调用它"
(v/with-viewer {:transform-fn v/inspect-wrapped-values}
  "探索查看器 API")

;; 如您所见, `:transform-fn` 的参数不仅仅是我们传递的字符串, 而是一个映射,
;; 原始值位于 `:nextjournal/value` 键下. 我们称此映射为 `wrapped-value`.
;; 我们稍后会看到这启用了什么. 但让我们先看看最简单的例子之一.

;; **第一个简单示例**

(def greet-viewer
  {:transform-fn (clerk/update-val #(clerk/html [:strong "你好, " % " 👋"]))})

;; 对于这个简单的 `greet-viewer`, 我们只进行了一个简单的值转换. 为此, `clerk/update-val` 是一个小的辅助函数,
;; 它接受一个函数 `f` 并返回一个只更新 `wrapped-value` 中值的函数, 是 `#(update % :nextjournal/val f)` 的简写.

(v/with-viewer greet-viewer
  "詹姆斯·克拉克·麦克斯韦")

;; `:transform-fn` 在 JVM 上运行, 这意味着您可以通过在 REPL 上调用 `clerk/present` 来探索它的作用.
^{::clerk/viewer v/inspect-wrapped-values}
(clerk/present (v/with-viewer greet-viewer
                 "詹姆斯·克拉克·麦克斯韦"))


;; **将修改后的查看器向下传递给树**

v/table-viewer

(def custom-table-viewer
  (update v/table-viewer :add-viewers v/add-viewers [(assoc v/table-head-viewer :transform-fn (v/update-val (partial map (comp (partial str "列: ") str/capitalize name))))
                                                     (assoc v/table-missing-viewer :render-fn '(fn [x] [:span.red "N/A"]))]))

(clerk/with-viewer custom-table-viewer
  {:col/a [1 2 3 4] :col/b [1 2 3] :col/c [1 2 3]})

(clerk/with-viewer custom-table-viewer
  {:col/a [1 2 3 4] :col/b [1 2 3] :col/c [1 2 3]})

;; #### 🐢 递归

;; 但这种呈现以及由此产生的节点转换并不总是您想要的.
;; 例如, `plotly` 或 `vl` 查看器希望接收未更改的子值, 以便将其用作规范.
;;
;; 要阻止 Clerk 的呈现向下遍历子节点, 请使用 `clerk/mark-presented` 作为 `:transform-fn`.
;; 比较下面 `[1 2 3]` 未更改的结果与您上面看到的结果.

^{::clerk/viewer v/inspect-wrapped-values}
(clerk/present (clerk/with-viewer {:transform-fn clerk/mark-presented
                                   :render-fn '(fn [x] [:pre (pr-str x)])}
                 [1 2 3]))

;; Clerk 的呈现还会将映射转换为序列, 以便对大型映射进行分页.
;; 当您处理已知有边界并希望保留其键的映射时, 可以使用 `clerk/mark-preserve-keys`.
;; 这仍然会转换(并分页)映射的值, 但会保留键不变.

^{::clerk/viewer v/inspect-wrapped-values ::clerk/auto-expand-results? true}
(clerk/present (clerk/with-viewer {:transform-fn clerk/mark-preserve-keys}
                 {:hello 42}))


;; #### 🔬 渲染

;; 如我们所见, 您也可以通过 `:transform-fn` 和在 JVM 上使用 `clerk/html` 来完成很多工作.
;; 当您想在浏览器中运行 Clerk 查看器渲染的代码时, 请使用 `:render-fn`.
;; 例如, 我们将为 Emmy 字面表达式编写一个多查看器, 它将计算两个替代表示并允许用户在浏览器中切换它们.

;; 我们从一个简单的函数开始, 它接受这样一个表达式并将其转换为一个包含两个表示的映射,
;; 一个是 TeX 格式, 另一个是原始形式.

(defn transform-literal [expr]
  {:TeX (-> expr emmy/->TeX clerk/tex)
   :original (clerk/code (with-out-str (emmy/print-expression (emmy/freeze expr))))})

;; 我们的 `literal-viewer` 调用 `transform-literal` 函数, 并调用 `clerk/mark-preserve-keys`.
;; 这告诉 Clerk 保持映射的键不变.

;; 在我们的 `:render-fn` 中, 它在浏览器中被调用, 我们将接收这个映射.
;; 注意这是一个引用形式, 而不是一个函数. Clerk 会将这个形式发送到浏览器进行评估.
;; 在那里, 它将创建一个 `reagent/atom` 来保存选择状态.
;; 最后, `nextjournal.clerk.render/inspect-presented` 是一个组件, 它接受一个经过 `clerk/present` 处理的 `wrapped-value` 并显示它.

(def literal-viewer
  {:pred emmy.expression/literal?
   :transform-fn (comp clerk/mark-preserve-keys
                       (clerk/update-val transform-literal))
   :render-fn '(fn [label->val]
                 (reagent.core/with-let [!selected-label (reagent.core/atom (ffirst label->val))]
                   [:<> (into
                         [:div.flex.items-center.font-sans.text-xs.mb-3
                          [:span.text-slate-500.mr-2 "查看为: "]]
                         (map (fn [label]
                                [:button.px-3.py-1.font-medium.hover:bg-indigo-50.rounded-full.hover:text-indigo-600.transition
                                 {:class (if (= @!selected-label label) "bg-indigo-100 text-indigo-600" "text-slate-500")
                                  :on-click #(reset! !selected-label label)}
                                 label]))
                         (keys label->val))
                    [nextjournal.clerk.render/inspect-presented (get label->val @!selected-label)]]))})

;; 现在让我们看看这是否有效. 尝试切换到原始表示!

^{::clerk/viewer literal-viewer}
(emmy/+ (emmy/square (emmy/sin 'x))
        (emmy/square (emmy/cos 'x)))

;; #### 📚 需要 CLJS

;; 将 `:render-fn` 作为引用形式内联写入, 当它们很小且独立时是可行的.
;; 对于更复杂的需求, Clerk 支持从类路径加载 ClojureScript 文件.

;; 要选择此功能, 请使用完全限定的符号作为 `:render-fn`, 并将 `:require-cljs` 设置为 `true`.
;; 这样, 您就告诉 Clerk 加载此 ClojureScript 文件(及其依赖项)到浏览器中的 Clerk SCI 环境中, 以便在那里使用.

(def literal-viewer-require-cljs
  (assoc literal-viewer
         :require-cljs true
         :render-fn 'emmy/render-literal))

;; 在常规 `.cljs` 文件中编写渲染函数通常更适合 IDE 工具(如 linter, REPL),
;; 并使重用现有 ClojureScript 代码更容易.

^{::clerk/viewer literal-viewer-require-cljs}
(emmy/+ (emmy/square (emmy/sin 'x))
        (emmy/square (emmy/cos 'x)))

;; #### 🥇 选择

;; 如果未指定查看器, Clerk 将遍历查看器序列, 并应用查看器中的 `:pred` 函数来查找匹配项.
;; 使用 `v/viewer-for` 为给定值选择一个查看器.
(def char?-viewer
  (v/viewer-for v/default-viewers \A))

;; 如果我们选择一个特定的查看器(此处使用 `clerk/html` 的 `v/html-viewer`), 这就是我们将获得的查看器.
(def html-viewer
  (v/viewer-for v/default-viewers (clerk/html [:h1 "foo"])))

;; 除了为每个值指定一个查看器之外, 我们还可以按命名空间修改查看器.
;; 在这里, 我们将上面的 `literal-viewer` 添加到整个命名空间.

^{::clerk/visibility {:result :hide}}
(clerk/add-viewers! [literal-viewer])

;; 如您所见, 我们现在会自动获得此查看器, 而无需显式选择它.
(emmy/+ (emmy/square (emmy/sin 'x))
        (emmy/square (emmy/cos 'x)))

;; #### 🔓 省略

(def string?-viewer
  (v/viewer-for v/default-viewers "Denn wir sind wie Baumstämme im Schnee."))

;; 请注意, 对于上面的 `string?` 查看器, `:page-size` 为 `80`. Clerk 中的所有集合查看器都是如此,
;; 它控制显示多少个元素. 因此, 使用上面的默认 `string?-viewer`, 我们显示前 80 个字符.
(def long-string
  (str/join (into [] cat (repeat 10 "Denn wir sind wie Baumstämme im Schnee.\n"))))

;; 如果我们更改查看器并在 `:page-size` 中设置不同的 `:n`, 我们只会看到 10 个字符.
(v/with-viewer (assoc string?-viewer :page-size 10)
  long-string)

;; 或者, 我们可以通过完全删除 `:page-size` 来关闭省略.
(v/with-viewer (dissoc string?-viewer :page-size)
  long-string)

;; 上面的操作是对单个查看器进行的更改. 但是我们还有一个函数 `update-viewers`,
;; 可以通过应用 `select-fn->update-fn` 映射来更新给定的查看器.
;; 在这里, 谓词是关键字 `:page-size`, 我们的更新函数为每个具有 `:page-size` 的查看器调用, 并将其删除.
(def without-pagination
  {:page-size #(dissoc % :page-size)})

;; 这是更新后的查看器:
(def viewers-without-lazy-loading
  (v/update-viewers v/default-viewers without-pagination))

;; 现在让我们确认这些修改后的查看器不再包含 `:page-size`.
(filter :page-size viewers-without-lazy-loading)

;; 并与默认值进行比较:
(filter :page-size v/default-viewers)

;; 现在让我们使用这些修改后的查看器显示上面的 `clojure-data` 变量.
(clerk/with-viewers viewers-without-lazy-loading
  clojure-data)

;; #### 👷 加载库

;; 这是一个自定义查看器, 用于 [Mermaid](https://mermaid-js.github.io/mermaid),
;; 一种类似 Markdown 的语法, 用于从文本创建图表.
;; 注意, 这个库不与 Clerk 捆绑, 但我们使用一个基于 [d3-require](https://github.com/d3/d3-require) 的组件来在运行时加载它.


(def mermaid-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require {:package ["mermaid@8.14/dist/mermaid.js"]}
                    (fn [mermaid]
                      [:div {:ref (fn [el] (when el
                                             (.render mermaid (str (gensym)) value #(set! (.-innerHTML el) %))))}])]))})

;; 然后我们可以使用 `with-viewer` 来使用上面的查看器.
(clerk/with-viewer mermaid-viewer
  "stateDiagram-v2
  [*] --> Still
  Still --> [*]

  Still --> Moving
  Moving --> Still
  Moving --> Crash
  Crash --> [*]")

;; #### 🧙 评估器

;; 默认情况下, [SCI](https://github.com/babashka/sci) 用于在浏览器中评估 `:render-fn` 函数.

;; 接下来是一个有意效率低下但有趣的方法, 用于计算第 n 个斐波那契数并显示其所需时间.

(def fib-viewer
  {:render-fn '(fn [n opts]
                 (reagent.core/with-let
                     [fib (fn fib [x]
                            (if (< x 2)
                              1
                              (+ (fib (dec x)) (fib (dec (dec x))))))
                      time-before (js/performance.now)
                      nth-fib (fib n)
                      time-after (js/performance.now)]
                   [:div
                    [:p
                     (if (= :cherry (-> opts :viewer :render-evaluator))
                       "Cherry"
                       "SCI")
                     " 用 " n " 计算了斐波那契数(" nth-fib ")"
                     " 耗时 " (js/Math.ceil (- time-after time-before) 2) "ms. "]]))})

(clerk/with-viewer fib-viewer 25)

;; 您可以通过查看器选项(参见 [自定义](#customizations))设置 `{::clerk/render-evaluator :cherry}` 来选择
;; [Cherry](https://github.com/squint-cljs/cherry) 作为替代评估器. Cherry 和 SCI
;; 对于查看器函数的主要区别在于性能. 对于性能敏感的代码, Cherry 更适合, 因为它直接编译为 JavaScript 代码.

(clerk/with-viewer fib-viewer {::clerk/render-evaluator :cherry} 25)

#_(clerk/halt!)
#_(clerk/serve! {:port 7777})

;; ## ⚙️ 自定义

;; Clerk 允许轻松自定义可见性, 结果宽度和预算.
;; 所有设置都可以使用 `ns` 元数据或顶级设置标记应用于整个文档, 以及使用元数据应用于每个表单.

;; 让我们从一个具体的例子开始, 了解它是如何工作的.

;; ### 🙈 可见性

;; 默认情况下, Clerk 将显示笔记本的所有代码和结果.

;; 您可以使用以下形状的映射来单独设置代码和结果的可见性:
;;```clojure
;;  {:nextjournal.clerk/visibility {:code :hide :result :show}}
;;```
;; 上面的示例将隐藏代码并仅显示结果.
;;
;; 有效值是 `:show`, `:hide` 和 `:fold`(仅适用于 `:code`).
;; 使用 `{:code :fold}` 将最初隐藏代码单元格, 但会显示一个指示器以切换其可见性:

^{::clerk/visibility {:code :fold}} (shuffle (range 25))

;; 可见性映射可以通过以下方式使用:

;; **通过 `ns` 表单设置文档默认值**
;;```clojure
;;  (ns visibility
;;   {:nextjournal.clerk/visibility {:code :fold}})
;;```
;; 上面的示例将默认隐藏所有代码单元格, 但会显示一个指示器以切换其可见性.
;; 在这种情况下, 结果将始终显示, 因为这是默认值.
;;
;; **作为元数据来控制单个顶级表单**
;;```clojure
;;  ^{::clerk/visibility {:code :hide}} (shuffle (range 25))
;;```
;; 这将隐藏代码但只显示结果:

^{::clerk/visibility {:code :hide}} (shuffle (range 25))

;; 将可见性设置为元数据将覆盖此特定表单的文档范围可见性设置.

;; **作为顶级表单来更改文档默认值**
;;
;; 无论通过 `ns` 表单设置了哪些默认值, 您都可以使用顶级映射作为标记来覆盖
;; 后面所有表单的可见性设置.
;;
;; 示例: 代码默认隐藏, 但您希望在某个特定点之后显示所有顶级表单的代码:
;;```clojure
;;  (ns visibility
;;   {:nextjournal.clerk/visibility {:code :hide}})
;;
;;  (+ 39 3) ;; 代码将被隐藏
;;  (range 25) ;; 代码将被隐藏
;;
;;  {:nextjournal.clerk/visibility {:code :show}}
;;
;;  (range 500) ;; 代码将可见
;;  (rand-int 42) ;; 代码将可见
;;```
;; 这对于调试也非常方便!
;;
;; ### 👻 Clerk 元数据
;;
;; 默认情况下, Clerk 会隐藏单元格上的 Clerk 元数据注释, 以避免分散对核心内容的注意力.
;; 当您希望读者了解元数据注释是如何编写的(如本书所示)时, 可以通过修改
;; `code-block-viewer` 来选择退出此行为:
;;
;;  (clerk/add-viewers! [(assoc v/code-block-viewer :transform-fn (v/update-val :text))])
;;
^{::clerk/visibility {:code :hide :result :hide}}
(v/reset-viewers! *ns* (v/add-viewers (v/get-viewers *ns*) [(assoc v/code-block-viewer :transform-fn (v/update-val :text))]))

;; ### 🍽 目录

;; 如果您想要像本文档中那样的目录, 请设置 `:nextjournal.clerk/toc` 选项.
;;
;;  (ns doc-with-table-of-contents
;;   {:nextjournal.clerk/toc true})
;;
;; 如果您希望它最初是折叠的, 请使用 `:collapsed` 作为值.

;; ### 🔮 结果展开

;; 如果您想更好地查看数据的形状, 而无需先单击和展开它, 请设置
;; `:nextjournal.clerk/auto-expand-results?` 选项.

^{::clerk/visibility {:code :fold}}
(def rows
  (take 15 (repeatedly (fn []
                         {:name (str
                                 (rand-nth ["奥斯卡" "凯伦" "弗拉德" "丽贝卡" "康拉德"]) " "
                                 (rand-nth ["米勒" "斯塔斯尼克" "罗宁" "迈耶" "布莱克"]))
                          :role (rand-nth [:管理员 :操作员 :经理 :程序员 :设计师])
                          :dice (shuffle (range 1 7))}))))


^{::clerk/auto-expand-results? true} rows

;; 此选项未来可能会成为默认值.


;; ### 🙅🏼‍♂️ 查看器预算

;; 为了避免向浏览器发送过多数据, Clerk 使用每个结果的预算来限制.
;; 您可以在上面看到此预算的作用. 使用 `:nextjournal.clerk/budget` 键可以更改其默认值 `200`,
;; 或者使用 `nil` 完全禁用它.

^{::clerk/budget nil ::clerk/auto-expand-results? true} rows


;; ## ⚛️ Clerk 同步

;; Clerk Sync 是一种支持 Clerk 在浏览器中运行的渲染显示与 JVM 之间轻量级交互的方式.
;; 通过用 `::clerk/sync` 元数据标记定义原子(atom)的表单, Clerk 将把这个原子同步到 Clerk 的渲染环境.
;; 当原子中的值改变时, 它也会重新计算笔记本.

^{::clerk/sync true}
(defonce !counter (atom 0))

(clerk/with-viewer {:render-fn '(fn [] [:button.bg-sky-500.hover:bg-sky-700.text-white.rounded-xl.px-2.py-1
                                        {:on-click #(swap! !counter inc)}
                                        "增加计数器"])}
  {})

;; ## 🚰 Tap 检查器

;; Clerk 附带了一个用于 Clojure tap 系统的检查器笔记本.
;; 使用 REPL 中的以下表单来显示它.

;;```clojure
;;(nextjournal.clerk/show! 'nextjournal.clerk.tap)
;;```

;; 然后您可以在代码库中的任何地方调用 `tap>`, Tap 检查器将显示您的值.
;; 这支持上面描述的完整查看器 API.

;;```clojure
;;(tap> (clerk/html [:h1 "你好 🚰 Tap 检查器 👋"]))
;;```

;; ## 👷‍♀️ 静态构建

;; Clerk 可以从笔记本集合构建静态 HTML.
;; 入口点是 `nextjournal.clerk/build!` 函数.
;; 您可以通过 `:paths` 选项(也支持 glob 模式)向其传递一组笔记本.

;; 当 Clerk 构建多个笔记本时, 它会自动生成一个索引页面, 该页面在打开构建时将首先显示.
;; 您可以通过 `:index` 选项覆盖此索引页面.

;; 另外值得注意的是, 有一个 `:compile-css` 选项, 它会编译一个 CSS 文件,
;; 其中只包含生成标记中使用的 CSS 类. (否则, Clerk 使用 Tailwind 的 Play CDN 脚本, 这可能会导致页面最初闪烁. )

;; 如果设置, `:ssr` 选项将使用 React 的服务器端渲染将生成的标记包含在构建 HTML 中.

;; 有关选项的完整列表, 请参阅 `nextjournal.clerk/build!` 中的文档字符串.

;; **以下是一些示例: **

;; ```clj
;; ;; 构建单个笔记本
;; (clerk/build! {:paths ["notebooks/rule_30.clj"]})
;;
;; ;; 使用自定义索引页面构建 `notebook/` 中的所有笔记本.
;; (clerk/build! {:paths ["notebooks/*"]
;;        :index "notebooks/welcome.clj"})
;; ```

;; ## ⚡️ Render nREPL

;; 为了交互式开发 `:render-fn`, Clerk 附带了一个 Render nREPL 服务器.
;; 要启用它, 请将 `:render-nrepl` 选项传递给 `serve!`.
;; 您可以通过传递不同的 `:port` 号来更改默认端口 `1339`.

;;  (nextjournal.clerk/serve! {:render-nrepl {}})

;; > nREPL 服务器已在端口 1339 上启动...

;; ⚠️ **编辑器连接提示**

;; Cider

;; 1. 运行 `M-x` `cider-connect-cljs`
;; 2. 选择 `localhost`
;; 3. 输入 `1339` 作为端口
;; 4. 选择 `nbb` repl 类型
;; 5. 打开一个 ClojureScript 缓冲区并运行 `M-x` `sesman-link-with-buffer` 选择新连接的 repl.


;; Calva

;;  1. 连接到正在运行的 REPL 服务器, 而不是项目中的服务器
;;  2. 选择 `nbb` 作为项目类型/连接序列
;;  3. 输入 `localhost:1339`(或自定义端口)

;; ## 🤖 Clerk 的工作原理

;; ### 🔖 解析

;; 首先, 我们使用 `rewrite-clj` 解析给定的 Clojure 文件.
(def parsed
  (parser/parse-file "./notebooks/book.clj"))

;; ### 🧐 分析

;; 然后, 每个表达式都使用 `tools.analyzer` 进行分析.
;; 记录依赖图, 分析后的表单和原始文件.

(def analyzed
  (ana/build-graph parsed))


;; 此分析是递归进行的, 会深入到所有依赖符号.

(ana/find-location 'nextjournal.clerk.analyzer/analyze-file)

(ana/find-location `dep/depend)

(ana/find-location 'io.methvin.watcher.DirectoryChangeEvent)

(ana/find-location 'java.util.UUID)

(let [{:keys [graph]} analyzed]
  (dep/transitive-dependencies graph 'nextjournal.clerk.book/analyzed))

;; ### 🪣 哈希

;; 然后我们可以使用这些信息来哈希每个表达式.
(def hashes
  (:->hash (ana/hash analyzed)))

;; ### 🗃 缓存评估

;; Clerk 使用哈希作为文件名, 并且只重新评估以前未见过的表单.
;; 缓存使用 [nippy](https://github.com/ptaoussanis/nippy).
(def rand-fifteen
  (do (Thread/sleep 10)
      (shuffle (range 15))))

;; 我们可以使用哈希映射中的变量名查找缓存键.
#_(when-let [form-hash (get hashes 'nextjournal.clerk.book/rand-fifteen)]
    (let [hash (slurp (eval/->cache-file (str "@" form-hash)))]
      (eval/thaw-from-cas hash)))

;; 作为一种逃逸机制, 您可以使用 `::clerk/no-cache` 标记表单或变量, 以始终重新评估它.
;; 以下表单将永远不会被缓存.
^::clerk/no-cache (shuffle (range 42))

;; 对于应该缓存的具有副作用的函数, 例如数据库查询, 您可以添加一个像 `#inst` 这样的值来控制何时进行评估.

(def query-results
  (let [_run-at #_(java.util.Date.) #inst "2021-05-20T08:28:29.445-00:00"
        ds (next.jdbc/get-datasource {:dbtype "sqlite" :dbname "chinook.db"})]
    (with-open [conn (next.jdbc/get-connection ds)]
      (clerk/table (next.jdbc/execute! conn ["SELECT AlbumId, Bytes, Name, TrackID, UnitPrice FROM tracks"])))))

(clerk/show! "./notebooks/book.clj")

(comment
  (clerk/present (clerk/html [:h1 "haha"]))

  (clerk/serve! {:browse true})
  )
