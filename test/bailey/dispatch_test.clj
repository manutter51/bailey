(ns bailey.dispatch-test
  (require
    [bailey.dispatch :as dispatch]
    [barnum.api :as bar]
    [ring.mock.request :as mock]
    [midje.sweet :as m]))

(defn with-url-mismatch [data]
  (assoc data :last-match-fail "URL mismatch"))

(defn with-verb-mismatch [data]
  (assoc data :last-match-fail "Verb mismatch"))

(m/fact "verb-matcher works for individual verbs"
        (let [f (dispatch/make-verb-matcher :get)]
          (f (mock/request :get "/"))
          => m/truthy

          (f (mock/request :post "/"))
          => m/falsey))

(m/fact "verb-matcher works for multiple verbs"
        (let [f (dispatch/make-verb-matcher [:get :post])]
          (f (mock/request :get "/"))
          => m/truthy

          (f (mock/request :post "/"))
          => m/truthy

          (f (mock/request :put "/"))
          => m/falsey))

(m/fact "verb-matcher works for :any"
        (let [f (dispatch/make-verb-matcher :any)]
          (f (mock/request :get "/"))
          => m/truthy

          (f (mock/request :post "/"))
          => m/truthy

          (f (mock/request :put "/"))
          => m/truthy

          (f (mock/request :delete "/"))
          => m/truthy

          (f (mock/request :options "/"))
          => m/truthy

          (f (mock/request :head "/"))
          => m/truthy))

(m/fact "dispatch/on creates a dispatch event handler that handles URLs properly"
        (let [handler (dispatch/on :get "/home"
                                   [ctx data]
                                   (bar/return {:body "Hello, world."}))
              ctx {}
              match-verb-and-url (mock/request :get "/home")
              match-url-not-verb (mock/request :post "/home")
              match-verb-not-uri (mock/request :get "/login")]

          (handler ctx match-verb-and-url)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-url-not-verb)
          => {:status :ok :data (with-verb-mismatch match-url-not-verb)}

          (handler ctx match-verb-not-uri)
          => {:status :ok :data (with-url-mismatch match-verb-not-uri)})

        (m/fact "dispatch/on creates a dispatch event handler that handles params properly"
                (let [handler (dispatch/on :get "/resource/:id/subresource/:type"
                                           [ctx data]
                                           (let [params (:url-params data)]
                                             (bar/return {:body (str "Resource " (:id params) " has subresource " (:type params) ".")})))
                      ctx {}
                      match-verb-and-url (mock/request :get "/resource/foo/subresource/bar")
                      match-url-not-verb (mock/request :post "/resource/foo/subresource/bar")
                      match-verb-not-uri-1 (mock/request :get "/resource/foo/subresource")
                      match-verb-not-uri-2 (mock/request :get "/resource/foo")
                      match-verb-not-uri-3 (mock/request :get "/resource/foo/subresource/bar/")
                      match-verb-not-uri-4 (mock/request :get "/resource/foo/subresource/")
                      match-verb-not-uri-5 (mock/request :get "/resource/foo/")
                      match-verb-not-uri-6 (mock/request :get "/resource/")
                      match-verb-not-uri-7 (mock/request :get "/resource")]

                  (handler ctx match-verb-and-url)
                  => {:status :ok-return :data {:body "Resource foo has subresource bar."}}

                  (handler ctx match-url-not-verb)
                  => {:status :ok :data (with-verb-mismatch match-url-not-verb)}

                  (handler ctx match-verb-not-uri-1)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-1)}

                  (handler ctx match-verb-not-uri-2)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-2)}

                  (handler ctx match-verb-not-uri-3)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-3)}

                  (handler ctx match-verb-not-uri-4)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-4)}

                  (handler ctx match-verb-not-uri-5)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-5)}

                  (handler ctx match-verb-not-uri-6)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-6)}

                  (handler ctx match-verb-not-uri-7)
                  => {:status :ok :data (with-url-mismatch match-verb-not-uri-7)})))

(m/fact "dispatch/on creates a dispatch event handler that handles :any verb properly"
        (let [handler (dispatch/on :any "/home"
                                   [ctx data]
                                   (bar/return {:body "Hello, world."}))
              ctx {}
              match-verb-and-url (mock/request :get "/home")
              match-url-not-verb (mock/request :post "/home")
              match-verb-not-uri (mock/request :get "/login")
              ]

          (handler ctx match-verb-and-url)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-url-not-verb)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-verb-not-uri)
          => {:status :ok :data (with-url-mismatch match-verb-not-uri)}))
