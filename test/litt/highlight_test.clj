(ns litt.highlight-test
  (:require [litt.highlight :as highlight]
            [clojure.test :as t]
            [edamame.core :as e]
            [litt.src :as src]))

(t/deftest highlight
  (t/is (= (highlight/highlight
            (val
             (first
              (src/definitions {:file/filename "foo.clj"
                                :file/content "(defn foo [] 1)"}))))
           "(<span class=\"macro\">defn</span> foo [] 1)")))
