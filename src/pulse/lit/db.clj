(ns pulse.lit.db
  (:require
   [babashka.fs :as fs]
   [pulse.lit.definitions :as defs]
   [pulse.lit.references :as refs]))

(def config
  {:config/lit-paths ["**.md"]
   :config/src-paths ["**.clj*"]
   :config/css-path "src/styles.css"
   :config/export-path "dist"})

(defn expand [paths]
  (->> (mapcat (partial fs/glob ".") paths)
       (filter fs/regular-file?)
       (map str)))

(defn add-definitions [db source-file]
  (update db :lit/definitions merge (defs/definitions source-file)))

(defn add-references [db literary-file]
  (->> (refs/references literary-file)
       (update db :lit/references (partial merge-with into))))

(defn initialize-db [{:config/keys [lit-paths src-paths]}]
  (as-> config db
    (reduce add-references db (expand lit-paths))
    (reduce add-definitions db (expand src-paths))))

(defonce db (atom (initialize-db config)))
