(ns ti.wtf.core
  (:require [clojure.string :as string]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as ring-mp-params]
            [ring.middleware.params :as ring-params]
            [rum.core :as rum]))

(def BASE_URL "http://ti.wtf")

(def db (atom []))

(def a-z-range (range 97 (+ 97 26)))
(def A-Z-range (range 65 (+ 65 26)))

(def base62-digits
  (concat
   (map char a-z-range)
   (map char A-Z-range)
   (map str (range 0 10))))

(def sample-url "https://example.org/some/very/long/url")

(defn id->shorthand [id]
  (loop [res  ""
         rest id]
    (if (= rest 0)
      (if (string/blank? res)
        (str (first base62-digits))
        res)
      (recur
       (str (->> base62-digits
                 count
                 (mod rest)
                 (nth base62-digits))
            res)
       (quot rest (count base62-digits))))))


(rum/defc root-comp []
  [:div
   {:style
    {:font-family "monospace"}}
   [:style
    ":focus {
  outline: none;
}"]
   [:pre
    (format
     "The insaniest WTF.

Share your WTF with a short url:

    curl -X POST --data '%s' ti.wtf

" sample-url)]
   [:form
    [:input {:type        "text"
             :name        "shorten"
             :placeholder sample-url
             :autofocus   true
             :size        70
             :style       {:border        "none"
                           :padding-left  "0.25rem"
                           :border-bottom "1px dashed"}}]

    [:input {:type  "submit"
             :style {:border       "1px"
                     :border-style "dashed"
                     :margin-left  "0.5rem"
                     :background   "none"}}]]])

(defn root-form [request]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    (rum/render-html (root-comp))})

(defn generate-link [db url]
  {:ti.wtf/shorten-url  "shorten here"
   :ti.wtf/original-url "here"})

(defn create-shorten-url [{:keys [form-params] :as request}]
  (clojure.pprint/pprint form-params)
  {:status  200
   :body    "http://ti.wtf/u/test"})

(def router
  (ring/router
   [["/" {:get
          {:handler root-form}
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
