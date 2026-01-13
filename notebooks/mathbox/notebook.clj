^#:nextjournal.clerk
{:toc true
 :no-cache true
 :visibility :hide-ns}
(ns mathbox.notebook
  (:require [mentat.clerk-utils.docs :as docs]
            [mentat.clerk-utils.show :refer [show-sci]]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :hide}}
(clerk/eval-cljs
 ;; 这些别名只在当前命名空间内生效.
 '(do (require '[mathbox.core :as mathbox])
      (require '[mathbox.primitives :as mb])
      (require '[reagent.core :as reagent])))

;; # MathBox.cljs

;; 面向 [Reagent](https://reagent-project.github.io/) 的接口, 用于使用
;; [MathBox](https://github.com/unconed/mathbox) 数学可视化库.
;;
;; [![Build Status](https://github.com/mentat-collective/mathbox.cljs/actions/workflows/kondo.yml/badge.svg?branch=main)](https://github.com/mentat-collective/mathbox.cljs/actions/workflows/kondo.yml)
;; [![License](https://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/mentat-collective/mathbox.cljs/blob/main/LICENSE)
;; [![cljdoc badge](https://cljdoc.org/badge/org.mentat/mathbox.cljs)](https://cljdoc.org/d/org.mentat/mathbox.cljs/CURRENT)
;; [![Clojars Project](https://img.shields.io/clojars/v/org.mentat/mathbox.cljs.svg)](https://clojars.org/org.mentat/mathbox.cljs)
;;
;; > 本页的交互式文档由 [此源文件](https://github.com/mentat-collective/mathbox.cljs/blob/$GIT_SHA/dev/mathbox/notebook.clj)
;; > 通过 [Clerk](https://github.com/nextjournal/clerk) 生成. 请参考
;; > [README 中的说明](https://github.com/mentat-collective/mathbox.cljs/tree/main#interactive-documentation-via-clerk),
;; > 在本地运行并修改本笔记本.
;; >
;; > 更多细节请查看 [Github 项目](https://github.com/mentat-collective/mathbox.cljs),
;; > 以及 [cljdoc 页面](https://cljdoc.org/d/org.mentat/mathbox.cljs/CURRENT/doc/readme) 的 API 文档.
;;
;; ## 什么是 MathBox?
;;
;; MathBox 是一个使用 WebGL 在浏览器中渲染演示级数学图形的库. 它构建在
;; [Three.js](https://threejs.org/), [Threestrap](https://github.com/unconed/threestrap)
;; 与 [ShaderGraph](https://github.com/unconed/shadergraph) 之上, 提供简洁的 API
;; 来可视化数学关系并以声明式方式驱动动画.

;; 在简单场景下, 它可以优雅地绘制 2D, 3D 或 4D 图像, 包括点, 向量, 标签,
;; 线框与着色曲面.

;; 在更复杂的用法里, 数据可以在 MathBox 内部处理, 编译为 GPU 程序并反馈到自身.
;; 结合着色器与渲染到纹理的效果, 你可以创建更复杂的视觉效果, 例如经典的
;; Winamp 风格音乐可视化.
;;
;; 例如, 下面是一个动态更新的曲面示例, 由环绕相机观察:

^{::clerk/width :wide
  ::clerk/visibility {:code :fold}}
(show-sci
 [mathbox.examples.test.face/Face])

;; > 本示例的代码位于
;; > [这里](https://github.com/mentat-collective/mathbox.cljs/tree/main/dev/mathbox/examples/test/face.cljc).
;; > 更多示例请访问 [示例索引](https://mathbox.mentat.org/dev/mathbox/examples/index.html).

;; [MathBox.cljs](https://github.com/mentat-collective/mathbox.cljs) 通过
;; [MathBox-react](https://github.com/ChristopherChudzicki/mathbox-react) 对 MathBox 做了扩展,
;; 提供 [Reagent](https://reagent-project.github.io/) 组件, 让你可以在 ClojureScript
;; UI 中轻松组织 MathBox 的构造.

;; ## 快速开始
;;
;; 参考 Clojars 页面说明, 将 `MathBox.cljs` 安装到你的 ClojureScript 项目中:

;; [![Clojars
;;    Project](https://img.shields.io/clojars/v/org.mentat/mathbox.cljs.svg)](https://clojars.org/org.mentat/mathbox.cljs)
;;
;; 或者通过 Git 依赖获取最新代码:

^{::clerk/visibility {:code :hide}}
(docs/git-dependency
 "mentat-collective/mathbox.cljs")

;; 在 ClojureScript 命名空间中引入 `mathbox.core` 和 `mathbox.primitives`:

;; ```clj
;; (ns my-app
;;   (:require [mathbox.core :as mathbox]
;;             [mathbox.primitives :as mb]
;;             [reagent.core :as reagent]))
;; ```
;;
;; 你还需要加载 `MathBox` 随附的样式表. 如果使用 Clerk 和
;; [`clerk-utils`](https://github.com/mentat-collective/clerk-utils), 可以在 `dev/user.clj`
;; 加入如下代码:

;; ```clj
;; (mentat.clerk-utils.css/set-css!
;;  "https://unpkg.com/mathbox/build/mathbox.css")
;; ```
;;
;; 否则请在你的项目中自行加载该 CSS 文件.
;;
;; ## 第一个场景

;; 你可以像写 Reagent 组件树那样声明一个 MathBox 组件树, 来创建 MathBox.cljs 场景.

;; 要在 MathBox 中呈现任何内容, 需要建立四件事:

;; 1) 一个正在观察场景的相机.
;; 2) 一个坐标系, 用来承载内容.
;; 3) 用于表示几何数据的载体.
;; 4) 用于绘制数据的形状选择.

;; 在这个示例里, 我们将构建一个 2D 的矩形视图, 包含一组点, 并将其绘制为连续曲线.

;; ### 从相机开始

;; 默认的 3D 相机起点是 `[0 0 0]`(即 X, Y, Z), 位于图形中心. +Z 指向屏幕外, -Z 指向屏幕内.

;; 使用 `mathbox/MathBox` 声明一个场景, 并为容器提供一些可选项.
;;
;; 插入 `mathbox.primitives/Camera`, 将 `:position` 后移 3 个单位到 `[0 0 3]`.
;; 我们还设置 `:proxy` 为 true, 允许交互式相机控制覆盖该位置.

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container
   {:style {:height "400px" :width "100%"}}}
  [mb/Camera
   {:position [0 0 3]
    :proxy true}]])

;; 现在我们得到一个空场景, 只显示加载条. MathBox 的 DOM 结构如下:

;; ```jsx
;; <root>
;;   <camera proxy={true} position={[0, 0, 3]} />
;; </root>
;; ```

;; > 如何在控制台生成此结构, 请参见后文的
;; > [打印 DOM](#printing-the-dom).

;; 如果你向组件传入一个单参数函数作为 `:ref`, 将得到指向 `<camera />` 的 MathBox selection.

;; ### 添加坐标系

;; 接下来我们建立一个简单的 2D 笛卡尔坐标系, 宽度是高度的两倍.

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}]])

;; `:range` 用向量的形式指定观察范围: `X` 方向是 `[-2 2]`, `Y` 方向是 `[-1 1]`.

;; `scale` 用于指定投影视图尺寸, 这里是 `[2 1]`, 即 2 个 `X` 单位与 1 个 `Y` 单位.

;; 为 `mb/Cartesian` 添加两个坐标轴与网格, 这样我们终于能看到一些内容:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3}]
   [mb/Axis {:axis 2 :width 3}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]]])

;; 你会看到默认 50% 灰色的网格线出现在设定宽度处. DOM 如下:

;; ```jsx
;; <root>
;;   <camera proxy={true} position={[0, 0, 3]} />
;;   <cartesian range={[[-2, 2], [-1, 1]]} scale={[2, 1]}>
;;     <axis axis={1} width={3} />
;;     <axis axis={2} width={3} />
;;     <grid width={2} divideX={20} divideY={10} />
;;   </cartesian>
;; </root>
;; ```

;; 可以通过传入 `:color "black"` 将坐标轴变为黑色:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]]])

;; 由于元素的屏幕尺寸取决于相机位置, 我们可以通过设置 `<root>` 的 `focus` 来校准单位,
;; 使其与相机距离匹配. 将 `<root>` 的参数传给 `mathbox/MathBox` 即可:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]]])

;; 对应结构如下:

;; ```jsx
;; <root focus={3}>
;;   <camera proxy={true} position={[0, 0, 3]} />
;;   <cartesian range={[[-2, 2], [-1, 1]]} scale={[2, 1]}>
;;     <axis axis={1} width={3} color="black" />
;;     <axis axis={2} width={3} color="black" />
;;     <grid width={2} divideX={20} divideY={10} />
;;   </cartesian>
;; </root>
;; ```

;; ### 添加数据并绘制

;; 现在绘制一条随时间移动的正弦波. 先创建一个 `mb/Interval`. 这是一个 1D 数组,
;; 在笛卡尔视图范围内采样, 并包含一个 `:expr` 用于生成数据点.

;; 我们[创建一个新的组件](https://github.com/reagent-project/reagent/blob/master/doc/CreatingReagentComponents.md),
;; 生成 64 个点, 每个点有两个 `:channels`, 即 `X` 与 `Y` 值.
;; 这个值决定每次 `emit` 需要输出多少项.

(show-sci
 (defn Data []
   [mb/Interval
    {:expr (fn [emit x _i t]
             (emit x (Math/sin (+ x t))))
     :width 64
     :channels 2}]))

;; 其中 `x` 是采样的 X 坐标, `_i` 是数组索引(0-63), `t` 是从 0 开始的秒级时间.
;; `emit` 的用法类似于 `return`, 但允许高效地输出多个值.

;; 有了数据之后, 我们可以[创建一个新组件](https://github.com/reagent-project/reagent/blob/master/doc/CreatingReagentComponents.md)
;; `Curve`, 在其中添加 `mb/Line` 进行绘制. 线条的目标默认是组件树中前一个节点.
;;
;; > 注意这里使用了 [React fragments](https://reactjs.org/docs/fragments.html),
;; > 通过以 `:<>` 开头的向量可以打包多个组件.

(show-sci
 (defn Curve []
   [:<>
    [Data]
    [mb/Line {:width 5
              :color "#3090FF"}]]))

;; 注意这里使用的是 HTML 十六进制颜色, CSS 形式如 `"rgb(255,128,53)"` 也可以.

;; 将 `Curve` 加入组件树:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]
   [Curve]]])

;; DOM 结构如下:

;; ```jsx
;; <root focus={3}>
;;   <camera proxy={true} position={[0, 0, 3]} />
;;   <cartesian range={[[-2, 2], [-1, 1]]} scale={[2, 1]}>
;;     <axis axis={1} width={3} color="black" />
;;     <axis axis={2} width={3} color="black" />
;;     <grid width={2} divideX={20} divideY={10} />
;;     <interval expr={(emit, x, i, t) => {
;;           emit(x, Math.sin(x + t));
;;         }} width={64} channels={2} />
;;     <line width={5} color="#3090FF" />
;;   </cartesian>
;; </root>
;; ```

;; ### 添加更多形状

;; 将数据与形状分离的好处是, 可以用多种方式绘制同一份数据. 例如添加 `mb/Point`,
;; 就能沿着数据区间绘制点:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]
   [Curve]
   [mb/Point {:size 8 :color "#3090FF"}]]])

