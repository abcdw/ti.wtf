(ns ti.wtf.core
  (:require [clojure.string :as string]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as ring-mp-params]
            [ring.middleware.params :as ring-params]))

(def our-digits
  (concat
   (map char (range 97 (+ 97 26)))
   (map char (range 65 (+ 65 26)))
   (map str (range 0 10))))

(defn- to-our-digits [res rest]
  (if (= rest 0)
    (if (string/blank? res)
      (str (first our-digits))
      res)
    (to-our-digits
     (str (nth our-digits (mod rest (count our-digits)))
          res)
     (quot rest (count our-digits)))))

(defn id->shorthand [id]
  (to-our-digits "" id))

(defn root-html [request]
  {:status 200
   :body   "ok"})

(defn generate-link [db url]
  {:ti.wtf/shorten-url "shorten here"
   :ti.wtf/original-url "here"})

(defn create-shorten-url [{:keys [form-params] :as request}]
  (clojure.pprint/pprint form-params)
  {:status 201
   :body   "http://ti.wtf/u/test"})

(def router
  (ring/router
   [["/" {:get
          {:handler root-html}
          :post
          {:handler create-shorten-url}}]]))

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
