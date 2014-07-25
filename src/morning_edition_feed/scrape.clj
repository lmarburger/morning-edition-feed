(ns morning-edition-feed.scrape
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string])
  (:import (java.util Date Locale TimeZone)
           java.text.SimpleDateFormat))

(def ^:dynamic *base-url* "http://www.npr.org/programs/morning-edition/")

(defn ^SimpleDateFormat make-simple-format
  "Parse simple date strings."
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "yyyy-MM-dd" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "America/New_York"))))

(defn parse-simple-date [str]
  (.parse (make-simple-format) str))

(defn broadcast-time
  "Morning Edition is broadcast at 5am ET."
  [date]
  (-> (.getTime date)
      (+ (* 5 60 60 1000))
      (Date.)))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn extract-story [date node]
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
    (if audio-url
      {:id id
       :headline headline
       :story-url story-url
       :audio-url (string/replace audio-url #"\?dl=1" "")
       :segment-num segment-num
       :story-date story-date})))

(defn complete? [html]
  (when-not (first (html/select html [:.full-show :.unavailable]))
    html))

(defn previous-stories-url [html]
  (-> (html/select html [:.program-nav :.prev :a])
      first
      (get-in [:attrs :href])))

(defn latest-complete-story
  ([] (latest-complete-story *base-url*))
  ([url]
     (let [raw-body (fetch-url url)]
       (if (complete? raw-body)
         raw-body
         (recur (previous-stories-url raw-body))))))

(defn fetch-latest-stories []
  (let [raw-body (latest-complete-story)
        date (-> (html/select raw-body [[:meta (html/attr= :name "date")]])
                 first
                 (get-in [:attrs :content])
                 parse-simple-date
                 broadcast-time)
        stories (html/select raw-body
                             [[:.story (complement (html/attr? :id))]])]
    {:date date :stories (map (partial extract-story date) stories)}))

(defn print-story [date story]
  (println (str " - " date " - " (:headline story))))