;; 可用的形状在 [`mathbox.primitives.draw`
;; namespace](https://cljdoc.org/d/org.mentat/mathbox.cljs/CURRENT/api/mathbox.primitives.draw) 中有文档.
;; 点, 线与曲面都很直观. 例如, 用数据填充 2D `mb/Area`, 再传给 `mb/Surface`,
;; 就可以绘制实体三角网格.

;; 对于向量, 面片与条带, 规则会变化. 如果要绘制 64 个箭头向量, 需要 128 个点:
;; 每个向量包含起点与终点. 因此数据结构需要改变. 我们将 `items` 设为 2,
;; 每次迭代输出两个点, 并加上绿色的 `mb/Vector` 来绘制:

(show-sci
 (defn Vector []
   [:<>
    [mb/Interval
     {:expr (fn [emit x _i t]
              (emit x 0)
              (emit x (- (Math/sin (+ x t)))))
      :width 64
      :channels 2
      :items 2}]
    [mb/Vector
     {:end true
      :width 5
      :color "#50A000"}]]))

;; > 除了 `:expr`, 还可以提供 `:data` 数组(常量或变化数据, 扁平或嵌套).
;; > MathBox 会遍历该数组并自动 `emit`, 同时捕获实时数据.
;; > 如果数据不变化, 可以设置 `:live false` 进行优化.

;; 添加新的 `Vector` 组件后再次渲染场景:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]
   [Curve]
   [mb/Point {:size 8 :color "#3090FF"}]

   [Vector]]])

