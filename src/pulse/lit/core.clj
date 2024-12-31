(ns pulse.lit.core
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]
   [pulse.lit.definitions :as defs]
   [pulse.lit.export :as export]
   [pulse.lit.lsp :as lsp]
   [pulse.lit.references :as refs]))

(def config
  {:lit/lit-paths ["**.md"]
   :lit/src-paths ["**.clj*"]
   :lit/css-path "src/styles.css"
   :lit/export-path "dist"})

(defn expand [paths]
  (->> (mapcat (partial fs/glob ".") paths)
       (filter fs/regular-file?)
       (map str)))

(defn report-coverage
  ([] (report-coverage config))
  ([{:lit/keys [lit-paths src-paths]}]
   (let [refs (set (mapcat refs/references (expand lit-paths)))
         defs (into {} (map defs/definitions (expand src-paths)))]
     (doseq [[ns ds] (sort (group-by :ns (sort-by (comp :row defs) (keys defs))))
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
        (mapcat (comp keys defs/definitions))
        (map defs/definition->str)
        (run! println))))

(defn definition-info
  ([name] (definition-info config name))
  ([{:lit/keys [src-paths]} name]
   (-> (into {} (map defs/definitions (expand src-paths)))
       (defs/locate-definition-by-name name)
       (pp/pprint))))

(defn export
  ([] (export config))
  ([{:lit/keys [lit-paths src-paths css-path export-path]}]
   (let [defs (into {} (map defs/definitions (expand src-paths)))]
     (fs/create-dirs (fs/path export-path "css"))
     (fs/copy css-path (fs/path export-path "css" "styles.css")
              {:replace-existing true})
     (-> (fn [path]
           (let [[base _] (fs/split-ext path)
                 out-path (str export-path "/" base ".html")
                 exported (export/lit->html defs path)]
             (fs/create-dirs (fs/parent out-path))
             (spit out-path exported)))
         (pmap (expand lit-paths))
         (doall)))))

(defn lsp
  ([] (lsp config))
  ([config] (lsp/lsp-loop config)))
