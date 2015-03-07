; Copyright (C) 2013, 2014, 2015 Dr. Thomas Schank  (DrTom@schank.ch, Thomas.Schank@algocon.ch)
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns cider-ci.reverse-proxy.main
  (:require 
    [cemerick.url :as url-util]
    [cider-ci.reverse-proxy.proxy :as reverse-proxy]
    [cider-ci.utils.config :as config :refer [get-config]]
    [cider-ci.utils.debug :as debug]
    [cider-ci.utils.http-server :as http-server]
    [cider-ci.utils.map :refer [deep-merge]]
    [cider-ci.utils.nrepl :as nrepl]
    [cider-ci.utils.with :as with]
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    ))

(defn get-patterns-and-ports []
  (->> (get-config)
       :services
       (map (fn [[k v]] v))
       (map :http)
       (map (fn [c] [(-> (str (:context c) (:sub_context c)) 
                         java.util.regex.Pattern/quote
                         (#(str "(?i)^" % ".*"))
                         re-pattern)
                     (:port c)]))))

(defn find-port-for-path [path]
  (->> (get-patterns-and-ports)
       (filter (fn [pp] 
                 (re-matches (first pp) path)))
       first 
       second))

(defn dispatcher [request,handler]
  (let [proxy-url (url-util/url (:proxy-url request))
        path (:path proxy-url)]
    (if-let [port (find-port-for-path path)]
      (let [url (str (assoc proxy-url :port port))]
        (logging/info (str proxy-url " -> " url))
        (handler (assoc request :url url)))
      (let [msg (str "CIDER-CI_REVERSE-PROXY no target for " proxy-url)]
        (logging/info msg)
        {:status 404 :body msg}))))

(defn initialize []
  (let [http-conf (-> (get-config) :reverse_proxy :http)
        handler (reverse-proxy/create-handler #'dispatcher)]
    (http-server/start http-conf handler)))

(defn -main [& args]
  (with/logging 
    (config/initialize ["../config/config_default.yml" "./config/config_default.yml" "./config/config.yml"])
    (nrepl/initialize (-> (get-config) :reverse_proxy :nrepl))
    (initialize)))

;### Debug ####################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