;; ### 添加浮动标签

;; 最后为坐标系添加标签. 首先需要建立 `mb/Scale`, 用于将视图划分为合适的刻度间隔.

;; ```clj
;; [mb/Scale {:divide 10}]
;; ```

;; 使用 `mb/Ticks` 绘制刻度:

;; ```clj
;; [mb/Ticks {:width 5 :size 15 :color "black"}]
;; ```

;; 接下来用 `mb/Format` 将数字格式化为栅格文字:

;; ```clj
;; [mb/Format {:digits 2 :weight "bold"}]
;; ```

;; 最后用 `mb/Label` 绘制浮动标签:

;; ```clj
;; [mb/Label {:color "red"
;;            :zIndex 1}]
;; ```

;; 将这些组件全部加入后, 场景如下:

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]
   [Curve]
   [mb/Point {:size 8 :color "#3090FF"}]

   [Vector]

   [mb/Scale {:divide 10}]
   [mb/Ticks {:width 5 :size 15 :color "black"}]
   [mb/Format {:digits 2 :weight "bold"}]
   [mb/Label {:color "red"
              :zIndex 1}]]])

;; 这里使用类似 CSS 的 `:zIndex`, 保证标签在 2D 层叠而不是被放到 3D 深度中.
;; 该值指定层级索引, 默认是 0, `1...n` 会逐层叠加. 不允许使用负的 `:zIndex`.

