(ns litt.serve
  (:require
   [babashka.fs :as fs]
   [clojure.string :as s]
   [litt.db :as db]
   [litt.typesetting :as typesetting]
   [org.httpkit.server :as server]))

(defn resolve-lit [{:sources/keys [lit] :as db} uri]
  ((zipmap (map (comp first fs/split-ext) (keys lit)) (keys lit))
   (s/replace-first uri "/" "")))

(defn app [db {:keys [uri] :as req}]
  (when-let [md (resolve-lit db uri)]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (->> (get-in db [:sources/lit md :file/content])
                   (typesetting/md-file->html db))}))

(defonce server
  (server/run-server (fn [req] (app @db/db req)) {:port 8080}))
