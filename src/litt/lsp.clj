(ns litt.lsp
  (:require
   [cheshire.core :as json]
   [litt.db :as db]
   [litt.definitions :as defs]))

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

(defmulti prepare-response (comp keyword :method))

(defmethod prepare-response :initialize [_]
  [{:capabilities
    {:completionProvider {:triggerCharacters ["`"]}
     :textDocumentSync 2}}])

(defmethod prepare-response :textDocument/completion [_]
  [{:isIncomplete true
    :items (for [[def info] (:lit/definitions @db/db)
                 :let [label (defs/definition->str def)]]
             {:label label :insertText (str label "`{=litt}")})}])

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
