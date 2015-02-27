(defproject bailey "0.1.0-SNAPSHOT"
  :description "Event-based web application infrastructure"
  :url "https://github.com/manutter51/bailey"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.2"]
                 [barnum/barnum "0.2.0-SHAPSHOT"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})
