^#:nextjournal.clerk
{:toc true
 :visibility :hide-ns}
(ns mathbox.examples.test.polar
  (:require [nextjournal.clerk :as clerk]))

;; ## 极坐标示例
;;
;; 这是一个与 [[functions]] 中示例相近的版本, 但函数完全在客户端定义(受时间所限, 之后可能会调整).
;;
;; 这个查看器会建立极坐标系并在其上渲染函数. 当角度超过 $2\\pi$ 时, 曲线不会重复, 而是沿着螺旋向上延展.
;;
;; 下面是查看器与交互输出:

^{::clerk/width :wide
  ::clerk/viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [{:keys [offset]}]
      [mathbox.core/MathBox {:style {:height "400px" :width "100%"}
                             :init {:background-color 0xffffff
                                    :focus 3}}
       [mathbox.primitives/Camera {:proxy true
                                   :position [0 0 3]}]
       [mathbox.primitives/Polar
        {:bend 1
         :range [[(* -2 Math/PI) (* 2 Math/PI)]
                 [0 1]
                 [-1 1]]
         :scale [2 1 1]
         :helix 0.1}
        ;; 带刻度的半径轴.
        [mathbox.primitives/Transform {:position [0 0.5 0]}
         [mathbox.primitives/Axis {:detail 256}]
         [mathbox.primitives/Scale {:divide 10 :unit Math/PI :base 2}]
         [mathbox.primitives/Ticks {:width 2
                                    :classes ["foo", "bar"]}]
         [mathbox.primitives/Ticks {:opacity 0.5
                                    :width 1
                                    :size 50
                                    :normal [0 1 0]
                                    :classes ["foo", "bar"]}]]


        ;; 极坐标轴.
        [mathbox.primitives/Axis {:axis 2}]
        [mathbox.primitives/Transform {:position [(/ Math/PI 2) 0 0]}
         [mathbox.primitives/Axis {:axis 2}]]
        [mathbox.primitives/Transform {:position [(- (/ Math/PI 2)) 0 0]}
         [mathbox.primitives/Axis {:axis 2}]]


        ;; 网格所在的不透明表面.
        [mathbox.primitives/Area {:width 256
                                  :height 2}]
        [mathbox.primitives/Surface {:color "#fff"
                                     :opacity 0.75
                                     :zBias -10}]

        ;; 添加网格线, 不透明表面已经存在.
        [mathbox.primitives/Grid {:divideX 5
                                  :detailX 256
                                  :width 1
                                  :opacity 0.5
                                  :unitX Math/PI
                                  :baseX 2
                                  :zBias -5
                                  :zOrder -2}]

        ;; 函数曲线.
        [mathbox.primitives/Interval
         {:width 256
          :expr
          (fn [emit theta _i t]
            (let [r (+ offset (* 0.5
                                 (Math/sin
                                  (* 3 (+ theta t)))))]
              (emit theta r)))
          :channels 2}]
        [mathbox.primitives/Line {:points "<"
                                  :color 0x3090ff
                                  :width 5}]]])}}
{:offset 0.5}
