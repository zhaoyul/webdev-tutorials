^{:nextjournal.clerk/visibility {:code :hide}}
(ns emmy.talk-recap
  "Emmy 演讲复盘: 公式、符号与可交互示例."
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]
            [emmy.common :as common]))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
# 演讲复盘: 可读的数学与可玩的物理

这一页按演讲思路复刻核心示例: 符号计算与泛型数学, 自动微分与泰勒展开,
以及用 MathBox 呈现复杂数学交互场景。
")

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
Emmy 把数学运算变成可泛化的系统. 符号与函数都能参与同样的运算,
从而把表达式保留下来, 让代码成为可审问的文档。
")

^{::clerk/visibility {:code :show :result :show}}
(def x 'x)

^{::clerk/visibility {:code :show :result :show}}
(def y 'y)

^{::clerk/visibility {:code :show :result :show}}
(+ x y 1)

^{::clerk/visibility {:code :show :result :show}}
(def f (literal-function 'f))

^{::clerk/visibility {:code :show :result :show}}
(def g (literal-function 'g))

^{::clerk/visibility {:code :show :result :show}}
((+ f g) 'x)

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
把数学结构写成程序后, 代数简化就能直接用程序表达。
")

^{::clerk/visibility {:code :show :result :show}}
(def theta 'theta)

^{::clerk/visibility {:code :show :result :show}}
(magnitude (up (cos theta) (sin theta)))

^{::clerk/visibility {:code :show :result :show}}
(common/render (simplify (+ (square (sin theta)) (square (cos theta)))))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
演讲中的 ε 类型思想, 在 Emmy 里对应为 D 算子:
给定函数, D 返回它的导数函数。
")

^{::clerk/visibility {:code :show :result :show}}
((D sin) 'x)

^{::clerk/visibility {:code :show :result :show}}
(common/render ((D sin) 'x))

^{::clerk/visibility {:code :show :result :show}}
(defn h [x]
  (sin (square x)))

^{::clerk/visibility {:code :show :result :show}}
(common/render ((D h) 'x))

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
泰勒级数是把复杂函数还原成可计算多项式的一条路径。
下面的 MathBox 示例用滑块演示逼近过程。
")

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-taylor-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [title]}]
      (reagent.core/with-let
          [!order (reagent.core/atom 6)
           !func (reagent.core/atom "sin")
           !mathbox (reagent.core/atom nil)
           width 256
           mount! (fn [el]
                    (when (and el (nil? @!mathbox))
                      (let [mathbox-fn (.-mathBox js/window)]
                        (when mathbox-fn
                          (let [controls (.-OrbitControls js/THREE)
                                options (cond-> {:element el
                                                 :plugins ["core" "controls" "cursor"]}
                                          controls (assoc :controls {:klass controls}))
                                mathbox (mathbox-fn (clj->js options))
                                three (.-three mathbox)
                                renderer (.-renderer three)]
                            (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                            (when-let [camera (.-camera three)]
                              (.set (.-position camera) 0 0 4)
                              (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                            (let [view (.cartesian mathbox
                                                   (clj->js {:range [[-3 3] [-2 8] [-1 1]]
                                                             :scale [1 1 1]}))
                                  _ (.axis view (clj->js {:axis 1 :color 0x94a3b8}))
                                  _ (.axis view (clj->js {:axis 2 :color 0x94a3b8}))
                                  _ (.grid view (clj->js {:divideX 6 :divideY 6 :color 0x1e293b}))
                                  series (fn [kind x n]
                                           (case kind
                                             "exp"
                                             (loop [k 0 term 1 sum 1]
                                               (if (>= k n)
                                                 sum
                                                 (let [term (* term (/ x (inc k)))]
                                                   (recur (inc k) term (+ sum term)))))
                                             "log"
                                             (loop [k 1 term x sum x]
                                               (if (>= k n)
                                                 sum
                                                 (let [term (* term (- x) (/ k (inc k)))]
                                                   (recur (inc k) term (+ sum term)))))
                                             (loop [k 0 term x sum x]
                                               (if (>= k n)
                                                 sum
                                                 (let [term (* term (/ (- (* x x)) (* (+ (* 2 k) 2) (+ (* 2 k) 3))))]
                                                   (recur (inc k) term (+ sum term)))))))
                                  domain (fn [kind]
                                           (case kind
                                             "log" [-0.9 1.2]
                                             "exp" [-2.5 2.5]
                                             [-3.14 3.14]))
                                  base (.interval view
                                                  (clj->js {:width width
                                                            :expr (fn [emit _ i _t]
                                                                    (let [kind @!func
                                                                          n (js/Math.round @!order)
                                                                          bounds (domain kind)
                                                                          x-min (nth bounds 0)
                                                                          x-max (nth bounds 1)
                                                                          u (/ i (dec width))
                                                                          x (+ x-min (* u (- x-max x-min)))
                                                                          y (case kind
                                                                              "exp" (js/Math.exp x)
                                                                              "log" (js/Math.log (+ 1 x))
                                                                              (js/Math.sin x))]
                                                                      (emit x y 0)))}))
                                  approx (.interval view
                                                    (clj->js {:width width
                                                              :expr (fn [emit _ i _t]
                                                                      (let [kind @!func
                                                                            n (js/Math.round @!order)
                                                                            bounds (domain kind)
                                                                            x-min (nth bounds 0)
                                                                            x-max (nth bounds 1)
                                                                            u (/ i (dec width))
                                                                            x (+ x-min (* u (- x-max x-min)))
                                                                            y (series kind x n)]
                                                                        (emit x y 0)))}))]
                              (.line base (clj->js {:width 3 :color 0x94a3b8}))
                              (.line approx (clj->js {:width 4 :color 0x38bdf8})))
                            (reset! !mathbox mathbox))))))]
        [:div {:class "space-y-4"}
         [:div {:class "flex flex-wrap items-center justify-between gap-4"}
          [:div {:class "text-lg font-semibold text-slate-800"}
           (or title "MathBox: 泰勒级数逼近")]
          [:div {:class "flex flex-wrap items-center gap-3 text-sm text-slate-600"}
           [:label {:class "flex items-center gap-2"}
            [:span "函数"]
            [:select {:value @!func
                      :class "rounded-md border border-slate-200 bg-white px-2 py-1"
                      :on-change (fn [e]
                                   (reset! !func (.. e -target -value)))}
             [:option {:value "sin"} "sin(x)"]
             [:option {:value "exp"} "exp(x)"]
             [:option {:value "log"} "log(1+x)"]]]
           [:label {:class "flex items-center gap-2"}
            [:span "阶数"]
            [:input {:type "range"
                     :min 1
                     :max 12
                     :step 1
                     :value @!order
                     :on-change (fn [e]
                                  (reset! !order (js/parseFloat (.. e -target -value))))}]
            [:span (str (js/Math.round @!order))]]]]
         [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
          (fn [_]
            [:div {:style {:width "100%"
                           :height "420px"
                           :border "1px solid #e2e8f0"
                           :border-radius "16px"
                           :overflow "hidden"
                           :background "#0f172a"}
                   :ref mount!}])]]
        (finally
          (when-let [mathbox @!mathbox]
            (when (.-destroy mathbox)
              (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-taylor-viewer {:title "MathBox: 泰勒级数逼近"})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
双摆示例来自演讲中的混沌演示. 两个系统只有极小的初始差异,
但轨迹会迅速分离。
")

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-double-pendulum-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [title]}]
      (reagent.core/with-let
          [!mathbox (reagent.core/atom nil)
           !state-a (reagent.core/atom {:theta1 1.0
                                        :theta2 1.3
                                        :omega1 0.0
                                        :omega2 0.0
                                        :last nil
                                        :p1 [0 0]
                                        :p2 [0 0]})
           !state-b (reagent.core/atom {:theta1 1.0
                                        :theta2 1.31
                                        :omega1 0.0
                                        :omega2 0.0
                                        :last nil
                                        :p1 [0 0]
                                        :p2 [0 0]})
           mount! (fn [el]
                    (when (and el (nil? @!mathbox))
                      (let [mathbox-fn (.-mathBox js/window)]
                        (when mathbox-fn
                          (let [controls (.-OrbitControls js/THREE)
                                options (cond-> {:element el
                                                 :plugins ["core" "controls" "cursor"]}
                                          controls (assoc :controls {:klass controls}))
                                mathbox (mathbox-fn (clj->js options))
                                three (.-three mathbox)
                                renderer (.-renderer three)
                                g 9.81
                                m1 1.0
                                m2 1.0
                                l1 1.0
                                l2 1.0
                                step (fn [state t]
                                       (let [last (:last state)]
                                         (if (nil? last)
                                           (let [theta1 (:theta1 state)
                                                 theta2 (:theta2 state)
                                                 x1 (* l1 (js/Math.sin theta1))
                                                 y1 (* -1 l1 (js/Math.cos theta1))
                                                 x2 (+ x1 (* l2 (js/Math.sin theta2)))
                                                 y2 (+ y1 (* -1 l2 (js/Math.cos theta2)))]
                                             (assoc state :last t :p1 [x1 y1] :p2 [x2 y2]))
                                           (let [dt (min 0.02 (max 0 (- t last)))
                                                 theta1 (:theta1 state)
                                                 theta2 (:theta2 state)
                                                 omega1 (:omega1 state)
                                                 omega2 (:omega2 state)
                                                 delta (- theta1 theta2)
                                                 den (- (+ (* 2 m1) m2) (* m2 (js/Math.cos (* 2 delta))))
                                                 domega1 (/ (- (- (* g (+ (* 2 m1) m2) (js/Math.sin theta1))
                                                                  (* m2 g (js/Math.sin (- theta1 (* 2 theta2)))))
                                                               (* 2 (js/Math.sin delta)
                                                                  m2
                                                                  (+ (* omega2 omega2 l2)
                                                                     (* omega1 omega1 l1 (js/Math.cos delta)))))
                                                            (* l1 den))
                                                 domega2 (/ (* 2 (js/Math.sin delta)
                                                               (+ (* omega1 omega1 l1 (+ m1 m2))
                                                                  (* g (+ m1 m2) (js/Math.cos theta1))
                                                                  (* omega2 omega2 l2 m2 (js/Math.cos delta))))
                                                            (* l2 den))
                                                 omega1' (+ omega1 (* domega1 dt))
                                                 omega2' (+ omega2 (* domega2 dt))
                                                 theta1' (+ theta1 (* omega1' dt))
                                                 theta2' (+ theta2 (* omega2' dt))
                                                 x1 (* l1 (js/Math.sin theta1'))
                                                 y1 (* -1 l1 (js/Math.cos theta1'))
                                                 x2 (+ x1 (* l2 (js/Math.sin theta2')))
                                                 y2 (+ y1 (* -1 l2 (js/Math.cos theta2')))]
                                             (assoc state
                                                    :theta1 theta1'
                                                    :theta2 theta2'
                                                    :omega1 omega1'
                                                    :omega2 omega2'
                                                    :last t
                                                    :p1 [x1 y1]
                                                    :p2 [x2 y2])))))]
                            (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                            (when-let [camera (.-camera three)]
                              (.set (.-position camera) 0 0 4)
                              (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                            (let [view (.cartesian mathbox
                                                   (clj->js {:range [[-2 2] [-2 1] [-1 1]]
                                                             :scale [1 1 1]}))
                                  _ (.axis view (clj->js {:axis 1 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 2 :color 0x64748b}))
                                  _ (.grid view (clj->js {:divideX 6 :divideY 6 :color 0x1e293b}))
                                  source (fn [state color]
                                           (let [data (.interval view
                                                                 (clj->js {:width 3
                                                                           :live true
                                                                           :expr (fn [emit _ i t]
                                                                                   (when (= i 0)
                                                                                     (swap! state step t))
                                                                                   (let [{:keys [p1 p2]} @state
                                                                                         x1 (nth p1 0)
                                                                                         y1 (nth p1 1)
                                                                                         x2 (nth p2 0)
                                                                                         y2 (nth p2 1)]
                                                                                     (case i
                                                                                       0 (emit 0 0 0)
                                                                                       1 (emit x1 y1 0)
                                                                                       2 (emit x2 y2 0))))}))]
                                             (.line data (clj->js {:width 4 :color color}))
                                             data))]
                              (source !state-a 0x38bdf8)
                              (source !state-b 0xf97316))
                            (reset! !mathbox mathbox))))))]
        [:div {:class "space-y-4"}
         [:div {:class "text-lg font-semibold text-slate-800"}
          (or title "MathBox: 双摆混沌")]
         [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
          (fn [_]
            [:div {:style {:width "100%"
                           :height "420px"
                           :border "1px solid #e2e8f0"
                           :border-radius "16px"
                           :overflow "hidden"
                           :background "#0f172a"}
                   :ref mount!}])]]
        (finally
          (when-let [mathbox @!mathbox]
            (when (.-destroy mathbox)
              (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-double-pendulum-viewer {:title "MathBox: 双摆混沌"})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
这一段复刻演讲里的扭结示例. 使用 p, q 控制环面结的绕行次数。
")

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-knot-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [title]}]
      (reagent.core/with-let
          [!p (reagent.core/atom 2)
           !q (reagent.core/atom 3)
           !mathbox (reagent.core/atom nil)
           width 512
           mount! (fn [el]
                    (when (and el (nil? @!mathbox))
                      (let [mathbox-fn (.-mathBox js/window)]
                        (when mathbox-fn
                          (let [controls (.-OrbitControls js/THREE)
                                options (cond-> {:element el
                                                 :plugins ["core" "controls" "cursor"]}
                                          controls (assoc :controls {:klass controls}))
                                mathbox (mathbox-fn (clj->js options))
                                three (.-three mathbox)
                                renderer (.-renderer three)]
                            (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                            (when-let [camera (.-camera three)]
                              (.set (.-position camera) 0 0 5)
                              (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                            (let [view (.cartesian mathbox
                                                   (clj->js {:range [[-3 3] [-3 3] [-2 2]]
                                                             :scale [1 1 1]}))
                                  _ (.axis view (clj->js {:axis 1 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 2 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 3 :color 0x64748b}))
                                  curve (.interval view
                                                   (clj->js {:width width
                                                             :expr (fn [emit _ i _t]
                                                                     (let [p (js/Math.round @!p)
                                                                           q (js/Math.round @!q)
                                                                           u (* (/ i (dec width)) (* 2 js/Math.PI))
                                                                           r 0.6
                                                                           R 2.0
                                                                           cosq (js/Math.cos (* q u))
                                                                           sinq (js/Math.sin (* q u))
                                                                           cosp (js/Math.cos (* p u))
                                                                           sinp (js/Math.sin (* p u))
                                                                           x (* (+ R (* r cosq)) cosp)
                                                                           y (* (+ R (* r cosq)) sinp)
                                                                           z (* r sinq)]
                                                                       (emit x y z)))}))]
                              (.line curve (clj->js {:width 4 :color 0x22c55e})))
                            (reset! !mathbox mathbox))))))]
        [:div {:class "space-y-4"}
         [:div {:class "flex flex-wrap items-center justify-between gap-4"}
          [:div {:class "text-lg font-semibold text-slate-800"}
           (or title "MathBox: 环面扭结")]
          [:div {:class "flex items-center gap-3 text-sm text-slate-600"}
           [:label {:class "flex items-center gap-2"}
            [:span "p"]
            [:input {:type "range"
                     :min 1
                     :max 6
                     :step 1
                     :value @!p
                     :on-change (fn [e]
                                  (reset! !p (js/parseFloat (.. e -target -value))))}]
            [:span (str (js/Math.round @!p))]]
           [:label {:class "flex items-center gap-2"}
            [:span "q"]
            [:input {:type "range"
                     :min 1
                     :max 6
                     :step 1
                     :value @!q
                     :on-change (fn [e]
                                  (reset! !q (js/parseFloat (.. e -target -value))))}]
            [:span (str (js/Math.round @!q))]]]]
         [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
          (fn [_]
            [:div {:style {:width "100%"
                           :height "420px"
                           :border "1px solid #e2e8f0"
                           :border-radius "16px"
                           :overflow "hidden"
                           :background "#0f172a"}
                   :ref mount!}])]]
        (finally
          (when-let [mathbox @!mathbox]
            (when (.-destroy mathbox)
              (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-knot-viewer {:title "MathBox: 环面扭结"})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## MathBox: 势阱与能量交换

这一段复刻演讲里的势阱示例. 用简单的四次势能描述一个粒子,
通过滑块注入能量, 观察粒子在势阱内往复运动。
")

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-well-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [title]}]
      (reagent.core/with-let
          [!k (reagent.core/atom 1.0)
           !alpha (reagent.core/atom 0.2)
           !v0 (reagent.core/atom 0.0)
           !mathbox (reagent.core/atom nil)
           !state (reagent.core/atom {:x 1.2 :v 0.0 :last nil})
           !energy (reagent.core/atom {:ke 0.0 :pe 0.0 :total 0.0})
           width 256
           restart! (fn []
                      (let [x0 1.2
                            v0 @!v0
                            k @!k
                            alpha @!alpha
                            pe (+ (* 0.5 k x0 x0)
                                  (* 0.25 alpha x0 x0 x0 x0))
                            ke (* 0.5 v0 v0)]
                        (reset! !state {:x x0 :v v0 :last nil})
                        (reset! !energy {:ke ke :pe pe :total (+ ke pe)})))
           update! (fn [atom value]
                     (reset! atom value)
                     (restart!))
           mount! (fn [el]
                    (when (and el (nil? @!mathbox))
                      (let [mathbox-fn (.-mathBox js/window)]
                        (when mathbox-fn
                          (let [controls (.-OrbitControls js/THREE)
                                options (cond-> {:element el
                                                 :plugins ["core" "controls" "cursor"]}
                                          controls (assoc :controls {:klass controls}))
                                mathbox (mathbox-fn (clj->js options))
                                three (.-three mathbox)
                                renderer (.-renderer three)
                                potential (fn [x]
                                            (let [k @!k
                                                  alpha @!alpha]
                                              (+ (* 0.5 k x x)
                                                 (* 0.25 alpha x x x x))))
                                step (fn [state t]
                                       (let [last (:last state)]
                                         (if (nil? last)
                                           (assoc state :last t)
                                           (let [dt (min 0.02 (max 0 (- t last)))
                                                 k @!k
                                                 alpha @!alpha
                                                 x (:x state)
                                                 v (:v state)
                                                 acc (- (+ (* k x) (* alpha x x x)))
                                                 v' (+ v (* acc dt))
                                                 x' (+ x (* v' dt))
                                                 pe (potential x')
                                                 ke (* 0.5 v' v')]
                                             (reset! !energy {:ke ke :pe pe :total (+ ke pe)})
                                             (assoc state :x x' :v v' :last t)))))]
                            (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                            (when-let [camera (.-camera three)]
                              (.set (.-position camera) 0 0 4)
                              (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                            (let [view (.cartesian mathbox
                                                   (clj->js {:range [[-2.5 2.5] [0 5] [-1 1]]
                                                             :scale [1 1 1]}))
                                  _ (.axis view (clj->js {:axis 1 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 2 :color 0x64748b}))
                                  _ (.grid view (clj->js {:divideX 6 :divideY 5 :color 0x1e293b}))
                                  curve (.interval view
                                                   (clj->js {:width width
                                                             :expr (fn [emit _ i _t]
                                                                     (let [u (/ i (dec width))
                                                                           x (+ -2.5 (* u 5))
                                                                           y (potential x)]
                                                                       (emit x y 0)))}))
                                  particle (.interval view
                                                      (clj->js {:width 1
                                                                :expr (fn [emit _ _ t]
                                                                        (swap! !state step t)
                                                                        (let [x (:x @!state)
                                                                              y (potential x)]
                                                                          (emit x y 0)))}))]
                              (.line curve (clj->js {:width 3 :color 0x94a3b8}))
                              (.point particle (clj->js {:size 12 :color 0xfbbf24})))
                            (restart!)
                            (reset! !mathbox mathbox))))))]
        (let [{:keys [ke pe total]} @!energy
              scale (if (pos? total) (/ 100 total) 0)]
          [:div {:class "space-y-4"}
           [:div {:class "flex flex-wrap items-center justify-between gap-4"}
            [:div {:class "text-lg font-semibold text-slate-800"}
             (or title "MathBox: 势阱与能量交换")]
            [:div {:class "flex flex-wrap items-center gap-3 text-sm text-slate-600"}
             [:label {:class "flex items-center gap-2"}
              [:span "k"]
              [:input {:type "range"
                       :min 0.5
                       :max 2.5
                       :step 0.1
                       :value @!k
                       :on-change (fn [e]
                                    (update! !k (js/parseFloat (.. e -target -value))))}]
              [:span (.toFixed @!k 1)]]
             [:label {:class "flex items-center gap-2"}
              [:span "alpha"]
              [:input {:type "range"
                       :min 0.0
                       :max 0.6
                       :step 0.05
                       :value @!alpha
                       :on-change (fn [e]
                                    (update! !alpha (js/parseFloat (.. e -target -value))))}]
              [:span (.toFixed @!alpha 2)]]
             [:label {:class "flex items-center gap-2"}
              [:span "初始速度"]
              [:input {:type "range"
                       :min 0.0
                       :max 2.5
                       :step 0.1
                       :value @!v0
                       :on-change (fn [e]
                                    (update! !v0 (js/parseFloat (.. e -target -value))))}]
              [:span (.toFixed @!v0 1)]]]]
           [:div {:class "grid gap-2 text-sm text-slate-600"}
            [:div {:class "flex items-center gap-3"}
             [:div {:class "w-16"} "动能"]
             [:div {:class "h-2 flex-1 rounded-full bg-slate-200"}
              [:div {:style {:height "100%"
                             :width (str (* ke scale) "%")
                             :background "#38bdf8"
                             :border-radius "9999px"}}]]
             [:div {:class "w-16 text-right"} (.toFixed ke 2)]]
            [:div {:class "flex items-center gap-3"}
             [:div {:class "w-16"} "势能"]
             [:div {:class "h-2 flex-1 rounded-full bg-slate-200"}
              [:div {:style {:height "100%"
                             :width (str (* pe scale) "%")
                             :background "#f97316"
                             :border-radius "9999px"}}]]
             [:div {:class "w-16 text-right"} (.toFixed pe 2)]]]
           [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
            (fn [_]
              [:div {:style {:width "100%"
                             :height "420px"
                             :border "1px solid #e2e8f0"
                             :border-radius "16px"
                             :overflow "hidden"
                             :background "#0f172a"}
                     :ref mount!}])]])
        (finally
          (when-let [mathbox @!mathbox]
            (when (.-destroy mathbox)
              (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-well-viewer {:title "MathBox: 势阱与能量交换"})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## MathBox: 环面上的测地线

用一个简化的测地线模型来复刻演讲里的甜甜圈轨迹. 调整 R 可以切换
苹果环面与柠檬环面的感觉, 初始速度控制轨迹缠绕程度。
")

^{::clerk/visibility {:code :show :result :hide}}
(def mathbox-torus-geodesic-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [title]}]
      (reagent.core/with-let
          [!R (reagent.core/atom 1.8)
           !u0 (reagent.core/atom 1.2)
           !v0 (reagent.core/atom 0.2)
           !v-start (reagent.core/atom 0.6)
           !mathbox (reagent.core/atom nil)
           trail-len 240
           !state (reagent.core/atom {:u 0.0 :v 0.6 :vdot 0.2 :c 0.0 :last nil})
           !trail (reagent.core/atom (vec (repeat trail-len [0 0 0])))
           restart! (fn []
                      (let [R @!R
                            r 1.0
                            v-start @!v-start
                            u0 @!u0
                            v0 @!v0
                            c (* (js/Math.pow (+ R (* r (js/Math.cos v-start))) 2) u0)]
                        (reset! !state {:u 0.0 :v v-start :vdot v0 :c c :last nil})
                        (reset! !trail (vec (repeat trail-len [0 0 0])))))
           update! (fn [atom value]
                     (reset! atom value)
                     (restart!))
           mount! (fn [el]
                    (when (and el (nil? @!mathbox))
                      (let [mathbox-fn (.-mathBox js/window)]
                        (when mathbox-fn
                          (let [controls (.-OrbitControls js/THREE)
                                options (cond-> {:element el
                                                 :plugins ["core" "controls" "cursor"]}
                                          controls (assoc :controls {:klass controls}))
                                mathbox (mathbox-fn (clj->js options))
                                three (.-three mathbox)
                                renderer (.-renderer three)
                                r 1.0
                                step (fn [state t]
                                       (let [last (:last state)]
                                         (if (nil? last)
                                           (assoc state :last t)
                                           (let [dt (min 0.02 (max 0 (- t last)))
                                                 R @!R
                                                 u (:u state)
                                                 v (:v state)
                                                 vdot (:vdot state)
                                                 c (:c state)
                                                 denom (js/Math.pow (+ R (* r (js/Math.cos v))) 3)
                                                 vddot (- (/ (* (js/Math.sin v) c c) (* r denom)))
                                                 vdot' (+ vdot (* vddot dt))
                                                 v' (+ v (* vdot' dt))
                                                 u' (+ u (* (/ c (js/Math.pow (+ R (* r (js/Math.cos v'))) 2)) dt))]
                                             (assoc state :u u' :v v' :vdot vdot' :last t)))))]
                            (.setClearColor renderer (js/THREE.Color. 0x0f172a) 1.0)
                            (when-let [camera (.-camera three)]
                              (.set (.-position camera) 0 0 6)
                              (.lookAt camera (js/THREE.Vector3. 0 0 0)))
                            (let [view (.cartesian mathbox
                                                   (clj->js {:range [[-4 4] [-4 4] [-3 3]]
                                                             :scale [1 1 1]}))
                                  _ (.axis view (clj->js {:axis 1 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 2 :color 0x64748b}))
                                  _ (.axis view (clj->js {:axis 3 :color 0x64748b}))
                                  _ (.grid view (clj->js {:divideX 8 :divideY 8 :color 0x1e293b}))
                                  rings 18
                                  sides 12
                                  wire-width 128
                                  two-pi (* 2 js/Math.PI)]
                              (dotimes [i rings]
                                (let [u (* (/ i rings) two-pi)
                                      line (.interval view
                                                      (clj->js {:width wire-width
                                                                :expr (fn [emit _ j _t]
                                                                        (let [v (* (/ j (dec wire-width)) two-pi)
                                                                              R @!R
                                                                              x (* (+ R (* r (js/Math.cos v))) (js/Math.cos u))
                                                                              y (* (+ R (* r (js/Math.cos v))) (js/Math.sin u))
                                                                              z (* r (js/Math.sin v))]
                                                                          (emit x y z)))}))]
                                  (.line line (clj->js {:width 1 :color 0x1e293b}))))
                              (dotimes [i sides]
                                (let [v (* (/ i sides) two-pi)
                                      line (.interval view
                                                      (clj->js {:width wire-width
                                                                :expr (fn [emit _ j _t]
                                                                        (let [u (* (/ j (dec wire-width)) two-pi)
                                                                              R @!R
                                                                              x (* (+ R (* r (js/Math.cos v))) (js/Math.cos u))
                                                                              y (* (+ R (* r (js/Math.cos v))) (js/Math.sin u))
                                                                              z (* r (js/Math.sin v))]
                                                                          (emit x y z)))}))]
                                  (.line line (clj->js {:width 1 :color 0x1e293b}))))
                              (let [path (.interval view
                                                    (clj->js {:width trail-len
                                                              :expr (fn [emit _ i t]
                                                                      (when (= i 0)
                                                                        (let [{:keys [u v]} (swap! !state step t)
                                                                              R @!R
                                                                              x (* (+ R (* r (js/Math.cos v))) (js/Math.cos u))
                                                                              y (* (+ R (* r (js/Math.cos v))) (js/Math.sin u))
                                                                              z (* r (js/Math.sin v))]
                                                                          (swap! !trail (fn [trail]
                                                                                          (let [next (conj trail [x y z])]
                                                                                            (if (> (count next) trail-len)
                                                                                              (subvec next 1)
                                                                                              next))))))
                                                                      (let [[x y z] (nth @!trail i)]
                                                                        (emit x y z)))}))
                                    head (.interval view
                                                    (clj->js {:width 1
                                                              :expr (fn [emit _ _ _t]
                                                                      (let [[x y z] (last @!trail)]
                                                                        (emit x y z)))}))]
                                (.line path (clj->js {:width 4 :color 0x38bdf8}))
                                (.point head (clj->js {:size 10 :color 0xfbbf24}))))
                            (restart!)
                            (reset! !mathbox mathbox))))))]
        [:div {:class "space-y-4"}
         [:div {:class "flex flex-wrap items-center justify-between gap-4"}
          [:div {:class "text-lg font-semibold text-slate-800"}
           (or title "MathBox: 环面上的测地线")]
          [:div {:class "flex flex-wrap items-center gap-3 text-sm text-slate-600"}
           [:label {:class "flex items-center gap-2"}
            [:span "R"]
            [:input {:type "range"
                     :min 0.7
                     :max 2.4
                     :step 0.1
                     :value @!R
                     :on-change (fn [e]
                                  (update! !R (js/parseFloat (.. e -target -value))))}]
            [:span (.toFixed @!R 1)]]
           [:label {:class "flex items-center gap-2"}
            [:span "u'0"]
            [:input {:type "range"
                     :min 0.4
                     :max 2.0
                     :step 0.1
                     :value @!u0
                     :on-change (fn [e]
                                  (update! !u0 (js/parseFloat (.. e -target -value))))}]
            [:span (.toFixed @!u0 1)]]
           [:label {:class "flex items-center gap-2"}
            [:span "v'0"]
            [:input {:type "range"
                     :min -0.8
                     :max 0.8
                     :step 0.05
                     :value @!v0
                     :on-change (fn [e]
                                  (update! !v0 (js/parseFloat (.. e -target -value))))}]
            [:span (.toFixed @!v0 2)]]
           [:label {:class "flex items-center gap-2"}
            [:span "v0"]
            [:input {:type "range"
                     :min -1.2
                     :max 1.2
                     :step 0.05
                     :value @!v-start
                     :on-change (fn [e]
                                  (update! !v-start (js/parseFloat (.. e -target -value))))}]
            [:span (.toFixed @!v-start 2)]]]]
         [nextjournal.clerk.render/with-d3-require {:package ["mathbox@0.1.0/build/mathbox-bundle.min.js"]}
          (fn [_]
            [:div {:style {:width "100%"
                           :height "420px"
                           :border "1px solid #e2e8f0"
                           :border-radius "16px"
                           :overflow "hidden"
                           :background "#0f172a"}
                   :ref mount!}])]]
        (finally
          (when-let [mathbox @!mathbox]
            (when (.-destroy mathbox)
              (.destroy mathbox))))))})

^{::clerk/visibility {:code :show :result :show}}
(clerk/with-viewer mathbox-torus-geodesic-viewer {:title "MathBox: 环面上的测地线"})

^{::clerk/visibility {:code :hide :result :show}}
(clerk/md "
## 小结

这一页复刻了演讲中的主要示例:
符号与函数的泛型运算, 单位圆简化, 自动微分, 泰勒级数逼近,
双摆混沌, 环面扭结, 势阱能量交换, 以及环面测地线的交互可视化。
")
