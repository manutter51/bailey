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
          => nil))

(m/fact "dispatch/on creates a dispatch event handler"
        (macroexpand-1 '(dispatch/on :get "/home"
                                     [ctx data]
                                     ({:body "Hello, world."})))
        => '(fn [ctx data] "Hello, world"))