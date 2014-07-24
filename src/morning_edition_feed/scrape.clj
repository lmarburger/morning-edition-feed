(ns morning-edition-feed.scrape
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

;;; TODO: Extract total time
;;; TODO: Extract episode image

(def ^:dynamic *base-url* "http://www.npr.org/programs/morning-edition/")
(def ^:dynamic *fake-stories*
  [{:headline "Story 1"
    :id "abc123"
    :story-url "http://google.com"
    :audio-url "http://nrp.org/story-1.mp3"
    :segment-num "1"}
   {:headline "Second Story"
    :id "def456"
    :story-url "http://bing.com"
    :audio-url "http://nrp.org/story-2.mp3"
    :segment-num "2"}
   {:headline "Finale"
    :id "ghi789"
    :story-url "http://yahoo.com"
    :audio-url "http://nrp.org/story-3.mp3"
    :segment-num "3"}])

(defn- ^SimpleDateFormat make-simple-format
  "Parse simple date strings."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "yyyy-MM-dd" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

;; (defn- ^SimpleDateFormat make-http-format
;;   "Formats or parses dates into HTTP date format (RFC 822/1123)."
;;   []
;;   ;; SimpleDateFormat is not threadsafe, so return a new instance each time
;;   (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
;;     (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn parse-simple-date [str]
  (.parse (make-simple-format) str))

(defn- fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn- extract-story [date node]
  (let [headline (-> (html/select [node] [:h1 :a])
                     first
                     html/text)
        id (-> (html/select [node] [[:input (html/attr= :type "hidden")]])
               first
               (get-in [:attrs :id]))
        audio-url (-> (html/select [node] [:.download :a])
                      first
                      (get-in [:attrs :href]))
        story-url (-> (html/select [node] [:h1 :a])
                      first
                      (get-in [:attrs :href]))
        segment-num (-> (html/select [node] [:.segment-num])
                        first
                        html/text
                        Integer.)
        story-date (-> (.getTime date)
                       (+ (* segment-num 1000))
                       Date.)]
    {:id id
     :headline headline
     :story-url story-url
     :audio-url (string/replace audio-url #"\?dl=1" "")
     :segment-num segment-num
     :story-date story-date}))

(defn fake-fetch-latest-stories []
  {:date "July 22, 2014"
   :stories *fake-stories*})

(defn fetch-latest-stories []
  (let [raw-body (fetch-url *base-url*)
        date (parse-simple-date
              (get-in (first (html/select raw-body
                                          [[:meta (html/attr= :name "date")]]))
                      [:attrs :content]))
        stories (html/select raw-body
                             [[:.story (complement (html/attr? :id))]])]
    {:date date :stories (map (partial extract-story date) stories)}))

(defn print-story [date story]
  (println (str " - " date " - " (:headline story))))
