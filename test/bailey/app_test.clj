(ns bailey.app-test
  (require
    [bailey.app :as app]
    [barnum.api :as bar]
    [midje.sweet :as m]))

(m/fact "bailey.app/setup registers events for app initialization"
        (let [ctx (app/setup)]
          (bar/event-keys ctx))
        => (m/contains
             [:bailey.app/init :bailey.app/load-config  :bailey.app/server-failed-start
              ::app/server-restart :bailey.app/server-start :bailey.app/server-stop
              :bailey.req/new :bailey.req/auth :bailey.dispatch/dispatch :bailey.dispatch/dispatch-error
              :bailey.dispatch/pre-dispatch :bailey.req/parse :bailey.req/parse-json
              :bailey.req/parse-multipart :bailey.req/parse-xml :bailey.render/post-render
              :bailey.render/pre-render :bailey.render/render :bailey.req/session
              :bailey.req/session-store]
             :in-any-order :gaps-ok))

(m/fact "Events have docstrings"
        (bar/docs (app/setup) :bailey.app/server-start)
        => "Fires after the server successfully starts. Intended for debugging/logging.")

(m/fact "bailey.app/setup accepts either :jetty or :servlet, but not both"
        (app/setup :jetty)
        => (m/contains {:bailey.app/server-type :jetty})

        (app/setup :servlet)
        => (m/contains {:bailey.app/server-type :servlet})

        (app/setup :jetty :servlet)
        => (m/throws Exception "Server type must be either :jetty or :servlet, not both."))
