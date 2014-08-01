(ns morning-edition-feed.core
  (:require [net.cgrand.enlive-html :as html]
            [yeller-clojure-client.ring :as yeller])
  (:use [morning-edition-feed.utils :only [render-to-response]]
        [morning-edition-feed.api :only [fetch-latest-program]]
        [net.cgrand.moustache :only [app]]
        [environ.core :refer [env]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.reload :only [wrap-reload]]))

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

(defn run-server* [app & {:keys [port] :or {port (Integer. (or (env :port)
                                                               5000))}}]
  (let [nses (if-let [m (meta app)]
               [(-> (:ns (meta app)) str symbol)]
               [])]
    (println "run-server*" nses)
    (run-jetty
     (-> app
         (wrap-reload nses)
         (yeller/wrap-ring {:token *yeller-token*}))
     {:port port :join? false})))

(defmacro run-server [app]
  `(run-server* (var ~app)))

(def routes
  (app
   [""] (fn [req] (render-to-response (podcast-feed)))))

(defn -main [& args]
  (run-server routes))
