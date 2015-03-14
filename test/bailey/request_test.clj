(ns bailey.request-test
  (require
    [bailey.request :as req]
    [barnum.api :as bar]
    [midje.sweet :as m]))

(m/fact "parse-query-string adds a :query-params map to the data"
        (keys (req/parse-query-string {:query-string "foo=bar"}))
        => (m/contains [:query-params]))

(defn as-data [s]
  {:query-string s})

(m/fact "parse-query-string converts keys to keywords"
        (let [data (as-data "foo=bar")
              result (req/parse-query-string data)
              ks (keys (:query-params result))]
          ks
          => (m/contains [:foo])))

(m/fact "parse-query-string handles multiple keys and repeated keys"
        (let [qstr "foo=foo&bar=bar"
              data (as-data qstr)]
          (req/parse-query-string data)
          => {:query-string qstr :query-params {:foo "foo" :bar "bar"}})

        (let [qstr "foo=foo&bar=bar&foo=foo2"
              data (as-data qstr)]
          (req/parse-query-string data)
          => {:query-string qstr :query-params {:foo ["foo" "foo2"] :bar "bar"}}))