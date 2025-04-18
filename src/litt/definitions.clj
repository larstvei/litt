(ns litt.definitions
  (:require
   [clojure.string :as s]
   [edamame.core :as e]))

(defn str->definition [s]
  (->> (map symbol (s/split s #"/|@"))
       (zipmap [:ns :name :dispatch])))

(defn definition->str [{:keys [ns name dispatch]}]
  (str ns (when name "/") name (when dispatch "@") dispatch))

(defn form->definition [ns-name [def name dispatch]]
  (when (symbol? name)
    (cond-> {:ns ns-name}
      (not= def 'ns) (assoc :name name)
      (= def 'defmethod) (assoc :dispatch dispatch))))

(defn definition-info [file lines form]
  (let [{:keys [row end-row]} (meta form)]
    {:file file
     :line row
     :source (s/join "\n" (subvec lines (dec row) end-row))}))

(defn definitions [{:file/keys [file content]}]
  (let [forms (e/parse-string-all content {:all true})
        ns-name (second (first forms))
        lines (vec (s/split-lines content))]
    (-> (fn [defs form]
          (if-let [key (form->definition ns-name form)]
            (assoc defs key (definition-info file lines form))
            defs))
        (reduce {} forms))))

(defn locate-definition-by-name [defs name]
  (defs (str->definition name)))

