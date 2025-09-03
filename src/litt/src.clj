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

(defn ast-add-node [ast node]
  (update ast :ast/children (fnil conj []) node))

(defn ast-add-meta-node [ast node]
  (update ast :ast/meta (fnil conj []) node))

(defn ast-set-node-type [ast type]
  (assoc ast :ast/node type))

(defn ast-inherit-location [ast {:token/keys [location]}]
  (let [{:loc/keys [start end]} location]
    (-> ast
        (update-in [:ast/location :loc/start] (fnil min start) start)
        (update-in [:ast/location :loc/end] (fnil max end) end))))

(defn ast-from-token [token]
  (-> {:ast/node :leaf}
      (assoc :ast/token token)
      (ast-inherit-location token)))

(defn ast-nth [ast index]
  (->> (:ast/children ast)
       (remove (comp #{:meta} :ast/node))
       (drop index)
       (first)))

(defn ast-tokens [ast]
  (if (= :leaf (:ast/node ast))
    [(:ast/token ast)]
    (mapcat ast-tokens (:ast/children ast))))

(defn ast-definition-node? [ast]
  (and (= (:ast/node ast) :list)
       (-> ast (ast-nth 0) :ast/token :token/kind (= :definition))))

(def token-open-type
  (comp {"(" :list "[" :vec "{" :map} :token/lexeme))

(defn cst->ast
  ([cst] (cst->ast {:ast/node :root} cst))
  ([node [x & xs]]
   (cond
     (nil? x) node
     (vector? x) (recur (ast-add-node node (cst->ast x)) xs)
     (skip? x) (recur node xs)
     (meta? x) (as-> (cst->ast (take 1 xs)) meta-node
                 (ast-set-node-type meta-node :meta)
                 (ast-add-node node meta-node)
                 (recur meta-node (rest xs)))
     (open? x) (-> node
                   (ast-set-node-type (token-open-type x))
                   (ast-inherit-location x)
                   (recur xs))
     (close? x) (recur (ast-inherit-location node x) xs)
     :else (recur (ast-add-node node (ast-from-token x)) xs))))

(defn parse [s]
  (-> s lex tokens->cst cst->ast))

(defn loc-substring [s {:loc/keys [start end]}]
  (subs s start end))

(defn str->definition-name [s]
  (zipmap [:ns :name :dispatch] (s/split s #"/|@")))

(defn definition-name->str [{:keys [ns name dispatch]}]
  (str ns (when name "/") name (when dispatch "@") dispatch))

(defn ast-extract-definition-name [{:def/keys [src] :as definition} ast]
  (let [op (-> ast (ast-nth 0) :ast/token :token/lexeme)
        name (-> ast (ast-nth 1) :ast/token :token/lexeme)
        dispatch (delay (loc-substring src (:ast/location (ast-nth ast 2))))]
    (cond-> {:ns (:def/ns definition)}
      (not= op "ns") (assoc :name name)
      (= op "defmethod") (assoc :dispatch @dispatch))))

(defn definition-info [definition {:ast/keys [location] :as ast}]
  (let [definition-name (ast-extract-definition-name definition ast)]
    (-> definition
        (assoc :def/location location)
        (assoc :def/ast ast)
        (assoc :def/name definition-name)
        (update :def/src loc-substring location))))

(defn definitions [{:file/keys [filename content]}]
  (let [ast (parse content)
        ns-name (-> ast (ast-nth 0) (ast-nth 1) :ast/token :token/lexeme)]
    (-> (partial definition-info {:def/filename filename
                                  :def/ns ns-name
                                  :def/src content})
        (map (filter ast-definition-node? (:ast/children ast))))))

(defn str-insert [s ins i]
  (str (subs s 0 i) ins (subs s i)))

(defn wrap-css-class [offset src {:token/keys [kind location] :as token}]
  (let [span (str "<span class=\"" (name kind) "\">")
        span-end "</span>"]
    (-> src
        (str-insert span-end (- (:loc/end location) offset))
        (str-insert span (- (:loc/start location) offset)))))

(defn highlight [{:def/keys [ast location src]}]
  (->> (reverse (ast-tokens ast))
       (reduce (partial wrap-css-class (:loc/start location)) src)))
