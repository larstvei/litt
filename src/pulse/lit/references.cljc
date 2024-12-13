(ns pulse.lit.references
  (:require [clojure.string :as s]
            [pulse.lit.definitions :as defs]))

(defn references [literary-file]
  (->> (s/split-lines (slurp literary-file))
       (keep (partial re-matches #"`(.*)`\{\.ref-def\}"))
       (map (comp defs/str->definition last))
       (map #(with-meta % {:file (str literary-file)}))))
