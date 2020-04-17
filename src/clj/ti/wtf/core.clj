(ns ti.wtf.core
  (:require [hiccup.core :as hiccup]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as ring-mp-params]
            [ring.middleware.params :as ring-params]))

(def router
  (ring/router
   [["/" {:get
          {:handler identity}}]]))

(def app
  (ring/ring-handler
   router

   (fn [req]
     {:status 404
      :body   ":/"})

   {:middleware [ring-params/wrap-params
                 ring-mp-params/wrap-multipart-params]}))

(defn -main []
  (jetty/run-jetty #'app {:port 3000 :join? false}))

(comment
  (def server (-main))
  (.stop server)
  (app {:request-method :get :uri "/api"})
  )
