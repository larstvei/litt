(ns pulse.lit.references
  (:require [clojure.string :as s]))

(defn references [literary-file]
  (->> (s/split-lines (slurp literary-file))
       (keep (partial re-matches #"`(.*)`\{\.ref-def\}"))
       (map (comp #(s/split % #"/") last))
       (map (partial map symbol))
       (map (partial zipmap [:ns :name]))
       (map #(with-meta % {:file (str literary-file)}))))
