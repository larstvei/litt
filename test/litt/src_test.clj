(ns litt.src-test
  (:require
   [clojure.test :as t]
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
  (t/are [d expected] (= (src/extract-definition-name 'ns d) expected)
    '(ns ns) '{:ns ns}
    '(defn foo [] 1) '{:ns ns :name foo}
    '(defmethod foo :val [] 1) '{:ns ns :name foo :dispatch :val}
    '(comment (+ 1 2) ...) nil))
