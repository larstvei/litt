(ns litt.references
  (:require
   [clojure.string :as s]
   [litt.src :as src]))

(defn references [{:file/keys [file content]}]
  (->> (s/split-lines content)
       (keep-indexed
        (fn [i line]
          (when-let [[_ match] (re-matches #"`(.*)`\{=litt\}" line)]
            {(src/str->definition-name match) [{:file file :line (inc i)}]})))))
