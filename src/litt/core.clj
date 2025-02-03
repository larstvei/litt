(ns litt.core
  (:require
   [clojure.pprint :as pp]
   [litt.db :as db]
   [litt.definitions :as defs]
   [litt.typesetting :as typesetting]
   [litt.lsp :as lsp]))

(defn report-coverage
  ([] (report-coverage @db/db))
  ([{:lit/keys [references definitions]}]
   (doseq [[ns ds] (->> (keys definitions)
                        (sort-by (comp :line definitions))
                        (group-by :ns)
                        sort)
           :let [covered (filter references ds)
                 uncovered (remove references ds)
                 status (if (empty? uncovered) "✅" "⚠️️")]]
     (printf "%-60s%-3d/%3d   %s%n" ns (count covered) (count ds) status)
     (doseq [{:keys [ns name]} (sort-by (comp :row meta) uncovered)]
       (printf "  | %-66s%s%n" (if name name ns) "❌")))))

(defn list-definitions
  ([] (list-definitions @db/db))
  ([{:lit/keys [definitions]}]
   (->> definitions
        (keys)
        (map defs/definition->str)
        (run! println))))

(defn definition-info
  ([name] (definition-info @db/db name))
  ([{:lit/keys [definitions]} name]
   (-> definitions
       (defs/locate-definition-by-name name)
       (pp/pprint))))

(defn typeset
  ([] (typeset @db/db))
  ([db] (typesetting/typeset! db)))

(defn lsp []
  (lsp/lsp-loop))
