(ns ti.wtf.core-test
  (:require [ti.wtf.core :as sut]
            [clojure.test :refer :all]
            [clojure.string :as string]))

(defn reset-db [f]
  (sut/unmigrate!)
  (sut/migrate!)
  (f)
  (sut/unmigrate!)
  (sut/migrate!))

(use-fixtures :once reset-db)

(deftest root-html
  (let [query          {:request-method :get
                        :uri            "/"}
        {:keys [status body]
         :as   result} (sut/app query)]
    (is (= 200 status))
    (is body)
    ;; (?s) for multiline matching
    (is (re-find #"(?s)</form>" body))))

(deftest get-shorten-url
  (testing "get shorten url for provided url"
    (let [query           {:request-method :get
                           :uri            "/"
                           :query-params   {"s" "https://example.org/very/long/url"}}
          new-url-pattern (re-pattern (str (:base-url sut/config) "/.*"))
          {:keys [status body]
           :as   result}  (sut/app query)]
             (is (= 200 status))
             (is (re-matches new-url-pattern body))))

  (testing "get html instead just url"
    (let [query {:request-method :get
                 :uri            "/"
                 :query-params   {"s" "https://example.org/very/long/url"
                                  "html"    "true"}}
          {:keys [status body]
           :as   result} (sut/app query)]
      (is (= 200 status))
      (is (re-find #"(?s)</body>" body)))))

(deftest alias-redirect
  (let [alias          "baa"
        query          {:request-method :get
                        :uri            (str "/" alias)}
        {:keys [status headers]
         :as   result} (sut/app query)]
    (is (= 308 status))
    (is (contains? headers "location"))))

(deftest generate-link
  (let [db     {:urls [{}]}
        url ""
        result (sut/generate-link db url)]))

(deftest id->alias
  (is (= "a" (sut/id->alias 0)))
  (is (= "9" (sut/id->alias 61)))
  (is (= "ba" (sut/id->alias 62))))
