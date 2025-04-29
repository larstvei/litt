(ns litt.highlight-test
  (:require [litt.highlight :as highlight]
            [clojure.test :as t]
            [litt.src :as src]))

(t/deftest highlight
  (t/is (= (highlight/highlight
            {:def/ast (src/parse-definition "(defn foo [] 1)")
             :def/start 1
             :def/lines ["(defn foo [] 1)"]})
           (str "(<span class=\"macro\">defn</span>"
                " <span class=\"symbol\">foo</span> []"
                " <span class=\"number\">1</span>)"))))
