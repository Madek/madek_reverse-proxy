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
    [cider-ci.utils.routing :as routing]
    [cider-ci.utils.with :as with]
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [compojure.core :as cpj]
    [compojure.handler :as cpj.handler]
    [ring.util.response :refer [file-response]]
    ))


;##############################################################################

(defn format-method [request]
  (clojure.string/upper-case (name (:request-method request))))

(defn log-fileserver-request [request prefix path]
  (logging/info (format-method request) prefix  path))


;##############################################################################

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
      (let [url (str (assoc proxy-url :port port))
            res (handler (assoc request :url url)) ]
        (logging/info (str (format-method (:proxy-request request)) 
                           " " proxy-url " -> " url " " (:status res)))
        res )
      (let [msg (str "CIDER-CI_REVERSE-PROXY no target for " proxy-url)]
        (logging/info msg)
        {:status 404 :body msg}))))


;##############################################################################

(defn docs-handler [request]
  (let [path (-> request :route-params :*)
        file-path (str "../documentation/" path )]
    (log-fileserver-request request "/cider-ci/docs/" path)
    (file-response file-path)))

(defn demo-project-handler [request]
  (let [path (-> request :route-params :*)
        file-path (str "../.git/modules/demo-project-bash/" path )]
    (log-fileserver-request request "/cider-ci/demo-project-bash/" path)
    (file-response file-path)))


;##############################################################################

(defn redirect-to-ui []
  (logging/info "REDIRECT to UI" )
  {:status 301 
   :headers {"Location" "/cider-ci/ui/"}})

(defn build-main-handler [proxy-handler]
  (routing/wrap-debug-logging 
    (cpj/routes
      (cpj/ANY "/cider-ci/docs/*" request docs-handler)
      (cpj/ANY "/cider-ci/demo-project-bash/*" request demo-project-handler)
      (cpj/ANY "/" [] (redirect-to-ui))
      (cpj/ANY "*" request proxy-handler))
    'cider-ci.reverse-proxy.main))

(defn initialize []
  (let [http-conf (-> (get-config) :reverse_proxy :http)
        proxy-handler (reverse-proxy/create-handler #'dispatcher)
        handler (build-main-handler proxy-handler) ]
    (http-server/start http-conf handler)))


;##############################################################################

(defn -main [& args]
  (with/logging 
    (config/initialize)
    (nrepl/initialize (-> (get-config) :reverse_proxy :nrepl))
    (initialize)))


;### Debug ####################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
