(ns litt.src
  (:require
   [clojure.string :as s]
   [edamame.core :as e]))

(defn str->definition-name [s]
  (->> (map symbol (s/split s #"/|@"))
       (zipmap [:ns :name :dispatch])))

(defn definition-name->str [{:keys [ns name dispatch]}]
  (str ns (when name "/") name (when dispatch "@") dispatch))

(defn extract-definition-name [ns-name [def name dispatch]]
  (when (symbol? name)
    (cond-> {:ns ns-name}
      (not= def 'ns) (assoc :name name)
      (= def 'defmethod) (assoc :dispatch dispatch))))

(defn definition-info [file lines form]
  (let [{:keys [row end-row]} (meta form)]
    {:def/file file
     :def/start row
     :def/lines (subvec lines (dec row) end-row)}))

(defn definitions [{:file/keys [file content]}]
  (let [forms (e/parse-string-all content {:all true})
        ns-name (second (first forms))
        lines (vec (s/split-lines content))]
    (-> (fn [defs form]
          (if-let [key (extract-definition-name ns-name form)]
            (assoc defs key (definition-info file lines form))
            defs))
        (reduce {} forms))))
