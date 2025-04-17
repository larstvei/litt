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
