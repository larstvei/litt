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

(defn initialize-response [{:keys [id]}]
  {:id id :jsonrpc "2.0" :result {:capabilities {}}})

(defn handle-message [{:keys [method] :as message}]
  (case method
    "initialize" [(initialize-response message)]
    nil))

(defn lsp-loop [config]
  (->> (java.io.BufferedReader. *in*)
       (read-message)
       (handle-message)
       (run! send-message)
       (while true)))
