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
  ([{:lit/keys [definitions]
     :config/keys [css-path export-path lit-paths]}]
   (fs/create-dirs (fs/path export-path "css"))
   (fs/copy css-path (fs/path export-path "css" "styles.css")
            {:replace-existing true})
   (-> (fn [path]
         (let [[base _] (fs/split-ext path)
               out-path (str export-path "/" base ".html")
               exported (export/lit->html definitions path)]
           (fs/create-dirs (fs/parent out-path))
           (spit out-path exported)))
       (pmap (db/expand lit-paths))
       (doall))))

(defn lsp
  ([] (lsp @db/db))
  ([db] (lsp/lsp-loop db)))
