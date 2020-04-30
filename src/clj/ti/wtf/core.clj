(ns ti.wtf.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [honeysql.core :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as ring-mp-params]
            [ring.middleware.params :as ring-params]
            [rum.core :as rum]))

;; (def config {:base-url "https://ti.wtf"
;;              :protocol "https"
;;              :domain   "ti.wtf"})

(defn get-config []
  (-> (io/resource "config.edn")
      (aero/read-config)))

(def config (get-config))

(def a-z-range (range 97 (+ 97 26)))
(def A-Z-range (range 65 (+ 65 26)))

(def base62-digits
  (concat
   (map char a-z-range)
   (map char A-Z-range)
   (map str (range 0 10))))

(defn id->alias [id]
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

(def ds (jdbc/get-datasource (:db config)))

(defn init-db! []
  (let [ds (jdbc/get-datasource (dissoc (:db config) :dbname))]
    (jdbc/execute! ds ["CREATE DATABASE ti"])))

(def first-id (* 62 62))

(defn migrate! []
  (run!
   (fn [stmt] (jdbc/execute!
                ds [stmt]))
    [(str "CREATE SEQUENCE alias_seq START WITH " first-id ";")
     "CREATE TABLE alias (
id BIGINT PRIMARY KEY DEFAULT nextval('alias_seq'),
alias TEXT,
original_url TEXT);
"
     "CREATE INDEX idx_alias ON alias(alias);"
     "CREATE INDEX idx_original_url ON alias(original_url);"]))

(defn unmigrate! []
  (run!
   (fn [stmt] (jdbc/execute!
                ds [stmt]))
   ["DROP INDEX idx_alias;"
    "DROP INDEX idx_original_url;"
    "DROP TABLE alias;"
    "DROP SEQUENCE alias_seq;"]))

(comment
  (unmigrate!)
  (migrate!)

  (create-alias! "test.com")
  (get-alias-by-alias-field "baaa")

  (db-exec! {:select :* :from :alias-seq})
  (db-exec! {:select :* :from :alias})

  (db-exec! {:insert-into :alias
             :columns     [:original-url]
             :values      [["http://example.com"]]})
  )

(defn as-kebab-maps [rs opts]
  (let [kebab #(string/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts :qualifier-fn kebab :label-fn kebab))))

(defn db-exec! [query]
  (jdbc/execute!
   ds
   (if (string? query)
     [query]
     (-> query
         sql/build
         sql/format))
   {:return-keys true :builder-fn as-kebab-maps}))

(defn generate-alias-field [{:alias/keys [id] :as alias}]
  (assoc alias :alias/alias (id->alias id)))

(defn create-alias-placeholder! [url]
  (first
   (db-exec! {:insert-into :alias
              :columns     [:original-url]
              :values      [[url]]})))

(defn update-alias! [alias]
  (first
   (db-exec! {:update :alias
              :set    alias
              :where  [:= :id (:alias/id alias)]})))

(defn create-alias! [url]
  (->
   (create-alias-placeholder! url)
   generate-alias-field
   update-alias!))

(defn get-alias-by-alias-field [alias]
  (-> {:select :*
       :from   :alias
       :where  [:= :alias alias]}
      db-exec!
      first))

(defn get-alias-by-original-url [url]
  (-> {:select :*
       :from   :alias
       :where  [:= :original-url url]}
      db-exec!
      first))

(def sample-url "https://example.org/some/very/long/url")

(def styles "
* { color: #3E3E3E;}
:focus { outline: none; }
iframe { overflow: hidden; }
a { color: inherit; }

code {
  margin-left: 2rem;
  color: #161616;
}
")



(rum/defc form-comp []
  [:div
    [:form
     {:target "shorten-url"
      :style  {:margin "0"}}
     [:input {:type        "text"
              :name        "u"
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
  [:html
   [:head
    [:link {:rel  "icon"
            :href "data:,"}]]
   [:body
    [:div
     {:style
      {:margin "1rem"}}
     [:style styles]
     [:pre    "# ti.wtf\n\nThis is url shortener of course.\n\n\n"]

     [:pre "## Using via form\n\nJust paste you url below and get a shorter one or at least better one."]
     (form-comp)

     [:pre "\n\n"]
     [:pre
      "## Using via cli\n
Create a short url with curl:
"
      [:code
       (format
        "curl \"%s?u=%s\""
        (:base-uri config)
        sample-url)]
      "\n\n"
      "or using " [:a {:href "https://github.com/abcdw/ti.wtf/blob/master/ti"} "ti"] " script:\n"
      [:code
       "./ti " sample-url]
      "\n\n\n"
      ]

     [:pre "## Source code\n\nThe source code available at "
      [:a {:href   "https://github.com/abcdw/ti.wtf"
           :target "_blank"} "abcdw/ti.wtf"]]
     ]]])

(defn uri->url [uri]
  (if (re-find #"^http.*" uri)
    uri
    (str (:default-protocol config) "://" uri)))

(rum/defc short-url-comp [url]
  [:body
   {:style
    {:margin 0}}
   [:a {:href   (uri->url url)
        :target "_blank"
        :style  {:margin "0" :color "inherit" :font-family "monospace"}}
     url]])

(defn get-shorten-url! [params]
  (let [url   (get params "u")
        alias (:alias/alias
               (or (get-alias-by-original-url url)
                   (create-alias! url)))

        short-url (str (:base-uri config) "/" alias)]
    {:status 200
     :body
     (if (contains? params "html")
       (rum/render-html (short-url-comp short-url))
       short-url)}))

(defn handle-root-post [{:keys [form-params] :as request}]
  (get-shorten-url! form-params))

(defn handle-root-get [{:keys [form-params query-params] :as request}]
  (if (contains? query-params "u")
    (get-shorten-url! query-params)
    {:status  200
     :headers {"content-type" "text/html"}
     :body    (rum/render-html (root-comp))}))

(defn alternative-alias-get [request]
  (let [alias     (get-in request [:path-params :alias])
        alias-int (Integer. alias)]
    (if (< (mod alias-int 20) 19)
      {:headers {"location" (str "/" (inc alias-int))}
       :status  307}
      {:headers {"content-type" "text/html"}
       :body (format "<meta http-equiv=\"Refresh\" content=\"0; url=%s\" />"
                     (inc alias-int))})))


(defn handle-alias-get [request]
  (let [alias        (-> (get-in request [:path-params :alias])
                         get-alias-by-alias-field)
        original-url (:alias/original-url alias)]
    (if alias
      {:headers {"location"
                 (if (string/blank? original-url)
                   (:base-url config)
                   (uri->url original-url))}
       :status  308}
      {:status 404})))

(def router
  (ring/router
   [["/"
     {:get
      {:handler handle-root-get}
      :post
      {:handler handle-root-post}}]
    ["/:alias"
     {:get
      {:handler handle-alias-get}}]]
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
  (jetty/run-jetty #'app {:port (:port config)}))

(comment
  (def server (jetty/run-jetty #'app {:port (:port config) :join? false}))
  (.stop server)
  (app {:request-method :get :uri "/api"})
  )
