; Copyright (C) 2013, 2014, 2015 Dr. Thomas Schank  (DrTom@schank.ch, Thomas.Schank@algocon.ch)
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(defproject cider-ci_reverse-proxy "1.0.2"
  :description "Cider-CI Reverse-Proxy"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE Version 3"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [
                 [drtom/logbug "1.0.0"]
                 [cider-ci/clj-utils "3.0.0-rc.1"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.apache.httpcomponents/httpclient "4.4.1"]
                 [org.clojure/core.incubator "0.1.3"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 ]
  :source-paths [ "src"]
  :profiles {:dev 
             {:dependencies [[midje "1.6.3"]]
              :plugins [[lein-midje "3.1.1"]]
              :repositories [["tmp" {:url "http://maven-repo-tmp.drtom.ch" :snapshots false}]]
              :resource-paths ["./config" "./resources" "../config"]}
             :production
             {:resource-paths ["/etc/cider-ci" "../config" "./config" "./resources"]}} 
  :aot [cider-ci.reverse-proxy.main] 
  :main cider-ci.reverse-proxy.main 
  :jvm-opts ["-Xmx64m"]
  :repl-options {:timeout  120000}
  )
