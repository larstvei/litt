(ns litt.definitions-test
  (:require
   [clojure.test :as t]
   [litt.definitions :as defs]))

(t/deftest str->definition
  (t/are [s expected] (= (defs/str->definition s) expected)
    "ns"     '{:ns ns}
    "ns/f"   '{:ns ns :name f}
    "ns/m@d" '{:ns ns :name m :dispatch d}))

(t/deftest definition->str
  (t/are [d expected] (= (defs/definition->str d) expected)
    '{:ns ns}                     "ns"
    '{:ns ns :name f}             "ns/f"
    '{:ns ns :name m :dispatch d} "ns/m@d"))

(t/deftest form->definition
  (t/are [d expected] (= (defs/form->definition 'ns d) expected)
    '(ns ns) '{:ns ns}
    '(defn foo [] 1) '{:ns ns :name foo}
    '(defmethod foo :val [] 1) '{:ns ns :name foo :dispatch :val}
    '(comment (+ 1 2) ...) nil))
