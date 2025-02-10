(ns litt.serve
  (:require
   [babashka.fs :as fs]
   [clojure.string :as s]
   [litt.db :as db]
   [litt.typesetting :as typesetting]
   [org.httpkit.server :as server]))

(defonce clients (atom #{}))

(defn basename [path]
  (first (fs/split-ext path)))

(defn resolve-lit [{:sources/keys [lit] :as db} uri]
  ((zipmap (map basename (keys lit)) (keys lit))
   (s/replace-first uri "/" "")))

(defn lit-body [db path]
  (->> (get-in db [:sources/lit path :file/content])
       (str "<script>" (slurp "resources/js/live-reload.js") "</script>")
       (typesetting/md-file->html db)))

(defn fallback-body [{:sources/keys [lit] :as db}]
  (->> (for [path (sort (map basename (keys lit)))]
         [:li [:a {:href (str "/" path)} path]])
       (into [:ul])
       (conj [:div [:h2 "Litt lurer på om du ser etter en av disse?"]])
       (typesetting/page db)))

(def sse-handshake
  {:status 200,
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"}})

(defn live-reload [db {:keys [uri] :as req}]
  (when-let [lit-path (resolve-lit db (subs uri 4))]
    (->> {:on-open (fn [ch] (swap! clients conj [lit-path ch])
                     (server/send! ch sse-handshake false))
          :on-close (fn [ch _] (swap! clients disj [lit-path ch]))}
         (server/as-channel req))))

(defn success [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn app [db {:keys [uri] :as req}]
  (cond
    (s/starts-with? uri "/sse")
    (live-reload db req)

    (resolve-lit db uri)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (lit-body db (resolve-lit db uri))}

    :else
    {:status 404
     :headers {"Content-Type" "text/html"}
     :body (fallback-body db)}))

(defonce server
  (server/run-server (fn [req] (app @db/db req)) {:port 8080}))
