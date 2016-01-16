(defproject bailey "0.1.0-SNAPSHOT"
  :description "Event-based web application infrastructure"
  :url "https://github.com/manutter51/bailey"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [clout "2.1.2"]
                 [barnum/barnum "0.2.0-SHAPSHOT"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring/ring-mock "0.3.0"]]
                   :plugins [[lein-midje "3.1.3"]]}})
