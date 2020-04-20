(ns ti.wtf.core-test
  (:require [ti.wtf.core :as sut]
            [clojure.test :refer :all]
            [clojure.string :as string]))


(deftest root-html
  (let [query {:request-method :get
               :uri            "/"}
        result (sut/app query)]
    (is (= 200 (:status result)))
    (is (contains? result :body))))

(deftest create-shorten-url
  (let [query          {:request-method :post
                        :uri            "/"
                        :form-params    {:shorten "https://example.org/very/long/url"}}
        {:keys [status body]
         :as   result} (sut/app query)]
    (is (= 200 status))
    (is (re-matches #"http://.*/u/.*" body))))

(deftest generate-link
  (let [db     {:urls [{}]}
        url ""
        result (sut/generate-link db url)]))

(deftest id->shorthand
  (is (= "a" (sut/id->shorthand 0)))
  (is (= "9" (sut/id->shorthand 61)))
  (is (= "ba" (sut/id->shorthand 62))))



;;
