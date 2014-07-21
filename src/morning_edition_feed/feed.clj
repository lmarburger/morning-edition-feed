(ns morning-edition-feed.feed
  (:require [net.cgrand.enlive-html :as html])
  (:use [morning-edition-feed.scrape :only [fetch-latest-stories]]))

(def ^:dynamic *date-sel* [:rss :> :channel :> :pubDate])
(def ^:dynamic *story-sel* [:rss :> :channel :> :item])

(html/defsnippet date-model "morning_edition_feed/feed.xml" *date-sel*
  [date]
  [:pubDate] (html/content date))

(html/defsnippet story-model "morning_edition_feed/feed.xml" *story-sel*
  [{id        :id
    headline  :headline
    story-url :story-url
    audio-url :audio-url}]
  [:guid]      (html/content id)
  [:title]     (html/content headline)
  [:linkx]     (html/content story-url)
  [:enclosure] (html/set-attr :url audio-url))

(html/deftemplate podcast "morning_edition_feed/feed.xml"
  [date stories]
  [:pubDate] (html/substitute (date-model date))
  [:item]    (html/substitute (map story-model stories)))

(defn podcast-feed []
  (let [{date :date stories :stories} (fetch-latest-stories)]
    (podcast date stories)))
