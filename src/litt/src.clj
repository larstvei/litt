(ns litt.src
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [edamame.core :as e]))

(def lexeme-spec
  [[:whitespace #"[\s,]+"]
   [:comment    #";[^\n]*"]
   [:meta       #"\^"]
   [:string     #"\"(?:\\.|[^\"])*\""]
   [:number     #"-?\d+(?:\.\d+)?"]
   [:keyword    #":[^\s,^()\[\]{}\";]+"]
   [:symbol     #"[^\s,^()\[\]{}\";]+"]
   [:open       #"\(|\[|\{"]
   [:close      #"\)|\]|\}"]])

(def lexeme-kinds
  (mapv first lexeme-spec))

(def regex
  (->> (map second lexeme-spec)
       (map #(str "(" % ")"))
       (s/join "|")
       (re-pattern)))

(defn symbol-kind [match]
  (let [sym (symbol match)]
    (cond (special-symbol? sym) :special-symbol
          (:macro (meta (resolve sym))) :macro
          (re-find #"\bdef" match) :macro
          :else :symbol)))

(defn lexeme-kind [[match & groups]]
  (let [kind (get lexeme-kinds (count (take-while nil? groups)))]
    (if (= kind :symbol) (symbol-kind match) kind)))

(defn make-token [lexeme kind start end]
  {:token/lexeme lexeme
   :token/kind kind
   :token/location {:loc/start start :loc/end end}})

(defn lex [s]
  (let [matches (re-seq regex s)
        lexemes (map first matches)
        kinds (map lexeme-kind matches)
        starts (reductions + 0 (map count lexemes))
        ends (rest starts)]
    (map make-token lexemes kinds starts ends)))

(defn skip? [t] (#{:whitespace :comment} (:token/kind t)))
(defn open? [t] (= (:token/kind t) :open))
(defn close? [t] (= (:token/kind t) :close))
(defn meta? [t] (= (:token/kind t) :meta))

(defn tokens->cst [tokens]
  (-> (fn [[tree & stack] token]
        (cond (open? token) (conj stack tree [token])
              (close? token) (->> (conj tree token)
                                  (conj (first stack))
                                  (conj (rest stack)))
              :else (conj stack (conj tree token))))
      (reduce (list []) tokens)
      (first)))

(defn macro? [form]
  (and (symbol? form)
       (:macro (meta (resolve form)))))

(defn obj-type [leaf]
  (cond (coll? leaf) :coll
        (keyword? leaf) :keyword
        (nil? leaf) :nil
        (number? leaf) :number
        (special-symbol? leaf) :special-symbol
        (string? leaf) :string
        (macro? leaf) :macro
        (symbol? leaf) :symbol
        :else :other))

(defn postprocess [{:keys [obj loc]}]
  {:ast/entry obj
   :ast/type (obj-type obj)
   :ast/leaf? (not (coll? obj))
   :ast/location {:loc/line (:row loc)
                  :loc/column (:col loc)
                  :loc/line-end (:end-row loc)
                  :loc/column-end (:end-col loc)}})

(def parse-opts {:all true :postprocess postprocess})

(defn parse-definition [s]
  (e/parse-string s parse-opts))

(defn parse-definitions [s]
  (->> (e/parse-string-all s parse-opts)
       (filter (comp #{:symbol} :ast/type second :ast/entry))))

(defn ast->form [ast]
  (walk/prewalk (fn [node] (or (:ast/entry node) node)) ast))

(defn str->definition-name [s]
  (->> (map symbol (s/split s #"/|@"))
       (zipmap [:ns :name :dispatch])))

(defn definition-name->str [{:keys [ns name dispatch]}]
  (str ns (when name "/") name (when dispatch "@") dispatch))

(defn extract-definition-name [[op name dispatch]]
  (cond-> {}
    (not= op 'ns) (assoc :name name)
    (= op 'defmethod) (assoc :dispatch dispatch)))

(defn definition-info [filename ns-name lines ast]
  (let [{:loc/keys [line line-end] :as location} (:ast/location ast)
        form (ast->form ast)
        definition-name (-> (extract-definition-name form)
                            (assoc :ns ns-name))]
    {:def/filename filename
     :def/ast ast
     :def/name definition-name
     :def/location location
     :def/form form
     :def/start line
     :def/lines (subvec lines (dec line) line-end)}))

(defn definitions [{:file/keys [filename content]}]
  (let [asts (parse-definitions content)
        ns-name (-> asts first :ast/entry second :ast/entry)
        lines (vec (s/split-lines content))]
    (map (partial definition-info filename ns-name lines) asts)))
