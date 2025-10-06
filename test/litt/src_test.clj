(ns litt.src-test
  (:require
   [clojure.test :as t]
   [litt.src :as src]
   [clojure.walk :as walk]))

(t/deftest symbol-kind
  (t/is (= :definition (src/symbol-kind "ns")))
  (t/is (= :definition (src/symbol-kind "def")))
  (t/is (= :definition (src/symbol-kind "t/deftest")))
  (t/is (= :definition (src/symbol-kind "clojure.test/deftest")))
  (t/is (= :special-symbol (src/symbol-kind "if")))
  (t/is (= :macro (src/symbol-kind "when")))
  (t/is (= :symbol (src/symbol-kind "foo")))
  (t/is (= :symbol (src/symbol-kind "user/foo"))))

(t/deftest make-token
  (t/is (= (src/make-token "x" :symbol 7 8)
           {:token/lexeme "x" :token/kind :symbol
            :token/location {:loc/start 7 :loc/end 8}})))

(t/deftest lex-basic
  (t/is (= (src/lex "") ()))
  (t/is (= (src/lex "()")
           [(src/make-token "(" :open 0 1)
            (src/make-token ")" :close 1 2)])))

(t/deftest lex-example
  (let [s "(def ^:private foo [1 \"bar\"]) ; comment"
        tokens (src/lex s)
        kinds (mapv :token/kind tokens)
        lexemes (mapv :token/lexeme tokens)]
    (t/is (= (subvec kinds 0 4)
             [:open :definition :whitespace :meta]))
    (t/is (= (subvec kinds (- (count kinds) 4))
             [:close :close :whitespace :comment]))
    (t/is (= (-> tokens first :token/location :loc/start) 0))
    (t/is (= (-> tokens last  :token/location :loc/end) (count s)))
    (t/is (= (apply str lexemes) s))))

(t/deftest tokens->parse-tree-basic
  (t/is (= (src/tokens->parse-tree (src/lex "")) []))
  (t/is (= (src/tokens->parse-tree (src/lex "{() ()}"))
           [[(src/make-token "{" :open 0 1)
             [(src/make-token "(" :open 1 2)
              (src/make-token ")" :close 2 3)]
             (src/make-token " " :whitespace 3 4)
             [(src/make-token "(" :open 4 5)
              (src/make-token ")" :close 5 6)]
             (src/make-token "}" :close 6 7)]])))

(t/deftest tokens->parse-tree-example
  (let [s "(def ^:private foo [1 \"bar\"]) ; comment"
        tokens (src/lex s)
        parse-tree (src/tokens->parse-tree tokens)]
    (t/is (= (walk/prewalk #(or (:token/lexeme %) %) parse-tree)
             [["(" "def" " " "^" ":private" " " "foo" " "
               ["[" "1" " " "\"bar\"" "]"]
               ")"]
              " " "; comment"]))
    (t/is (mapv vector? parse-tree) [true false false])
    (t/is (= (-> parse-tree first first :token/lexeme) "("))
    (t/is (= (-> parse-tree first last :token/lexeme) ")"))
    (t/is (= tokens (flatten parse-tree)))))

(t/deftest prune
  (t/are [s expected] (= (src/prune (src/parse s)) expected)
    "" []
    "()" [[]]
    "[]" [[]]
    "{}" [[]]
    "() ()" [[] []]
    "{() ()}" [[[] []]]
    "symbol" [(src/make-token "symbol" :symbol 0 6)]
    "(a [b] ; ok\nc)" [[(src/make-token "a" :symbol 1 2)
                        [(src/make-token "b" :symbol 4 5)]
                        (src/make-token "c" :symbol 12 13)]]))

(t/deftest str->definition-name
  (t/are [s expected] (= (src/str->definition-name s) expected)
    "ns"     {:ns "ns"}
    "ns/f"   {:ns "ns" :name "f"}
    "ns/m@d" {:ns "ns" :name "m" :dispatch "d"}))

(t/deftest definition-name->str
  (t/are [d expected] (= (src/definition-name->str d) expected)
    {:ns "ns"}                         "ns"
    {:ns "ns" :name "f"}               "ns/f"
    {:ns "ns" :name "m" :dispatch "d"} "ns/m@d"))

(t/deftest definition-info
  (let [src "\n(defn foo []\n  'bar)\n"
        info (src/definition-info
               {:def/filename "foo.clj" :def/ns "foo" :def/src src}
               (second (src/parse src)))]
    (t/is (= (:def/filename info) "foo.clj"))
    (t/is (= (:def/ns info) "foo"))
    (t/is (= (:loc/start (:def/location info)) 1))
    (t/is (= (:def/src info) "(defn foo []\n  'bar)"))))

(t/deftest definitions
  (t/are
      [file expected]
      (->> (src/definitions file)
           (map #(dissoc % :def/parse-tree :def/location))
           (= expected))

    {:file/filename "empty.clj"
     :file/content ""}
    '()

    {:file/filename "a.clj"
     :file/content "(ns a)"}
    '({:def/filename "a.clj",
       :def/ns "a",
       :def/src "(ns a)",
       :def/name {:ns "a"}})

    {:file/filename "b.clj"
     :file/content "(ns b)\n(defn f [] 1)\n(defn g [] 2)"}
    '({:def/filename "b.clj",
       :def/ns "b",
       :def/src "(ns b)",
       :def/name {:ns "b"}}
      {:def/filename "b.clj",
       :def/ns "b",
       :def/src "(defn f [] 1)",
       :def/name {:ns "b", :name "f"}}
      {:def/filename "b.clj",
       :def/ns "b",
       :def/src "(defn g [] 2)",
       :def/name {:ns "b", :name "g"}})))
