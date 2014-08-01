(defproject morning-edition-feed "1.0.0-SNAPSHOT"
  :description "Podcast feed for NRP's Morning Edition"
  :url "http://morning-edition-podcast.herokuapp.com"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [ring "1.3.0"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [enlive "1.1.5"]
                 [net.cgrand/moustache "1.1.0"]
                 [environ "0.5.0"]
                 [yeller-clojure-client "0.1.0-SNAPSHOT"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.8.11"]
            [environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "morning-edition-feed-standalone.jar"
  :ring {:handler morning-edition-feed.core/routes}
  :main morning-edition-feed.core
  :profiles {:production {:env {:production true}}
             :uberjar {:main morning-edition-feed.core :aot :all}})
