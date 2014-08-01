(defproject morning-edition-feed "1.0.0-SNAPSHOT"
  :description "Podcast feed for NRP's Morning Edition"
  :url "http://morning-edition-podcast.herokuapp.com"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [enlive "1.1.5"]
                 [compojure "1.1.8"]
                 [environ "0.5.0"]
                 [yeller-clojure-client "0.1.0-SNAPSHOT"]]
  :plugins [[lein-ring "0.8.11"]]
  :uberjar-name "morning-edition-feed-standalone.jar"
  :ring {:handler morning-edition-feed.core/app}
  :main morning-edition-feed.core
  :profiles {:uberjar {:main morning-edition-feed.core :aot :all}})
