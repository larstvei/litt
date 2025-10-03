(ns litt.serve
  (:require
   [babashka.fs :as fs]
   [clojure.string :as s]
   [hiccup2.core :as hiccup]
   [litt.db :as db]
   [litt.typesetting :as typesetting]
   [org.httpkit.server :as server]))

(def live-reload-js (slurp "resources/js/live-reload.js"))
(defonce server (atom nil))
(defonce clients (atom #{}))

(def sse-handshake
  {:status 200,
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"}})

(def html-success
  {:status 200
   :headers {"Content-Type" "text/html"}})

(def html-not-found
  {:status 404
   :headers {"Content-Type" "text/html"}})

(defn basename [path]
  (first (fs/split-ext path)))

(defn resolve-lit [{:sources/keys [lit]} uri]
  ((zipmap (map basename (keys lit)) (keys lit))
   (s/replace-first uri "/" "")))

(defn lit-body [db path]
  (->> (typesetting/md-file->html db path)
       (list [:script (hiccup/raw live-reload-js)])
       (typesetting/page db)))

(defn fallback-body [{:sources/keys [lit] :as db}]
  (->> (for [path (sort (map basename (keys lit)))]
         [:li [:a {:href (str "/" path)} path]])
       (into [:ul])
       (conj [:div [:h2 "Litt lurer på om du ser etter en av disse?"]])
       (typesetting/page db)))

(defn live-reload [db {:keys [uri] :as req}]
  (when-let [lit-path (resolve-lit db (subs uri 4))]
    (->> {:on-open (fn [ch] (swap! clients conj [lit-path ch])
                     (server/send! ch sse-handshake false))
          :on-close (fn [ch _] (swap! clients disj [lit-path ch]))}
         (server/as-channel req))))

(defn request-handler [db {:keys [uri] :as req}]
  (cond
    (s/starts-with? uri "/sse")
    (live-reload db req)

    (resolve-lit db uri)
    (assoc html-success :body (lit-body db (resolve-lit db uri)))

    :else
    (assoc html-not-found :body (fallback-body db))))

(defn start-server! []
  (let [handler (fn [req] (request-handler @db/db req))]
    (when (fn? @server) (@server))
    (add-watch
     db/db
     :live-reload
     (fn [_ _ _ db]
       (doseq [[lit-path ch] @clients
               :let [html (hiccup/html (typesetting/md-file->html db lit-path))
                     encoded (s/replace (str "data: " html) "\n" "\ndata: ")]]
         (server/send! ch (str encoded "\n\n") false))))
    (reset! server (server/run-server handler {:port 8080}))))
