(ns morning-edition-feed.core
  (:require [net.cgrand.enlive-html :as html])
  (:use [morning-edition-feed.utils :only [render-to-response]]
        [morning-edition-feed.scrape :only [fetch-latest-stories]]
        [net.cgrand.moustache :only [app]]
        [environ.core :refer [env]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.reload :only [wrap-reload]]
        [ring.middleware.stacktrace :only [wrap-stacktrace]])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

;;; TODO: Add last-modified response header
;;; TODO: Extract total time
;;; TODO: Extract episode image

(defn ^SimpleDateFormat make-http-format
  "Formats or parses dates into HTTP date format (RFC 822/1123)."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "America/New_York"))))

(defn podcast-date-format [date]
  (.format (make-http-format) date))

(def ^:dynamic *story-sel* [:rss :> :channel :> :item])

(html/defsnippet story-model "morning_edition_feed/feed.xml" *story-sel*
  [{:keys [id story-date headline story-url audio-url]}]
  [:guid]      (html/content id)
  [:pubDate]   (html/content (podcast-date-format story-date))
  [:title]     (html/content headline)
  [:linkx]     (html/content story-url)
  [:enclosure] (html/set-attr :url audio-url))

(html/deftemplate podcast "morning_edition_feed/feed.xml"
  [date stories]
  [:pubDate] (html/content (podcast-date-format date))
  [:item]    (html/substitute (map story-model stories)))

(defn podcast-feed []
  (let [{:keys [date stories]} (fetch-latest-stories)]
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
         (wrap-stacktrace))
     {:port port :join? false})))

(defmacro run-server [app]
  `(run-server* (var ~app)))

(def routes
  (app
   [""] (fn [req] (render-to-response (podcast-feed)))))

(defn -main [& args]
  (run-server routes))
