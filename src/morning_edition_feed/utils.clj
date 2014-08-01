(ns morning-edition-feed.utils
  (:require [net.cgrand.enlive-html :as html]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn render [t]
  (apply str t))

(defn replace-hack-tags
  "To circumvent enlive wanting to auto-close <link> tags, they're named <linkx>
   in the template and then replaced after rendering."
  [response]
  (string/replace response #"linkx>" "link>"))

(defn add-xml-declaration
  "Enlive strips the <?xml?> declaration from the template. Forcibly replace it."
  [response]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" response))

(defn add-xml-namespaces
  "Enlive strips xml namespaces from the template. Forcibly replace it."
  [response]
  (string/replace response
                  "<rss version=\"2.0\">"
                  "<rss xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\" version=\"2.0\">"))

(def render-to-response
  (comp add-xml-declaration
        add-xml-namespaces
        replace-hack-tags
        render))

(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (html/substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))
