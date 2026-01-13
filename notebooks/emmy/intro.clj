^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.intro
  "Emmy 入门: 核心概念与基本用法."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]))

;; # Emmy 介绍
;;
;; Emmy 是一个 Clojure/ClojureScript 实现的计算机代数系统, 灵感来自于
;; MIT 的 scmutils 系统. 它专为数学和物理研究设计, 支持符号计算、
;; 自动微分、数值积分等功能.

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 演讲摘录: Emmy 的来路与动机

我在 10 年前参加过 Clojure Conj. 我在 13 年前认识 Clojure. 我在这次会议上学到一件事, 我花了很多年维护的项目, 其实没多少人听说过. 时间过去得很快. Hadoop 曾经是最火的东西, 现在也都消失了.

我在 2010 年开始用 Clojure, 之后花了几年维护非常函数式的编程工具, 比如 Cascalog. 我确实彻底喝了 Kool-Aid. Mark Engelberg 把我带进这个语言, 他发给我一个解谜游戏的求解器, 说你可能读不懂, 但这是你能找到的最美的语言.

我去尼加拉瓜背包旅行时带上了《The Little Schemer》, 用纸笔做完了. 回来后又啃了一阵 Clojure 的书, 直到突然有一天开窍, 然后就一路向前了. 之后我又被学术界吸走了一段时间, 也在一些不太好的地方待过, 大家都懂.

两年半前发生了变化. 我被拉回到 Clojure 的世界, 随手拿起了左边这本书《Structure and Interpretation of Classical Mechanics》, 作者是 Gerald Sussman 和 Jack Wisdom. 你们可能知道另一部书《SICP》. 这本书看起来像它的续集, 开头还有点调皮, 说传统物理教材用一种不精确的数学语言, 里面有很多省略和类型错误, 外行很难进入. 他们要做的是用 Scheme 把整个经典力学重写一遍, 把每个概念钉得很死, 让你能问 REPL, 一切就会清楚. 听起来很美好, 但实际上这书非常难, 物理学家觉得难, Lisp 程序员也觉得难. 但他们做到了, 从微积分、变分力学、摄动理论一路走到广义相对论, 右边那本是期待已久的续作.

我 2015 年在第 40 页就弃了. 但两年半前我决定一定要把它啃完. 也许是因为 SICP 里那句: 程序首先是写给人看的, 只是顺带给机器执行. 这让我意识到, 当我们写程序时, 其实是在写一种让别人可以审问的文档. 你必须把想法写得足够精确, 让别人能理解. 于是我决定用代码做笔记, 让自己走出那种学完数学和物理后只剩下茫然的状态. 我想把笔记分享出来, 让别人也能像我一样理解.

但这有张力, 因为最后得到的还是 Lisp 代码. 我把这些给家人看, 他们说我们很高兴你开心, 但我们并不开心. 这让我一直觉得不够.

差不多同一时期, 我发现了 MathBox. 这是一个叫 Stephen Wittens 的人写的库, 他对 WebGL 和图形编程很不满, 就做了一个不可思议的声明式图形库. 这些箭头每一个都是独立的小模拟, 可以拖动滑块, 画面会变化, 非常漂亮. 但我不知道它到底在做什么. 你要理解它就得读代码, 代码又是以性能优先的方式写的, 很难从代码回到数学.

我想到, 如果能把这个书里最基础的东西用 Clojure/ClojureScript 重写, 然后不仅输出数学, 还能接到这种可交互的图形里, 那不就对了吗. 我想让大家看懂这些笔记, 知道图里到底发生了什么. 我以为不会太久.

接下来我变得越来越投入, 甚至辞掉了工作, 全职投入这个项目. 两年半过去, 我写过一次说快完成了, 然后又过了两年半, 我又写了一次说下个月给更好的更新, 结果就是现在这样.

