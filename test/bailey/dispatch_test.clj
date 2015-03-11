(ns bailey.dispatch-test
  (require
    [bailey.dispatch :as dispatch]
    [barnum.api :as bar]
    [midje.sweet :as m]))

(m/fact "dispatch/str->re returns a valid regex for plain url"
        (dispatch/str->re "/home")
        => #"^/home$")

(m/fact "urls that include keywords return a matcher that matches the keyword"
        (dispatch/str->re "/profile/:id")
        => #"^/profile/([^/]+)$")

(m/fact "verb-matcher works for individual verbs"
        (let [f (dispatch/make-verb-matcher :get)]
          (f :get)
          => true

          (f :post)
          => false))

(m/fact "verb-matcher works for multiple verbs"
        (let [f (dispatch/make-verb-matcher [:get :post])]
          (f :get)
          => true

          (f :post)
          => true

          (f :put)
          => false))

(m/fact "verb-matcher works for :any"
        (let [f (dispatch/make-verb-matcher :any)]
          (f :get)
          => true

          (f :post)
          => true

          (f :put)
          => true

          (f :delete)
          => true

          (f :options)
          => true

          (f :head)
          => true))

(m/fact "dispatch/make-matcher returns a fn that matches plain urls"
        (let [matcher (dispatch/make-matcher "/home")]

          (matcher "/home")
          => {:url-match "/home" :url-params {}}

          (matcher "/foo")
          => nil))

(m/fact "dispatch/make-matcher returns a fn that matches parameterized urls"
        (let [matcher (dispatch/make-matcher "/profile/:id")]

          (matcher "/profile/17")
          => {:url-match "/profile/:id" :url-params {:id "17"}}

          (matcher "/profile/other/17")
          => nil

          (matcher "/profile/17/other")
          => nil ))

(m/fact "dispatch/on creates a dispatch event handler that handles URLs properly"
        (let [handler (dispatch/on :get "/home"
                                   [ctx data]
                                   (bar/return {:body "Hello, world."}))
              ctx {}
              match-verb-and-url {:request-method :get :uri "/home"}
              match-url-not-verb {:request-method :post :uri "/home"}
              match-verb-not-uri {:request-method :get :uri "/login"}]

          (handler ctx match-verb-and-url)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-url-not-verb)
          => {:status :ok :data match-url-not-verb}

          (handler ctx match-verb-not-uri)
          => {:status :ok :data match-verb-not-uri}))

(m/fact "dispatch/on creates a dispatch event handler that handles params properly"
        (let [handler (dispatch/on :get "/resource/:id/subresource/:type"
                                   [ctx data]
                                   (let [params (:url-params data)]
                                     (bar/return {:body (str "Resource " (:id params) " has subresource " (:type params) ".")})))
              ctx {}
              match-verb-and-url {:request-method :get :uri "/resource/foo/subresource/bar"}
              match-url-not-verb {:request-method :post :uri "/resource/foo/subresource/bar"}
              match-verb-not-uri-1 {:request-method :get :uri "/resource/foo/subresource"}
              match-verb-not-uri-2 {:request-method :get :uri "/resource/foo"}
              match-verb-not-uri-3 {:request-method :get :uri "/resource/foo/subresource/bar/"}
              match-verb-not-uri-4 {:request-method :get :uri "/resource/foo/subresource/"}
              match-verb-not-uri-5 {:request-method :get :uri "/resource/foo/"}
              match-verb-not-uri-6 {:request-method :get :uri "/resource/"}
              match-verb-not-uri-7 {:request-method :get :uri "/resource"}]

          (handler ctx match-verb-and-url)
          => {:status :ok-return :data {:body "Resource foo has subresource bar."}}

          (handler ctx match-url-not-verb)
          => {:status :ok :data match-url-not-verb}

          (handler ctx match-verb-not-uri-1)
          => {:status :ok :data match-verb-not-uri-1}

          (handler ctx match-verb-not-uri-2)
          => {:status :ok :data match-verb-not-uri-2}

          (handler ctx match-verb-not-uri-3)
          => {:status :ok :data match-verb-not-uri-3}

          (handler ctx match-verb-not-uri-4)
          => {:status :ok :data match-verb-not-uri-4}

          (handler ctx match-verb-not-uri-5)
          => {:status :ok :data match-verb-not-uri-5}

          (handler ctx match-verb-not-uri-6)
          => {:status :ok :data match-verb-not-uri-6}

          (handler ctx match-verb-not-uri-7)
          => {:status :ok :data match-verb-not-uri-7}))

(m/fact "dispatch/on creates a dispatch event handler that handles :any verb properly"
        (let [handler (dispatch/on :any "/home"
                                   [ctx data]
                                   (bar/return {:body "Hello, world."}))
              ctx {}
              match-verb-and-url {:request-method :get :uri "/home"}
              match-url-not-verb {:request-method :post :uri "/home"}
              match-verb-not-uri {:request-method :get :uri "/login"}]

          (handler ctx match-verb-and-url)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-url-not-verb)
          => {:status :ok-return :data {:body "Hello, world."}}

          (handler ctx match-verb-not-uri)
          => {:status :ok :data match-verb-not-uri}))