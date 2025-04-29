(ns litt.src-test
  (:require
   [clojure.test :as t]
   [edamame.core :as e]
   [litt.src :as src]))

(t/deftest str->definition-name
  (t/are [s expected] (= (src/str->definition-name s) expected)
    "ns"     '{:ns ns}
    "ns/f"   '{:ns ns :name f}
    "ns/m@d" '{:ns ns :name m :dispatch d}))

(t/deftest definition-name->str
  (t/are [d expected] (= (src/definition-name->str d) expected)
    '{:ns ns}                     "ns"
    '{:ns ns :name f}             "ns/f"
    '{:ns ns :name m :dispatch d} "ns/m@d"))

(t/deftest extract-definition-name
  (t/are [d expected] (= (src/extract-definition-name d) expected)
    '(ns ns) '{}
    '(defn foo [] 1) '{:name foo}
    '(defmethod foo :val [] 1) '{:name foo :dispatch :val}))

(t/deftest definition-info
  (let [info (src/definition-info
               "foo.clj"
               'foo
               ["" "(defn foo []" "  'bar)" ""]
               (src/parse-definition "\n(defn foo []\n  'bar)\n"))]
    (t/is (= (:def/filename info) "foo.clj"))
    (t/is (= (:def/start info) 2))
    (t/is (= (:def/form info) '(defn foo [] 'bar)))
    (t/is (= (:def/lines info) ["(defn foo []" "  'bar)"]))))

(t/deftest definitions
  (t/are [file expected] (= (src/definitions file) expected)
    {:file/filename "empty.clj"
     :file/content ""}
    '{}

    {:file/filename "a.clj"
     :file/content "(ns a)"}
    '{{:ns a}
      {:def/filename "a.clj"
       :def/start 1
       :def/form (ns a)
       :def/lines ["(ns a)"]}}

    {:file/filename "b.clj"
     :file/content "(ns b)\n(defn f [] 1)\n(defn g [] 2)"}
    '{{:ns b}
      {:def/filename "b.clj"
       :def/start 1
       :def/form (ns b)
       :def/lines ["(ns b)"]}

      {:ns b :name f}
      {:def/filename "b.clj"
       :def/start 2
       :def/form (defn f [] {:form/wrapped 1})
       :def/lines ["(defn f [] 1)"]}

      {:ns b :name g}
      {:def/filename "b.clj"
       :def/start 3
       :def/form (defn g [] {:form/wrapped 2})
       :def/lines ["(defn g [] 2)"]}}))
