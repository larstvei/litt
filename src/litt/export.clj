(ns litt.export
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [cheshire.core :as json]
   [hiccup2.core :as hiccup]
   [pandocir.core :as pandocir]
   [litt.definitions :as defs]))

(defn call-pandoc [content]
  (let [{:keys [out err]} (process/sh {:in content} "pandoc" "-t" "json")]
    (when-not (empty? err)
      (binding [*out* *err*]
        (println "Pandoc error: " err)))
    (json/parse-string out keyword)))

(defn include-code-block [defs name]
  {:pandocir/type :pandocir.type/code-block
   :pandocir/text (:source (defs/locate-definition-by-name defs name))})

(defn filters [defs]
  {:pandocir.type/raw-inline
   (fn [{:pandocir/keys [format text]}]
     (case format
       "ref-def"
       (include-code-block defs text)

       "ref-file"
       (assoc {:pandocir/type :pandocir.type/code-block}
              :pandocir/text (slurp text))))})

(defn page [db body]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:meta {:name "author", :content "Lars Tveito"}]
    (for [css-file (keys (:sources/css db))]
      [:link {:rel "stylesheet", :href (str "/" css-file)}])
    [:title "Pulse"]]
   [:body body]])

(defn md-file->html [{:lit/keys [definitions] :as db} content]
  (->> (pandocir/postwalk
        (pandocir/raw->ir (call-pandoc content))
        (filters definitions))
       (pandocir/ir->hiccup)
       (page db)
       hiccup/html
       str))

(defn export! [{:config/keys [export-path] :sources/keys [css lit] :as db}]
  (fs/create-dirs export-path)
  (doseq [{:file/keys [file content]} (vals css)]
    (fs/create-dirs (fs/parent (fs/path export-path file)))
    (spit (str (fs/path export-path file)) content))
  (-> (fn [{:file/keys [file content]}]
        (let [[base _] (fs/split-ext file)
              out-path (str export-path "/" base ".html")
              exported (md-file->html db content)]
          (fs/create-dirs (fs/parent out-path))
          (spit out-path exported)))
      (pmap (vals lit))
      (doall)))
