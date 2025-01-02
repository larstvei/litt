(ns pulse.lit.references
  (:require
   [clojure.string :as s]
   [pulse.lit.definitions :as defs]))

(defn references [{:file/keys [file content]}]
  (->> (s/split-lines content)
       (keep-indexed
        (fn [i line]
          (when-let [[_ match] (re-matches #"`(.*)`\{=ref-def\}" line)]
            {(defs/str->definition match) [{:file file :line (inc i)}]})))))
