(ns litt.typesetting
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [cheshire.core :as json]
   [hiccup2.core :as hiccup]
   [litt.src :as src]
   [pandocir.core :as pandocir]
   [litt.db :as db]))

(defn call-pandoc [content]
  (let [{:keys [out err]}
        (process/sh {:in content}
                    "pandoc"
                    "-M" "link-citations=true"
                    "--citeproc"
                    "--bibliography" "resources/bibliography.json"
                    "--csl" "resources/apa.csl"
                    "-t" "json")]
    (when-not (empty? err)
      (binding [*out* *err*]
        (println "Pandoc error: " err)))
    (json/parse-string out keyword)))

(defn include-code-block [db name]
  {:pandocir/type :pandocir.type/code-block
   :pandocir/text (db/definition-source db name)})

(defn filters [db]
  {:pandocir.type/raw-inline
   (fn [{:pandocir/keys [format text]}]
     (case format
       "litt"
       (include-code-block db text)

       "litt-file"
       (assoc {:pandocir/type :pandocir.type/code-block}
              :pandocir/text (slurp text))))})

(defn page [db body]
  (-> [:html {:lang "nb"}
       [:head
        [:meta {:charset "UTF-8"}]
        [:meta {:name "viewport"
                :content "width=device-width, initial-scale=1.0"}]
        [:meta {:name "author", :content "Lars Tveito"}]
        (for [{:file/keys [content]} (vals (:sources/css db))]
          [:style content])
        [:title (:config/title db)]]
       [:body body]]
      hiccup/html
      str))

(defn md-file->html [{:sources/keys [lit] :as db} path]
  (->> (pandocir/raw->ir (call-pandoc (get-in lit [path :file/content])))
       (pandocir/postwalk (filters db))
       (pandocir/ir->hiccup)))

(defn typeset! [{:config/keys [export-path] :sources/keys [assets css lit] :as db}]
  (fs/create-dirs export-path)
  (doseq [{:file/keys [file content]} (into (vals css) (vals assets))]
    (fs/create-dirs (fs/parent (fs/path export-path file)))
    (spit (str (fs/path export-path file)) content))
  (-> (fn [{:file/keys [file content]}]
        (let [[base _] (fs/split-ext file)
              out-path (str export-path "/" base ".html")
              exported (md-file->html db file)]
          (fs/create-dirs (fs/parent out-path))
          (spit out-path (page db exported))))
      (pmap (vals lit))
      (doall)))
