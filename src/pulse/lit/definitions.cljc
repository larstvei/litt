(ns pulse.lit.definitions
  (:require [edamame.core :as e]))

(defn definitions [source-file]
  (let [forms (e/parse-string-all (slurp (str source-file)) {:all true})
        [ns-meta & metas] (->> (map meta forms)
                               (map #(assoc % :file (str source-file))))
        [ns-name & names] (map second forms)]
    (->> (map (partial assoc {:ns ns-name} :name) names)
         (map #(with-meta %2 %1) metas)
         (cons (with-meta {:ns ns-name} ns-meta)))))

(defn definition-str [{:keys [ns name]}]
  (str ns (when name "/") name))
