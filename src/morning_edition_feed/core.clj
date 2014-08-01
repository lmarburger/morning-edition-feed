(ns morning-edition-feed.core
  (:require [net.cgrand.enlive-html :as html]
            [yeller-clojure-client.ring :as yeller]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as jetty])
  (:use [morning-edition-feed.utils :only [render-to-response]]
        [morning-edition-feed.api :only [fetch-latest-program]]
        [environ.core :refer [env]]))

(def ^:dynamic *yeller-token* "DnHV_chmlQUuD0rdReK5M5j4LYNxLBmotdgqouwe4wI")
(def ^:dynamic *story-sel* [:rss :> :channel :> :item])

(html/defsnippet story-model "morning_edition_feed/feed.xml" *story-sel*
  [{:keys [id title description date story-url audio-url]}]
  [:guid]      (html/content id)
  [:pubDate]   (html/content date)
  [:title]     (html/content title)
  [:linkx]     (html/content story-url)
  [:enclosure] (html/set-attr :url audio-url))

(html/deftemplate podcast "morning_edition_feed/feed.xml"
  [date stories]
  [:pubDate] (html/content date)
  [:item]    (html/substitute (map story-model stories)))

(defn podcast-feed []
  (let [{:keys [date stories]} (fetch-latest-program)]
    (podcast date stories)))

(defroutes app-routes
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/xml; charset=utf-8"}
        :body (render-to-response (podcast-feed))}))

(def app
  (let [environment (or (env :env) "development")]
    (-> app-routes
        (yeller/wrap-ring {:token *yeller-token*
                           :environment environment})
        handler/site)))

(defn -main [& args]
  (let [port (Integer. (or (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