;; > 与 CSS 不同, 不建议使用过大的 `:zIndex`, 因为层级越高, 深度分辨率越差.

;; ### 让它动起来

;; 最后加入一个 `mb/Play` 模块, 增加一点动画.

^{::clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :focus 3}
  [mb/Camera {:position [0 0 3]
              :proxy true}]
  [mb/Cartesian
   {:range [[-2 2] [-1 1]]
    :scale [2 1]}

   [mb/Axis {:axis 1 :width 3 :color "black"}]
   [mb/Axis {:axis 2 :width 3 :color "black"}]
   [mb/Grid {:width 2 :divideX 20 :divideY 10}]
   [Curve]
   [mb/Point {:size 8 :color "#3090FF"}]

   [Vector]

   [mb/Scale {:divide 10}]
   [mb/Ticks {:width 5 :size 15 :color "black"}]
   [mb/Format {:digits 2 :weight "bold"}]
   [mb/Label {:color "red"
              :zIndex 1}]]
  [mb/Play
   {:target "cartesian"
    :pace 5
    :to 2
    :loop true
    :script
    [{:props {:range [[-2 2] [-1 1]]}}
     {:props {:range [[-4 4] [-2 2]]}}
     {:props {:range [[-2 2] [-1 1]]}}]}]])

;; 这里 `:script` 定义动画的关键帧. 我们声明要变更的 `:props`, 也就是 `:range`.
;; 关键帧以数组形式传入, 会均匀分配到 `(0, 1, 2)` 的关键帧时间点.

;; 将 `:pace` 设为每步 5 秒, 播放到关键帧时间 `2`, 然后启用 `:loop`.

