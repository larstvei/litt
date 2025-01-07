(ns litt.db
  (:require
   [babashka.fs :as fs]
   [litt.definitions :as defs]
   [litt.references :as refs]))

(def config
  {:config/title "Litt"
   :config/export-path "dist"
   :config/lit-paths ["chapters/**.md"]
   :config/src-paths ["{src,test}**.clj*"]
   :config/css-paths ["css/**.css"]})

(defn sync-definitions [{:sources/keys [src] :as db}]
  (-> (fn [db path file]
        (update db :lit/definitions merge (defs/definitions file)))
      (reduce-kv db src)))

(defn sync-references [{:sources/keys [lit] :as db}]
  (-> (fn [db path file]
        (update db :lit/references
                (partial apply merge-with into)
                (refs/references file)))
      (reduce-kv db lit)))

(defn expand [paths]
  (->> (mapcat (partial fs/glob ".") paths)
       (filter fs/regular-file?)
       (map str)))

(defn add-file [files file]
  (->> {:file/file file
        :file/content (slurp file)
        :file/read-at (java.time.Instant/now)}
       (assoc files file)))

(defn initialize-db [{:config/keys [lit-paths src-paths css-paths]}]
  (-> config
      (assoc :sources/lit (reduce add-file {} (expand lit-paths)))
      (assoc :sources/src (reduce add-file {} (expand src-paths)))
      (assoc :sources/css (reduce add-file {} (expand css-paths)))
      (sync-definitions)
      (sync-references)))

(defonce db (atom (initialize-db config)))