后来我们做出了 Emmy. 我开始寻找是否有人已经在做类似的事, 找到了 Colin Smith. 他做过一个 talk, 叫 Physics in Clojure, 把书里的代码全都跑通了. 我当时的天真想法是, 把所有文件改成 .cljc, 跑一遍 ClojureScript 编译就结束了. 结果那只是开始.

这些教材背后有一个 MIT Scheme 库, 是 Sussman 在 80 年代写的, 当时他在 Caltech 教物理, 和 Feynman 一起工作, 被拉进了一个项目要去求解控制宇宙演化的方程. 他把库叫 utils, 但里面有完整的广义相对论. 这库 6.5 万行, 结构朴素但力量巨大, 这事非常疯狂, 但它确实能在我的机器上跑起来.

库里有一个非常有趣的测试, 会检查每次提交是否还能生成爱因斯坦场方程. 这不是普通项目会有的测试.

Emmy 的核心思想之一是泛型分派. 我们用 protocols 和 multimethods, 把 plus, times, divide 这些数学操作扩展成可以作用于符号, 函数, 向量, 甚至更复杂的对象. Clojure 本身已经支持基于类型的分派, 这里只是把它系统化. 这样一来, 当你写 (+ 1 'x), 系统会返回一个符号表达式, 让符号计算沿着这条链往下继续.

这个思路的代价是你要把很多 Clojure core 的数学运算替换成泛型版本, 但一旦完成, 上层的表达式和函数都可以无缝参与. 这让你可以做一些疯狂的事, 比如把函数相加. 当你把符号传进去时, 会得到一个新的符号表达式. 你甚至可以把向量塞进去, 形成更复杂的表达式.

这很乱, 但系统有一套很强的简化算法, 能把复杂表达式化简成更干净的形式. 这也是能生成场方程的基础. 在单位圆上, 你最终会得到 1. 这是把数学结构和程序结构结合起来的好处.

进一步, 这也让我们开始考虑编译. 如果函数是由符号表达式构成, 我们就可以把它们改写成更快的目标代码, 甚至输出 JavaScript. 这是 Emmy 后续能做交互动画的关键.

然后是微积分. 物理系统的核心是状态, 有位置和速度. 速度更新位置, 力量更新速度. 所以你必须描述变化. 自动微分可以把这个过程变成程序上的可组合模块. 我们用一种带有 epsilon 的新数值类型, 让 epsilon 的平方为 0, 这样把 a + epsilon 带入函数就可以得到函数值和导数. 这让链式法则自然成立, 你只要定义基本算子的微分, 复杂函数就能自动求导.

这让我们可以在 Clerk 里探索, 看到符号表达式的可视化结果, 用 LaTeX 渲染, 再把这些东西转换成 JavaScript, 做出可交互的动画. 例如把泰勒展开变成一个可以拖动阶数的交互图, 观察函数逼近的过程. 这比静态图像更能帮助理解.

最后我们做了很多可交互的物理 demo, 比如双摆, 粒子在势阱中的运动, 在环面上的测地线. 这些 demo 不一定是最终答案, 但它们把数学从静态文本变成了可探索的系统. 你可以调整参数, 观察能量的变化, 看到系统如何演化. 这让学习从读书变成了动手玩.

Emmy 的最终目标, 是把这些传统教材变成可执行的在线书籍, 让所有图都变成可以互动的实验. 同时我们也计划让系统输出更多目标语言, 把 JVM 世界和其他生态连接起来.

这就是 Emmy 的来路: 用代码做笔记, 用程序表达数学, 用交互把物理带到眼前.
")

;; ## 核心概念

;; ### 1. 泛型数学操作 (Generic Mathematics)
;;
;; Emmy 实现了一套泛型数学操作系统, 可以统一处理:
;; - 数值 (整数, 有理数, 浮点数, 复数)
;; - 符号表达式
;; - 函数
;; - 矩阵和向量
;; - 微分形式

;; ### 2. 符号计算 (Symbolic Computation)
;;
;; Emmy 可以将符号作为抽象复数来处理, 对其进行代数运算会生成符号表达式.

;; ### 3. 自动微分 (Automatic Differentiation)
;;
;; Emmy 提供前向模式自动微分, 可以对数值和符号表达式求导.

;; ### 4. TeX 渲染
;;
;; Emmy 可以将符号表达式渲染为 TeX 和中缀表示法, 便于文档和可视化.

;; ## 快速开始

;; ### 基本算术运算

;; Emmy 的数值运算与 Clojure 类似, 但支持完整的数值塔:

^{::clerk/visibility {:code :show :result :show}}
(- (* 7 (/ 1 2)) 2)

;; 支持 vector 的运算
^{::clerk/visibility {:code :show :result :show}}
(* 7 [1 2 3 4 ])

;; 支持matrix
(* (matrix-by-rows [1 2 ] [3 4]) 200)

;; 支持matrix
(* (matrix-by-cols [ 1 2] [3 4]) 200)


;; 复数运算:
^{::clerk/visibility {:code :show :result :show}}
(asin -10)

;; ### 符号计算基础

;; 使用 quote (') 创建符号:
^{::clerk/visibility {:code :show :result :show}}
'x
'y

^{::clerk/visibility {:code :show :result :show}}
(+ 'x 'y)

^{::clerk/visibility {:code :show :result :show}}
(* 'x 'x)

^{::clerk/visibility {:code :show :result :show}}
(square 'x)

;; ### 表达式渲染

;; 使用 `->infix` 将表达式转换为中缀表示法:
^{::clerk/visibility {:code :show :result :show}}
(->infix (square (sin (+ 'a 3))))

;; 使用 `simplify` 简化表达式:
^{::clerk/visibility {:code :show :result :show}}
(simplify (+ (square (sin 'x)) (square (cos 'x))))

;; 组合使用:
^{::clerk/visibility {:code :show :result :show}}
(defn render [expr]
  (-> expr simplify ->infix))

(render (+ (square (sin 'x)) (square (cos 'x))))

;; ### 基本微分

;; `D` 是微分算子, 用于求导:
^{::clerk/visibility {:code :show :result :show}}
((D cube) 'x)

;; 简化后的结果:
^{::clerk/visibility {:code :show :result :show}}
(render ((D cube) 'x))

;; ## 数据类型概览

^{::clerk/visibility {:code :hide :result :show}}
(clerk/table
 {:head ["类型" "描述" "示例"]
  :rows [["符号 (Symbol)" "抽象数学变量" "'x, 'theta"]
         ["有理数 (Rational)" "精确分数" "1/2, 3/4"]
         ["复数 (Complex)" "实部+虚部" "(complex 1 2)"]
         ["向量 (Up/Down)" "协变/逆变向量" "(up 1 2 3)"]
         ["矩阵 (Matrix)" "二维数组" "(matrix-by-rows [[1 2] [3 4]])"]
         ["函数 (Function)" "可微分函数" "(fn [x] (* x x))"]
         ["算子 (Operator)" "作用于函数" "D, (expt D 2)"]]})

;; ## 向量与矩阵

;; Emmy 使用 `up` 和 `down` 来表示向量:

^{::clerk/visibility {:code :show :result :show}}
(def v (up 1 2 3))
v

^{::clerk/visibility {:code :show :result :show}}
(def w (down 4 5 6))
w

;; 向量运算:
^{::clerk/visibility {:code :show :result :show}}
(* v w)  ; 内积

;; 矩阵创建:
^{::clerk/visibility {:code :show :result :show}}
(def M (matrix-by-rows [[1 2] [3 4]]))
M

;; 矩阵与向量相乘:
^{::clerk/visibility {:code :show :result :show}}
(* M (up 1 0))

;; ## 下一步

;; 继续阅读:
;; - [symbolic.clj](symbolic.clj) - 深入符号计算
;; - [calculus.clj](calculus.clj) - 微积分与自动微分
;; - [mechanics.clj](mechanics.clj) - 拉格朗日与哈密顿力学
