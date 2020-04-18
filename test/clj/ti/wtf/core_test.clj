(ns ti.wtf.core-test
  (:require [ti.wtf.core :as sut]
            [clojure.test :as t]
            [clojure.string :as string]))


(t/deftest root-html
  (let [query {:request-method :get
               :uri            "/"}
        result (sut/app query)]
    (t/is (= 200 (:status result)))
    (t/is (contains? result :body)))
  (t/testing ""
    (t/is true)))

(t/deftest create-shorten-url
  (let [query          {:request-method :post
                        :uri            "/"
                        :form-params    {:shorten "https://example.org/very/long/url"}}
        {:keys [status body]
         :as   result} (sut/app query)]
    (t/is (= 201 status))
    (t/is (re-matches #"http://.*/u/.*" body))))

(def db {:url [{:a :b} {:c :d}]})

(t/deftest generate-link
  (let [db     {:urls [{}]}
        url ""
        result (sut/generate-link db url)]))

(t/deftest id->shorthand
  (t/is (= "a" (sut/id->shorthand 0)))
  (t/is (= "9" (sut/id->shorthand 61)))
  (t/is (= "ba" (sut/id->shorthand 62))))



;;
