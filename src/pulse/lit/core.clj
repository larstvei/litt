(ns pulse.lit.core
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]
   [pulse.lit.db :as db]
   [pulse.lit.definitions :as defs]
   [pulse.lit.export :as export]
   [pulse.lit.lsp :as lsp]))

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

(defn export
  ([] (export @db/db))
  ([db] (export/export! db)))

(defn lsp []
  (lsp/lsp-loop))