;; ## 在 SCI 中使用 MathBox.cljs
;;
;; `MathBox.cljs` 兼容 [SCI, Small Clojure Interpreter](https://github.com/babashka/sci).
;;
;; 要将 `MathBox.cljs` 安装到 SCI 上下文中, 需要引入
;; [`mathbox.sci`](https://cljdoc.org/d/org.mentat/mathbox.cljs/CURRENT/api/mathbox.sci)
;; 并调用 `mathbox.sci/install!`:

;; ```clj
;; (ns myproject.sci-extensions
;;   (:require [mathbox.sci]))

;; (mathbox.sci/install!)
;; ```
;;
;; 如果希望更细粒度地控制, 可以查看 [`mathbox.sci` 的 cljdoc 页面](https://cljdoc.org/d/org.mentat/mathbox.cljs/CURRENT/api/mathbox.sci),
;; 其中列出了 SCI 配置与独立的命名空间对象, 可自行组合.
;;
;; > 注意 `MathBox.cljs` 不自带 SCI 依赖, 需要自行引入.
;;
;; ## 在 Clerk 中使用 MathBox.cljs
;;
;; 将 `MathBox.cljs` 与 Nextjournal 的 [Clerk](https://clerk.vision/) 配合使用,
;; 可以编写像本文档一样内嵌 MathBox 场景的笔记本.
;;
;; 这要求为 Clerk 项目生成自定义的 ClojureScript 构建. 对已有项目而言, 最简单的方式是使用
;; [`clerk-utils` 项目](https://clerk-utils.mentat.org/). 请参考
;; [`clerk-utils` 的自定义 ClojureScript 指南](https://clerk-utils.mentat.org/#custom-clojurescript-builds).
;;
;; 如果你是第一次使用 Clerk, 可以使用下文介绍的 [`mathbox/clerk` 模板](#project-template),
;; 生成一个已完成 ["在 SCI 中使用 MathBox.cljs"](#mathbox.cljs-via-sci) 所有步骤的新项目.

;; ## 项目模板
;;
;; `MathBox.cljs` 提供了一个 [`deps-new`](https://github.com/seancorfield/deps-new) 模板,
;; 名称为 [`mathbox/clerk`](https://github.com/mentat-collective/mathbox.cljs/tree/main/resources/mathbox/clerk),
;; 方便快速创建已经配置好 ["在 SCI 中使用 MathBox.cljs"](#mathbox.cljs-via-sci) 的 Clerk 项目.

;; 首先安装 [`deps-new`](https://github.com/seancorfield/deps-new) 工具:

;; ```sh
;; clojure -Ttools install io.github.seancorfield/deps-new '{:git/tag "v0.5.0"}' :as new
;; ```

;; 要基于 [`mathbox/clerk`](https://github.com/mentat-collective/mathbox.cljs/tree/main/resources/mathbox/clerk)
;; 创建名为 `my-notebook-project` 的新项目, 运行如下命令:

^{::clerk/visibility {:code :hide}}
(clerk/md
 (format "
```sh
clojure -Sdeps '{:deps {io.github.mentat-collective/mathbox.cljs {:git/sha \"%s\"}}}' \\
-Tnew create \\
:template mathbox/clerk \\
:name myusername/my-notebook-project
```" (docs/git-sha)))

;; 生成项目的 `README.md` 中包含如何在新项目中开发的说明.

;; 如果你已有 Clerk 项目并准备加入 `MathBox.cljs`, 也可以参考
;; [`mathbox/clerk`](https://github.com/mentat-collective/mathbox.cljs/tree/main/resources/mathbox/clerk),
;; 获取项目结构方面的思路.

;; ## 指南
;;
;; 以下指南仍在整理中. 每个章节要么已经完成, 要么只是记录了需要补充的内容.

;; ### 配置 MathBox

;; TODO: 说明 MathBox 组件的配置方式等.

;; 什么是 threestrap, 它们之间如何关联?
;;
;; #### Threestrap 配置

;; ### 支持的原语
;;
;; 链接到示例目录与 cljdoc 文档.

;; ### 响应式更新

;; ### 示例目录
;;
;; ### 哪些组件可以嵌套?

;; - "view",
;; - "cartesian",
;; - "cartesian4",
;; - "polar",
;; - "spherical",
;; - "stereographic",
;; - "stereographic4",
;; - "transform",
;; - "transform4",
;; - "vertex",
;; - "fragment",
;; - "layer",
;; - "mask",
;; - "group",
;; - "inherit",
;; - "root",
;; - "unit",
;; - "rtt",
;; - "clock",
;; - "now",
;; - "move",
;; - "present",
;; - "reveal",
;; - "slide",

;; ### Options 与 Binds 的区别
;;
;; ### 使用 Fragments 组合组件
;;
;; ### 打印 DOM
;;
;; 目前不易实现, 因为缺少明显的挂钩位置.

;; ### 控制类型

;; TODO: 介绍 OrbitControls 等内容.

;; ```clj
;; ["three/examples/jsm/controls/OrbitControls.js" :as OrbitControls]
;; ```

;; ## 术语表

;; ### DOM

;; * DOM - 文档对象模型. 一般指页面上的 HTML. 在 MathBox 中, 指由节点及其层级构成的虚拟 DOM.
;; * Node - 一个原语的实例, 插入到 MathBox DOM 中.
;; * Primitive - MathBox 的基础构件之一.
;; * Prop 或 Property - 设置在节点上的单个值, 合称 *props*.
;; * Selection - DOM 的一个子集. 既可以是整个 DOM, 也可以是匹配选择器的节点集合. 选择器通常类似 CSS,
;;   例如原语名(`"camera"`), id(`"#colors"`), 或 class(`".points"`).

;; ### 图形

;; * RTT - Render To Texture. 将渲染结果写入纹理, 以便进一步处理.
;; * Shader - 用 GLSL 编写并在 GPU 上运行的程序, 语法类似 C++.
;; * [ShaderGraph](https://github.com/unconed/shadergraph) - MathBox 依赖之一, 将小型 GLSL 片段动态编译为单一着色器.
;; * [Three.js](http://threejs.org/) - 流行的 WebGL 库, MathBox 用于相机与控制器.
;; * [Threestrap](https://github.com/unconed/threestrap) - 用于配置 Three.js 的引导工具.
;; * WebGL - 用于渲染 3D 场景的 JavaScript API, 被 MathBox 使用.

;; ### 数据

;; * `expr` - 数据原语上的参数, 期望一个函数, 其参数包括:
;; * `emit` - 另一个函数, 调用时其参数会成为数据.
;; * `x, y, z` - 当前点的位置坐标, 最多三个数. Interval(1D), Area(2D), Volume(3D) 会在当前视图范围内均匀采样.
;;   如果不需要, 可以使用 Array, Matrix 或 Voxel, 它们不包含这些参数.
;; * `i, j, k` - 当前点的一到三个索引.
;; * `t` - 自程序启动以来的秒数.
;; * `d` - 距离上一帧的时间增量(秒).
;; * Width - 数据在 *x* 方向的大小, 即行数.
;; * Height - 数据在 *y* 方向的大小, 即列数.
;; * Depth - 数据在 *z* 方向的大小, 即层数.
;; * Items - 数据在 *w* 方向的大小, 即每个空间位置的数据点数量, 也就是 `expr` 中 `emit` 被调用的次数.
;; * Channels - 与每个数据点关联的数值数量, 即传给 `emit` 的参数个数. 它不是数组维度, 而是单个元素的长度.
;; * History - 在未使用的维度中保存过去的 1D 或 2D 数据.
;; * Swizzling - 通过索引列表对向量元素进行抽取, 重排或复制的过程. 例如 `"yxz"` 会交换 x 与 y.
;;   `swizzle` 作用于数组元素, `transpose` 作用于数组维度.

;; ## 谁在使用 MathBox / MathBox.cljs?

;; 以下站点使用了 MathBox 或 MathBox-react:

;; - [Math3D online graphing calculator](https://www.math3d.org/)
;; - [KineticGraphs JS Engine](https://kineticgraphs.org/) ([code](https://github.com/cmakler/kgjs))
;; - [Textbook: "Interactive Linear Algebra"](https://textbooks.math.gatech.edu/ila/) ([code](https://github.com/QBobWatson/ila))
;; - Many visualizations at [Sam Zhang's homepage](https://sam.zhang.fyi/#projects)
;; - Jesse Bettencourt's [Torus Knot Fibration](http://jessebett.com/TorusKnotFibration/) Master's project ([code](https://github.com/jessebett/TorusKnotFibration))
;; - [Interactive knot visualizations](https://rockey-math.github.io/mathbox/graph3d-curve)

;; 以及 [MathBox Google 讨论组](https://groups.google.com/forum/#!forum/mathbox) 中
;; [这个线程](https://groups.google.com/g/mathbox/c/Uvyb5fHaLq4) 列出的诸多演示.

;; ## 致谢与支持

;; 如果你想支持此工作与我的其他开源项目, 可以通过 [GitHub Sponsors](https://github.com/sponsors/sritchie) 赞助.
;; 感谢所有赞助者!

;; 我也感谢 [Clojurists Together](https://www.clojuriststogether.org/) 在本库开发期间提供的资助.
;; 欢迎[成为成员](https://www.clojuriststogether.org/developers/), 支持类似的开源项目.
;;
;; 关于我和我的工作, 请访问 https://samritchie.io.

;; ## 许可证

;; Copyright © 2022-2023 Sam Ritchie.

;; 本项目以 [MIT License](https://github.com/mentat-collective/mathbox.cljs/blob/main/LICENSE) 发布.
;; 详见 [LICENSE](https://github.com/mentat-collective/mathbox.cljs/blob/main/LICENSE).
