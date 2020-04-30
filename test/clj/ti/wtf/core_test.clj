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

(def sample-url "https://example.org/very/long/url")

(deftest root-html
  (let [query          {:request-method :get
                        :uri            "/"}
        {:keys [status body]
         :as   result} (sut/app query)]
    (is (= 200 status))
    (is body)
    ;; (?s) for multiline matching
    (is (re-find #"(?s)</form>" body))))

(deftest shorten-url
  (let [query           {:request-method :get
                         :uri            "/"
                         :query-params   {"u" sample-url}}
        new-url-pattern (re-pattern (str ".*" (:domain sut/config) "/.*"))
        {:keys [status body]
         :as   result}  (sut/app query)
        redirect-url    body]

    (testing "get shorten url for provided url"
      (is (= 200 status))
      (is (re-matches new-url-pattern redirect-url)))

    (testing "same url generates same alias"
      (let [query {:request-method :get
                            :uri            "/"
                            :query-params   {"u" sample-url}}

            {:keys [status body]} (sut/app query)]
        (is (= 200 status))
        (is (= redirect-url body))))

    (testing "url redirect is correct"
      (let [redirect-query           {:request-method :get
                                      :uri            (string/replace redirect-url #".*/" "/")}
            {:keys [status headers]} (sut/app redirect-query)
            {:strs [location]}       headers]
        (is (= 308 status))
        (is (= sample-url location))))))

(deftest html-response-check
  (testing "get html instead just url"
    (let [query          {:request-method :get
                          :uri            "/"
                          :query-params   {"u"    sample-url
                                           "html" "true"}}
          {:keys [status body]
           :as   result} (sut/app query)]
      (is (= 200 status))
      (is (re-find #"(?s)</body>" body)))))

(deftest alias-not-found
  (let [alias          "baacaac"
        query          {:request-method :get
                        :uri            (str "/" alias)}
        {:keys [status headers]
         :as   result} (sut/app query)]
    (is (= 404 status))))

(deftest id->alias
  (is (= "a" (sut/id->alias 0)))
  (is (= "9" (sut/id->alias 61)))
  (is (= "ba" (sut/id->alias 62))))
