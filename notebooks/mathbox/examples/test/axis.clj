^#:nextjournal.clerk
{:toc true
 :no-cache true
 :visibility :hide-ns}
(ns mathbox.examples.test.axis
  (:require [mentat.clerk-utils.show :refer [show-sci]]
            [nextjournal.clerk :as clerk]))

^{::clerk/visibility {:code :hide :result :hide}}
(clerk/eval-cljs
 ;; 这些别名只在当前命名空间内生效.
 '(do (require '[mathbox.core :as mathbox])
      (require '[mathbox.primitives :as mb])
      (require '[reagent.core :as reagent])
      (require '["three" :as three])))

;; # 坐标轴

^{:nextjournal.clerk/width :wide}
(show-sci
 (reagent/with-let
   [colors {:x (three/Color. 0xff4136)
            :y (three/Color. 0x2ecc40)
            :z (three/Color. 0x0074d9)}
    axis (fn [m]
           [mb/Axis
            (merge
             {:end true
              :width 5
              :liveProps
              {:depth
               (fn [t]
                 (+ 0.5 (* 0.5 (Math/sin (* t 5)))))}
              :color (:y colors)}
             m)])]
   [mathbox/MathBox
    {:container {:style {:height "500px" :width "100%"}}
     :renderer  {:background-color 0xffffff}
     :scale 720
     :focus 1}
    [mb/Camera
     {:proxy true
      :fov 45
      :position [-0.15 0.15 3.6]}]
    [mb/Cartesian {:range [[-2 2] [-1 1] [-1 1]]
                   :scale [2 1 1]}
     [axis {:color (:x colors)}]
     [axis {:axis 2 ;; "y" 也可以
            :color (:y colors)}]
     [axis {:axis "z"
            :color (:z colors)}]
     [mb/Array
      {:id "colors"
       :live false
       :data
       (mapv (fn [color]
               [(.-r color) (.-g color) (.-b color) 1])
             [(:x colors) (:y colors) (:z colors)])}]
     [mb/Array {:channels 3
                :live false
                :data [[2 0 0]
                       [0 1.11 0]
                       [0 0 1]]}]
     [mb/Text {:data ["x" "y" "z"]}]
     [mb/Label {:color 0xffffff
                :colors "#colors"}]]]))
