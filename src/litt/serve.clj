(ns litt.serve
  (:require
   [babashka.fs :as fs]
   [clojure.string :as s]
   [litt.db :as db]
   [litt.typesetting :as typesetting]
   [org.httpkit.server :as server]))

(defn basename [path]
  (first (fs/split-ext path)))

(defn resolve-lit [{:sources/keys [lit] :as db} uri]
  ((zipmap (map basename (keys lit)) (keys lit))
   (s/replace-first uri "/" "")))

(defn lit-body [db path]
  (->> (get-in db [:sources/lit path :file/content])
       (typesetting/md-file->html db)))

(defn fallback-body [{:sources/keys [lit] :as db}]
  (->> (for [path (sort (map basename (keys lit)))]
         [:li [:a {:href (str "/" path)} path]])
       (into [:ul])
       (typesetting/page db)))

(defn app [db {:keys [uri] :as req}]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (if-let [lit-path (resolve-lit db uri)]
           (lit-body db lit-path)
           (fallback-body db))})

(defonce server
  (server/run-server (fn [req] (app @db/db req)) {:port 8080}))
