(ns morning-edition-feed.core
  (:use [morning-edition-feed.feed :only [podcast-feed]]
        [morning-edition-feed.utils :only [render-to-response]]
        [net.cgrand.moustache :only [app]]
        [environ.core :refer [env]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.reload :only [wrap-reload]]
        [ring.middleware.stacktrace :only [wrap-stacktrace]]))

;;; TODO: Add last-modified response header
;;; TODO: Extract total time
;;; TODO: Extract episode image

(def routes
  (app
   [""] (fn [req] (render-to-response (podcast-feed)))))

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

(defn -main [& args]
  (run-server routes))
