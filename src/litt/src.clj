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

(defn add-location [{:keys [obj loc]}]
  (-> (if (instance? clojure.lang.IMeta obj)
        obj
        {:form/wrapped obj})
      (vary-meta merge loc)))

(defn parse-definitions [content]
  (let [parse-opts {:all true :postprocess add-location}]
    (e/parse-string-all content parse-opts)))

(defn definition-info [filename lines form]
  (let [{:keys [row end-row]} (meta form)]
    {:def/filename filename
     :def/start row
     :def/form form
     :def/lines (subvec lines (dec row) end-row)}))

(defn definitions [{:file/keys [filename content]}]
  (let [forms (parse-definitions content)
        ns-name (second (first forms))
        lines (vec (s/split-lines content))]
    (-> (fn [defs form]
          (if-let [key (extract-definition-name ns-name form)]
            (assoc defs key (definition-info filename lines form))
            defs))
        (reduce {} forms))))
