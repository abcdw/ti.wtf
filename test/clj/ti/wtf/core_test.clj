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
    ;; (?s) for multiline matching
    (is (re-find #"(?s)</form>" body))))

(deftest get-shorten-url
  (testing "get shorten url for provided url"
    (let [query           {:request-method :get
                           :uri            "/"
                           :query-params   {"shorten" "https://example.org/very/long/url"}}
          new-url-pattern (re-pattern (str (:base-url sut/config) "/.*"))
          {:keys [status body]
           :as   result}  (sut/app query)]
             (is (= 200 status))
             (is (re-matches new-url-pattern body))))

  (testing "get html instead just url"
    (let [query {:request-method :get
                 :uri            "/"
                 :query-params   {"shorten" "https://example.org/very/long/url"
                                  "html"    "true"}}
          {:keys [status body]
           :as   result} (sut/app query)]
      (is (= 200 status))
      (is (re-find #"(?s)</body>" body)))))

(deftest shorthand-redirect
  (let [shorthand      "baa"
        query          {:request-method :get
                        :uri            (str "/" shorthand)}
        {:keys [status headers]
         :as   result} (sut/app query)]
    (is (= 308 status))
    (is (contains? headers "location"))))

(deftest generate-link
  (let [db     {:urls [{}]}
        url ""
        result (sut/generate-link db url)]))

(deftest id->shorthand
  (is (= "a" (sut/id->shorthand 0)))
  (is (= "9" (sut/id->shorthand 61)))
  (is (= "ba" (sut/id->shorthand 62))))
