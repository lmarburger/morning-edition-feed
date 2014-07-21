(ns morning-edition-feed.scrape
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]))

(def ^:dynamic *base-url* "http://www.npr.org/programs/morning-edition/")
(def ^:dynamic *fake-stories*
  [{:headline "Story 1"
    :story-url "http://google.com"
    :id "abc123"
    :audio-url "http://nrp.org/story-1.mp3"}
   {:headline "Second Story"
    :story-url "http://bing.com"
    :id "def456"
    :audio-url "http://nrp.org/story-2.mp3"}
   {:headline "Finale"
    :story-url "http://yahoo.com"
    :id "ghi789"
    :audio-url "http://nrp.org/story-3.mp3"}])

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn extract-story [node]
  (let [headline  (html/text (first (html/select [node] [:h1 :a])))
        id        (get-in (first (html/select [node]
                                              [[:input (html/attr= :type "hidden")]]))
                          [:attrs :id])
        audio-url (get-in (first (html/select [node] [:.download :a]))
                          [:attrs :href])
        story-url (get-in (first (html/select [node] [:h1 :a]))
                          [:attrs :href])]
    {:id id
     :headline headline
     :story-url story-url
     :audio-url (string/replace audio-url #"\?dl=1" "")}))

(defn print-story [date story]
  (println (str " - " date " - " (:headline story))))

(defn fake-fetch-latest-stories []
  {:date "July 22, 2014"
   :stories *fake-stories*})

(defn fetch-latest-stories []
  (let [raw-body (fetch-url *base-url*)
        date    (html/text (first (html/select raw-body [:time :span])))
        stories (html/select raw-body
                             [[:.story (complement (html/attr? :id))]])]
    {:date date :stories (map extract-story stories)}))
