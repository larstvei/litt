(ns pulse.lit.export
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [hiccup2.core :as hiccup]
   [pandocir.core :as pandocir]
   [pulse.lit.definitions :as defs]))

(defn call-pandoc [file]
  (let [{:keys [out err]} (process/sh "pandoc" file "-t" "json")]
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

(defn md-file->pandocir [defs path]
  (pandocir/postwalk
   (pandocir/raw->ir (call-pandoc path))
   (filters defs)))

(defn page [body]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:meta {:name "author", :content "Lars Tveito"}]
    [:link {:rel "stylesheet", :href "/css/styles.css"}]
    [:title "Pulse"]]
   [:body body]])

(defn lit->html [defs path]
  (->> (md-file->pandocir defs path)
       pandocir/ir->hiccup
       page
       hiccup/html
       str))
