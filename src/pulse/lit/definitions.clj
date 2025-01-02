(ns pulse.lit.definitions
  (:require
   [clojure.string :as s]
   [edamame.core :as e]))

(defn str->definition [s]
  (zipmap [:ns :name] (map symbol (s/split s #"/"))))

(defn definition->str [{:keys [ns name]}]
  (str ns (when name "/") name))

(defn locate-definition-by-name [defs name]
  (defs (str->definition name)))

(defn definitions [{:file/keys [file content]}]
  (let [forms (e/parse-string-all content {:all true})
        ns-name (second (first forms))
        lines (vec (s/split-lines content))]
    (->> (for [form forms
               :let [{:keys [row end-row]} (meta form)
                     name (second form)]]
           [(cond-> {:ns ns-name}
              (not= (first form) 'ns) (assoc :name name))
            {:file file
             :line row
             :source (s/join "\n" (subvec lines (dec row) end-row))}])
         (into {}))))
