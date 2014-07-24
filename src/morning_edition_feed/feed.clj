(ns morning-edition-feed.feed
  (:require [net.cgrand.enlive-html :as html])
  (:use [morning-edition-feed.scrape :only [fetch-latest-stories]])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

(defn- ^SimpleDateFormat make-http-format
  "Formats or parses dates into HTTP date format (RFC 822/1123)."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

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
