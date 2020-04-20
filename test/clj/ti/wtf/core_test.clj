(ns ti.wtf.core-test
  (:require [ti.wtf.core :as sut]
            [clojure.test :refer :all]
            [clojure.string :as string]))


(deftest root-html
  (let [query          {:request-method :get
                        :uri            "/"}
        {:keys [status body]
         :as   result} (sut/app query)]
    (is (= 200 status))
    (is body)
    (is (re-find #"(?s)</form>" body))))

(deftest get-shorten-url
  (let [query           {:request-method :get
                         :uri            "/"
                         :query-params   {"shorten" "https://example.org/very/long/url"}}
        new-url-pattern (re-pattern (str (:domain sut/config) "/u/.*"))
        {:keys [status body]
         :as   result}  (sut/app query)]
    (is (= 200 status))
    (is (re-matches new-url-pattern body))))

(deftest generate-link
  (let [db     {:urls [{}]}
        url ""
        result (sut/generate-link db url)]))

(deftest id->shorthand
  (is (= "a" (sut/id->shorthand 0)))
  (is (= "9" (sut/id->shorthand 61)))
  (is (= "ba" (sut/id->shorthand 62))))
