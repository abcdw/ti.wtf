(ns ti.wtf.core
  (:require [clojure.string :as string]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as ring-mp-params]
            [ring.middleware.params :as ring-params]
            [rum.core :as rum]))

;; (def config {:base-url "https://ti.wtf"
;;              :protocol "https"
;;              :domain   "ti.wtf"})

(def config {:base-url "http://localhost:3000"
             :protocol "http"
             :domain   "localhost:3000"})

(def db (atom []))

(def a-z-range (range 97 (+ 97 26)))
(def A-Z-range (range 65 (+ 65 26)))

(def base62-digits
  (concat
   (map char a-z-range)
   (map char A-Z-range)
   (map str (range 0 10))))

(def sample-url "https://example.org/some/very/long/url")

(def styles "
* { font-family: monospace; }
:focus { outline: none; }
iframe { overflow: hidden; }
a { color: inherit; }

code {
  margin-left: 2rem;
}
")

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

(rum/defc form-comp []
  [:div
    [:form
     {:target "shorten-url"
      :style  {:margin "0"}}
     [:input {:type        "text"
              :name        "shorten"
              :placeholder sample-url
              :autofocus   true
              :size        70
              :style       {:border        "none"
                            :padding-left  "0.25rem"
                            :border-bottom "1px dashed"}}]
     [:input {:type "hidden"
              :name "html"}]
     [:input {:type  "submit"
              :style {:border       "1px dashed"
                      :margin-left  "0.5rem"
                      :background   "none"}}]]
   [:iframe {:name      "shorten-url"
             :scrolling "no"
             :style     {:border "none"
                         :margin "0.25rem"
                         :width  "100%"
                         :height "1.1rem"}}]])

(rum/defc root-comp []
  [:div
   {:style
    {:margin "1rem"}}
   [:style styles]
   [:pre    "# This is wtf.\n\nDon't know what the f*ck is this?\nThis is url shortener of course.\n\n\n\n"]

   [:pre "## Using via form\n\nJust paste you url below and get a shorter one or at least better one."]
    (form-comp)

   [:pre "\n\n\n"]
   [:pre
    "## Using via cli\n
Create a short url with curl:
"
    [:code
     (format
      "curl -X POST --data '%s' ti.wtf"
      sample-url)]
     "\n\n\n"
    ]



   [:pre "## Source code\n\nThe source code available at "
    [:a {:href   "https://github.com/abcdw/ti.wtf"
         :target "_blank"} "abcdw/ti.wtf"]]
   ])

(rum/defc short-url-comp [url]
  [:body
   {:style
    {:margin 0}}
   [:a {:href url
        :target "_blank"
        :style {:margin "0" :color "inherit" :font-family "monospace"}}
     url]])

(defn get-shorten-url [params]
  (let [short-url (str (:base-url config) "/test")]
    {:status 200
     :body
     (if (contains? params "html")
       (rum/render-html (short-url-comp short-url))
       short-url)}))

(defn handle-root-post [{:keys [form-params] :as request}]
  (get-shorten-url form-params))

(defn handle-root-get [{:keys [form-params query-params] :as request}]
  (if (contains? query-params "shorten")
    (get-shorten-url query-params)
    {:status  200
     :headers {"content-type" "text/html"}
     :body    (rum/render-html (root-comp))}))

(defn generate-link [db url]
  {:ti.wtf/shorten-url  "shorten here"
   :ti.wtf/original-url "here"})

(defn handle-shorthand-get [request]
  {:headers {"location" "https://example.org/test/url"}
   :status  308})

(def router
  (ring/router
   [["/"
     {:get
      {:handler handle-root-get}
      :post
      {:handler handle-root-post}}]
    ["/:shorthand"
     {:get
      {:handler handle-shorthand-get}}]]
   ))

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
