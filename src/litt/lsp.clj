(ns litt.lsp
  (:require
   [cheshire.core :as json]
   [litt.db :as db]
   [litt.definitions :as defs]))

(defn read-message [rdr]
  (drop-while (complement empty?) (line-seq rdr))
  (json/parse-stream rdr keyword))

(defn send-message [message]
  (let [json (json/generate-string message)
        length (count (.getBytes json))]
    (printf "Content-Length: %d\r\n\r\n%s" length json)
    (flush)))

(defn prepare-response [{:keys [method]}]
  (case method
    "initialize"
    [{:capabilities
      {:completionProvider
       {:triggerCharacters ["`"]}}}]

    "textDocument/completion"
    [{:isIncomplete false
      :items (for [[def info] (:lit/definitions @db/db)
                   :let [label (defs/definition->str def)]]
               {:label label :insertText (str label "`{=ref-def}")})}]

    "shutdown"
    [nil]

    "exit"
    (System/exit 0)

    nil))

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
