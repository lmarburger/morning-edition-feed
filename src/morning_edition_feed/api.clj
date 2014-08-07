(ns morning-edition-feed.api
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.string :as string])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

(def ^:dynamic *api-base-url* "http://api.npr.org/query?id=3&dateType=story&numResults=42&apiKey=")
(def ^:dynamic *mp3-base-url* "http://pd.npr.org/")

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

(defn stream-url->mp3-url
  "NPR doesn't offer up raw mp3 files only m4u files. Turns out the link to
   the mp3 is included in the streaming url."
  [stream-url]
  (str *mp3-base-url*
       (string/replace stream-url #"rtmp://.*:(.*)" "$1")))

(defn story->map
  [story]
  (let [id          (zip-xml/attr   story :id)
        title       (zip-xml/xml1-> story :title  zip-xml/text)
        description (zip-xml/xml1-> story :teaser zip-xml/text)
        date        (parse-story-date
                     (zip-xml/xml1-> story
                                     :show
                                     :showDate
                                     zip-xml/text))
        duration    (zip-xml/xml1-> story
                                    :audio
                                    :duration
                                    zip-xml/text)
        segment-num (Integer/valueOf (zip-xml/xml1-> story
                                                     :show
                                                     :segNum
                                                     zip-xml/text))
        story-url   (zip-xml/xml1-> story
                                    :link
                                    (zip-xml/attr= :type "html")
                                    zip-xml/text)
        stream-url  (zip-xml/xml1-> story
                                    :audio
                                    :format
                                    :mediastream
                                    zip-xml/text)
        image-url   (zip-xml/xml1-> story
                                    :image
                                    :crop
                                    (zip-xml/attr= :type "standard")
                                    (zip-xml/attr :src))]
    {:id id
     :title title
     :description description
     :date (format-story-date (hack-story-date date segment-num))
     :duration duration
     :story-url story-url
     :audio-url (stream-url->mp3-url stream-url)
     :segment-num segment-num
     :image-url image-url}))

(defn published-stories [root]
  (filter :audio-url
          (mapv story->map (zip-xml/xml-> root
                                          :list
                                          :story))))

(defn api->map
  [date root]
  (let [title       (zip-xml/xml1-> root :list :title zip-xml/text)
        description (zip-xml/xml1-> root :list :miniTeaser zip-xml/text)
        stories     (published-stories root)]
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
  ([token] (fetch-latest-program token (Date.)))
  ([token date]
     (let [api-url (str *api-base-url*
                        token
                        "&date="
                        (format-simple-date date))]
       (api->map date (fetch-url api-url)))))
