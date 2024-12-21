(ns pulse.lit.core
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]
   [pulse.lit.definitions :as defs]
   [pulse.lit.lsp :as lsp]
   [pulse.lit.references :as refs]))

(def config
  {:lit/lit-paths ["**.md"]
   :lit/src-paths ["**.clj*"]})

(defn expand [paths]
  (->> (mapcat (partial fs/glob ".") paths)
       (filter fs/regular-file?)
       (map str)))

(defn report-coverage
  ([] (report-coverage config))
  ([{:lit/keys [lit-paths src-paths]}]
   (let [refs (set (mapcat refs/references (expand lit-paths)))
         defs (group-by :ns (mapcat defs/definitions (expand src-paths)))]
     (doseq [[ns ds] (sort defs)
             :let [covered (filter refs ds)
                   uncovered (remove refs ds)
                   status (if (empty? uncovered) "✅" "⚠️️")]]
       (printf "%-60s%-3d/%3d   %s%n" ns (count covered) (count ds) status)
       (doseq [{:keys [ns name]} (sort-by (comp :row meta) uncovered)]
         (printf "  | %-66s%s%n" (if name name ns) "❌"))))))

(defn list-definitions
  ([] (list-definitions config))
  ([{:lit/keys [src-paths]}]
   (->> (expand src-paths)
        (mapcat defs/definitions)
        (map defs/definition->str)
        (run! println))))

(defn definition-info
  ([name] (definition-info config name))
  ([{:lit/keys [src-paths]} name]
   (-> (expand src-paths)
       (defs/locate-definition-by-name name)
       (meta)
       (pp/pprint))))

(defn lsp
  ([] (lsp config))
  ([config] (lsp/lsp-loop config)))
