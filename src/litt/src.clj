(ns litt.src
  (:require
   [clojure.string :as s]))

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

(def definition-regex
  (let [operators ["ns" "def" "defonce" "defn" "defn-"
                   "defmacro" "definline" "defmulti" "defmethod"
                   "defprotocol" "definterface" "defrecord"
                   "deftype" "defstruct"
                   "deftest" "deftest-"]]
    (re-pattern (str "([\\w\\.]+/)?(" (s/join "|" operators) ")"))))

(defn symbol-kind [match]
  (let [sym (symbol match)]
    (cond (re-matches definition-regex match) :definition
          (special-symbol? sym) :special-symbol
          (:macro (meta (resolve sym))) :macro
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

(defn parse [s]
  (-> s lex tokens->cst))

(defn prune [[node & tree]]
  (lazy-seq
   (cond (nil? node) nil
         (or (skip? node) (open? node) (close? node)) (prune tree)
         (meta? node) (prune (rest tree))
         :else (cons node (prune tree)))))

(defn form-location [form]
  {:loc/start (:loc/start (:token/location (first form)))
   :loc/end (:loc/end (:token/location (last form)))})

(defn loc-substring [s {:loc/keys [start end]}]
  (subs s start end))

(defn str->definition-name [s]
  (zipmap [:ns :name :dispatch] (s/split s #"/|@")))

(defn definition-name->str [{:keys [ns name dispatch]}]
  (str ns (when name "/") name (when dispatch "@") dispatch))

(defn tokens->str [tokens]
  (apply str (map :token/lexeme (flatten tokens))))

(defn definition-name [definition op name dispatch]
  (let [op-str (:token/lexeme op)
        name-str (tokens->str [name])
        dispatch-str (tokens->str [dispatch])]
    (cond-> {:ns (:def/ns definition)}
      (not= op-str "ns") (assoc :name name-str)
      (= op-str "defmethod") (assoc :dispatch dispatch-str))))

(defn definition-info [definition parse-tree]
  (let [[op name dispatch] (prune parse-tree)
        location (form-location parse-tree)]
    (when (= :definition (:token/kind op))
      (-> definition
          (assoc :def/location location)
          (assoc :def/parse-tree parse-tree)
          (assoc :def/name (definition-name definition op name dispatch))
          (update :def/src loc-substring location)))))

(defn definitions [{:file/keys [filename content]}]
  (let [parse-tree (parse content)
        ns-name (-> parse-tree first prune (nth 1) :token/lexeme)]
    (-> (partial definition-info {:def/filename filename
                                  :def/ns ns-name
                                  :def/src content})
        (keep parse-tree))))

(defn str-insert [s ins i]
  (str (subs s 0 i) ins (subs s i)))

(defn wrap-css-class [offset src {:token/keys [kind location] :as token}]
  (let [span (str "<span class=\"" (name kind) "\">")
        span-end "</span>"]
    (-> src
        (str-insert span-end (- (:loc/end location) offset))
        (str-insert span (- (:loc/start location) offset)))))

(defn highlight [{:def/keys [parse-tree location src]}]
  (->> (reverse (flatten parse-tree))
       (reduce (partial wrap-css-class (:loc/start location)) src)))
