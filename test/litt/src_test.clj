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
  (t/is (= (src/lex "") '()))
  (t/is (= (src/lex "()")
           '({:token/lexeme "("
              :token/kind :open
              :token/location {:loc/start 0 :loc/end 1}}
             {:token/lexeme ")"
              :token/kind :close
              :token/location {:loc/start 1 :loc/end 2}}))))

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

(t/deftest tokens->cst-basic
  (t/is (= (src/tokens->cst (src/lex "")) []))
  (t/is (= (src/tokens->cst (src/lex "{() ()}"))
           [[{:token/lexeme "{"
              :token/kind :open
              :token/location {:loc/start 0 :loc/end 1}}
             [{:token/lexeme "("
               :token/kind :open
               :token/location {:loc/start 1 :loc/end 2}}
              {:token/lexeme ")"
               :token/kind :close
               :token/location {:loc/start 2 :loc/end 3}}]
             {:token/lexeme " "
              :token/kind :whitespace
              :token/location {:loc/start 3 :loc/end 4}}
             [{:token/lexeme "("
               :token/kind :open
               :token/location {:loc/start 4 :loc/end 5}}
              {:token/lexeme ")"
               :token/kind :close
               :token/location {:loc/start 5 :loc/end 6}}]
             {:token/lexeme "}"
              :token/kind :close
              :token/location {:loc/start 6 :loc/end 7}}]])))

(t/deftest tokens->cst-example
  (let [s "(def ^:private foo [1 \"bar\"]) ; comment"
        tokens (src/lex s)
        cst (src/tokens->cst tokens)]
    (t/is (= (walk/prewalk #(or (:token/lexeme %) %) cst)
             [["(" "def" " " "^" ":private" " " "foo" " "
               ["[" "1" " " "\"bar\"" "]"]
               ")"]
              " " "; comment"]))
    (t/is (mapv vector? cst) [true false false])
    (t/is (= (-> cst first first :token/lexeme) "("))
    (t/is (= (-> cst first last :token/lexeme) ")"))
    (t/is (= tokens (flatten cst)))))

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



;; (t/deftest definition-info
;;   (let [info (src/definition-info
;;                "foo.clj"
;;                'foo
;;                ["" "(defn foo []" "  'bar)" ""]
;;                (src/parse "\n(defn foo []\n  'bar)\n"))]
;;     (t/is (= (:def/filename info) "foo.clj"))
;;     (t/is (= (:def/start info) 2))
;;     (t/is (= (:def/form info) '(defn foo [] 'bar)))
;;     (t/is (= (:def/lines info) ["(defn foo []" "  'bar)"]))))

;; (t/deftest definitions
;;   (t/are [file expected] (= (src/definitions file) expected)
;;     {:file/filename "empty.clj"
;;      :file/content ""}
;;     '{}

;;     {:file/filename "a.clj"
;;      :file/content "(ns a)"}
;;     '{{:ns a}
;;       {:def/filename "a.clj"
;;        :def/start 1
;;        :def/form (ns a)
;;        :def/lines ["(ns a)"]}}

;;     {:file/filename "b.clj"
;;      :file/content "(ns b)\n(defn f [] 1)\n(defn g [] 2)"}
;;     '{{:ns b}
;;       {:def/filename "b.clj"
;;        :def/start 1
;;        :def/form (ns b)
;;        :def/lines ["(ns b)"]}

;;       {:ns b :name f}
;;       {:def/filename "b.clj"
;;        :def/start 2
;;        :def/form (defn f [] {:form/wrapped 1})
;;        :def/lines ["(defn f [] 1)"]}

;;       {:ns b :name g}
;;       {:def/filename "b.clj"
;;        :def/start 3
;;        :def/form (defn g [] {:form/wrapped 2})
;;        :def/lines ["(defn g [] 2)"]}}))
