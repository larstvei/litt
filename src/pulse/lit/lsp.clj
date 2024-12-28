(ns pulse.lit.lsp
  (:require
   [clojure.string :as s]
   [cheshire.core :as json]))

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
    "initialize" [{:capabilities {}}]
    "shutdown" [nil]
    "exit" (System/exit 0)
    nil))

(defn wrap [{:keys [id]} result]
  {:id id, :jsonrpc "2.0", :result result})

(defn handle-message [message]
  (->> (prepare-response message)
       (map (partial wrap message))
       (run! send-message)))

(defn lsp-loop [config]
  (->> (java.io.BufferedReader. *in*)
       (read-message)
       (handle-message)
       (while true)))
