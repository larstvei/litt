(ns pulse.lit.references
  (:require
   [clojure.string :as s]
   [pulse.lit.definitions :as defs]))

(defn references [literary-file]
  (->> (s/split-lines (slurp literary-file))
       (keep-indexed
        (fn [i line]
          (when-let [[_ match] (re-matches #"`(.*)`\{=ref-def\}" line)]
            {(defs/str->definition match)
             {:file (str literary-file) :line (inc i)}})))
       (reduce (partial merge-with conj))))
