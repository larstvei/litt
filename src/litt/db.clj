(ns litt.db
  (:require
   [babashka.fs :as fs]
   [clojure.string :as s]
   [litt.references :as refs]
   [litt.src :as src]))

(defonce db (atom {}))

(def config
  {:config/title "Den lille boken om Litt"
   :config/export-path "dist"
   :config/lit-paths ["kapitler/**.md"]
   :config/src-paths ["{src,test}/**.clj*"]
   :config/css-paths ["resources/css/**.css"]})

(defn sync-definitions [{:sources/keys [src] :as db}]
  (-> (fn [db path file]
        (update db :lit/definitions merge (src/definitions file)))
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

(defn get-definition [db def-name-str]
  (->> [:lit/definitions (src/str->definition-name def-name-str)]
       (get-in db)))

(defn definition-source [db def-name-str]
  (->> (:def/lines (get-definition db def-name-str))
       (s/join "\n")))

(defn update-content! [path op]
  (swap! db (fn [db] (sync-references (update-in db (conj path :file/content) op)))))

(defn initialize-db! []
  (let [{:config/keys [title lit-paths src-paths css-paths asset-paths]} config
        initial-state
        (-> config
            (assoc :sources/lit (reduce add-file {} (expand lit-paths)))
            (assoc :sources/src (reduce add-file {} (expand src-paths)))
            (assoc :sources/css (reduce add-file {} (expand css-paths)))
            (assoc :sources/assets (reduce add-file {} (expand asset-paths)))
            (sync-definitions)
            (sync-references))]
    (reset! db initial-state)))

(comment
  (initialize-db!)
  )
