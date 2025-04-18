(ns litt.lsp
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [litt.db :as db]
   [litt.src :as src]
   [clojure.string :as s]))

(defn read-message [rdr]
  (let [header (take-while (complement empty?) (line-seq rdr))
        [[_ n]] (keep #(re-find #"Content-Length: (\d+)" %) header)
        num-bytes (parse-long n)
        buf (char-array num-bytes)]
    (.readLine rdr)
    (.read rdr buf 0 num-bytes)
    (json/parse-string (String. buf) keyword)))

(defn send-message [message]
  (let [json (json/generate-string message)
        length (count (.getBytes json))]
    (printf "Content-Length: %d\r\n\r\n%s" length json)
    (flush)))

(defn resolve-uri [uri]
  (-> (fs/absolutize (fs/path "."))
      (fs/relativize (fs/path (io/as-url uri)))
      (str)))

(defn position->index [{:keys [line character]} s]
  (->> (s/split s #"\n" -1)
       (take line)
       (map (comp inc count))
       (reduce + character)))

(defn content-change-function [{:keys [range text]}]
  (fn [s]
    (let [start (position->index (:start range) s)
          end (position->index (:end range) s)]
      (str (subs s 0 start) text (subs s end)))))

(defmulti prepare-response (comp keyword :method))

(defmethod prepare-response :initialize [_]
  [{:capabilities
    {:completionProvider {:triggerCharacters ["`"]}
     :textDocumentSync 2}}])

(defmethod prepare-response :textDocument/completion [_]
  [{:isIncomplete true
    :items (for [[def info] (:lit/definitions @db/db)
                 :let [label (src/definition->str def)]]
             {:label label :insertText (str label "`{=litt}")})}])

(defmethod prepare-response :textDocument/didChange [message]
  (let [uri (get-in message [:params :textDocument :uri])
        changes (get-in message [:params :contentChanges])]
    (db/update-content!
     [:sources/lit (resolve-uri uri)]
     (apply comp (map content-change-function changes))))
  [])

(defmethod prepare-response :shutdown [_]
  [nil])

(defmethod prepare-response :exit [_]
  (System/exit 0))

(defmethod prepare-response :default [_]
  nil)

(defn wrap [{:keys [id]} result]
  {:id id, :jsonrpc "2.0", :result result})

(defn handle-message [message]
  (->> (prepare-response message)
       (map (partial wrap message))
       (run! send-message)))

(defn lsp-loop []
  (->> (java.io.BufferedReader. *in*)
       (read-message)
       (handle-message)
       (while true)))
