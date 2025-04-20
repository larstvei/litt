(ns litt.highlight
  (:require [clojure.string :as s]
            [clojure.walk :as walk]))

(defn macro? [form]
  (and (symbol? form) (:macro (meta (resolve form)))))

(defn maybe-highlight-type [form]
  (cond (special-symbol? form) :special-form
        (macro? form) :macro
        (keyword? (:form/wrapped form)) :keyword
        (string? (:form/wrapped form)) :string
        (and (symbol? form)
             (= 2 (:col (meta form)))) :special-form))

(defn wrap-css-class [lines {:highlight/keys [type line from to]}]
  (->> (fn [s]
         (format "%s<span class=\"%s\">%s</span>%s"
                 (subs s 0 from)
                 (name type)
                 (subs s from to)
                 (subs s to)))
       (update lines line)))

(defn highlight [{:def/keys [start form lines]}]
  (->> (tree-seq coll? identity form)
       (keep #(when-let [type (maybe-highlight-type %)]
                (when-let [{:keys [row col end-row end-col]} (meta %)]
                  (assert (= row end-row))
                  {:highlight/type type
                   :highlight/line (- row start)
                   :highlight/from (dec col)
                   :highlight/to (dec end-col)})))
       (sort-by (juxt :highlight/line :highlight/from))
       (reverse)
       (reduce wrap-css-class lines)
       (s/join "\n")))
