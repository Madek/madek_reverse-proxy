; Copyright (C) 2013, 2014, 2015 Dr. Thomas Schank  (DrTom@schank.ch, Thomas.Schank@algocon.ch)
; See the "LICENSE.txt" file provided with this software.

(ns cider-ci.reverse-proxy.proxy
  (:require 
    [clj-http.client :as http-client]
    [clj-logging-config.log4j :as logging-config]
    [clojure.core.incubator :refer [dissoc-in]]
    [clojure.tools.logging :as logging]
    [ring.util.request :refer [request-url]]
    ))

(defn clj-http-client-handler [request]
  (try (http-client/request request)
       (catch Exception e
         (logging/warn {:error e})
         {:status 500
          :body (str "ERROR when requesting URL: " 
                     (:url request) " EXCEPTION: " e)})))

(defn read-body [req]
  (-> req
      (assoc :body (org.apache.commons.io.IOUtils/toByteArray 
                     (:body req)))
      (dissoc-in [:headers "content-length"])))

(defn prepare-request [req]
  (-> req
      (select-keys [:body :headers])
      read-body
      (conj {:as :stream 
             :decode-cookies false
             :decompress-body false
             :follow-redirects false
             :method (:request-method req)
             :proxy-request req
             :proxy-url  (request-url req)
             :throw-exceptions false })))

(defn create-handler [dispatcher]
  (fn [request]
    (-> request
        prepare-request
        (dispatcher clj-http-client-handler))))

;### Debug ####################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
