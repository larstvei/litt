(ns litt.highlight
  (:require [clojure.string :as s]
            [clojure.walk :as walk]))

(defn str-insert [s ins i]
  (str (subs s 0 i) ins (subs s i)))

(defn wrap-css-class [offset lines {:ast/keys [type location] :as ast}]
  (let [{:loc/keys [line column line-end column-end]} location
        span (str "<span class=\"" (name type) "\">")
        span-end "</span>"]
    (-> lines
        (update (- line-end offset) str-insert span-end (dec column-end))
        (update (- line offset) str-insert span (dec column)))))

(defn highlight [{:def/keys [ast start lines]}]
  (->> (tree-seq coll? identity ast)
       (filter :ast/leaf?)
       (reverse)
       (reduce (partial wrap-css-class start) lines)
       (s/join "\n")))
