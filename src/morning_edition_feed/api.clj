(ns morning-edition-feed.api
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml])
  (:use [environ.core :refer [env]])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

(def ^:dynamic *api-key* (env :npr-token))
(def ^:dynamic *api-url* (str "http://api.npr.org/query?id=3&dateType=story&numResults=42&apiKey=" *api-key*))

(defn test-api-root []
  (-> "out.xml"
      io/resource
      io/file))

(defn ^SimpleDateFormat make-http-format
  "Formats or parses dates into HTTP date format (RFC 822/1123)."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "America/New_York"))))

(defn format-story-date [date]
  (.format (make-http-format) date))

(defn parse-story-date [date-string]
  (.parse (make-http-format) date-string))

(defn hack-story-date [date segment-num]
  (-> (.getTime date)
      (+ (* segment-num 1000))
      (Date.)))

(defn story->map
  [story]
  (let [id          (zip-xml/attr   story :id)
        title       (zip-xml/xml1-> story :title   zip-xml/text)
        description (zip-xml/xml1-> story :teasure zip-xml/text)
        date        (parse-story-date
                     (zip-xml/xml1-> story
                                     :show
                                     :showDate
                                     zip-xml/text))
        story-url   (zip-xml/xml1-> story
                                    :link
                                    (zip-xml/attr= :type "html")
                                    zip-xml/text)
        audio-url   (zip-xml/xml1-> story
                                    :audio
                                    :format
                                    :mp4
                                    zip-xml/text)
        segment-num (Integer/valueOf (zip-xml/xml1-> story
                                                     :show
                                                     :segNum
                                                     zip-xml/text))]
    {:id id
     :title title
     :description description
     :date (format-story-date (hack-story-date date segment-num))
     :story-url story-url
     :audio-url audio-url
     :segment-num segment-num}))

(defn api->map
  [date root]
  (let [title       (zip-xml/xml1-> root :list :title zip-xml/text)
        description (zip-xml/xml1-> root :list :miniTeaser zip-xml/text)
        stories     (mapv story->map (zip-xml/xml-> root :list :story))]
    {:title title
     :description description
     :date (format-story-date date)
     :stories (sort-by :segment-num stories)}))

(defn fetch-url [url]
  (-> url
      xml/parse
      zip/xml-zip))

(defn ^SimpleDateFormat make-simple-format
  "Parse simple date strings."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "yyyy-MM-dd" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "America/New_York"))))

(defn format-simple-date [date]
  (.format (make-simple-format) date))

(defn fetch-latest-program
  ([] (fetch-latest-program (Date.)))
  ([date]
     (let [api-url (str *api-url* "&date=" (format-simple-date date))]
       (api->map date (fetch-url api-url)))))
