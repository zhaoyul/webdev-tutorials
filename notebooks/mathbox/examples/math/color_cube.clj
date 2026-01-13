^#:nextjournal.clerk
{:toc true
 :no-cache true
 :visibility :hide-ns}
(ns mathbox.examples.math.color-cube
  (:require [mentat.clerk-utils.show :refer [show-sci]]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :hide}}
(clerk/eval-cljs
 ;; 这些别名只在当前命名空间内生效.
 '(do (require '[mathbox.core :as mathbox])
      (require '[mathbox.primitives :as mb])))

;; ### 颜色立方体

^{:nextjournal.clerk/width :wide}
(show-sci
 [mathbox/MathBox
  {:container {:style {:height "400px" :width "100%"}}
   :renderer  {:background-color 0xeeeeee
               :max-distance 4}
   :scale 720
   :focus 1}
  [mb/Camera
   {:position [3.5 1 2.5]
    :proxy true}]
  [mb/Cartesian {:range [[0 1] [0 1] [0 1]]}
   [mb/Volume
    {:width 8 :height 5 :depth 20
     :items 1
     :channels 4
     :live false
     :expr (fn [emit x y z]
             (emit x y z 1.0))}]
   [mb/Point
    {:points "<"
     :colors "<"
     :color 0xffffff
     :size 20}]]])
