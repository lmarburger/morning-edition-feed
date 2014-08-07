(ns morning-edition-feed.core
  (:require [net.cgrand.enlive-html :as html]
            [yeller-clojure-client.ring :as yeller]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [morning-edition-feed.utils :refer [render-to-response]]
            [morning-edition-feed.api :refer [fetch-latest-program]]))

(def ^:dynamic *story-sel* [:rss :> :channel :> :item])
(def ^:dynamic *npr-token* (env :npr-token))
(def ^:dynamic *yeller-options*
  (when-let [token (env :yeller-token)]
    {:token token}))

(html/defsnippet story-model "morning_edition_feed/feed.xml" *story-sel*
  [{:keys [id title description date duration story-url audio-url image-url]}]
  [:guid] (html/content id)
  [:pubDate] (html/content date)
  [:title] (html/content title)
  [:itunes:summary] (html/content description)
  [:itunes:image] (if image-url
                    (html/set-attr :href image-url)
                    (html/substitute nil))
  [:itunes:duration] (html/content duration)
  [:linkx] (html/content story-url)
  [:enclosure] (html/set-attr :url audio-url))

(html/deftemplate podcast "morning_edition_feed/feed.xml"
  [date stories]
  [:pubDate] (html/content date)
  [:item]    (html/substitute (map story-model stories)))

(defn podcast-feed []
  (let [{:keys [date stories]} (fetch-latest-program *npr-token*)]
    (podcast date stories)))

(defroutes app-routes
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/xml; charset=utf-8"}
        :body (render-to-response (podcast-feed))}))

(defn wrap-yeller [app]
  (if-let [options *yeller-options*]
    (yeller/wrap-ring app options)
    app))

(def app
  (let [environment (or (env :env) "development")]
    (-> app-routes
        wrap-yeller
        handler/site)))

(defn -main [& args]
  (let [port (Integer. (or (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
