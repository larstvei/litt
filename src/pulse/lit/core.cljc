(ns pulse.lit.core
  (:require
   [babashka.fs :as fs]
   [pulse.lit.definitions :as defs]
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
  ([{:lit/keys [lit-paths src-paths] :as config}]
   (let [refs (set (mapcat refs/references (expand lit-paths)))
         defs (group-by :ns (mapcat defs/definitions (expand src-paths)))]
     (doseq [[ns ds] (sort defs)
             :let [covered (filter refs ds)
                   uncovered (remove refs ds)
                   status (if (empty? uncovered) "✅" "⚠️️")]]
       (printf "%-60s%-3d/%3d   %s%n" ns (count covered) (count ds) status)
       (doseq [{:keys [ns name]} (sort-by (comp :row meta) uncovered)]
         (printf "  %-68s%s%n" (if name name ns) "❌"))))))

(defn list-definitions
  ([] (list-definitions config))
  ([{:lit/keys [src-paths]}]
   (->> (expand src-paths)
        (mapcat defs/definitions)
        (map defs/definition->str)
        (run! println))))

(defn locate-definition
  ([name] (locate-definition config name))
  ([config name]
   ))
