(ns bailey.app-test
  (require
    [bailey.app :as app]
    [barnum.api :as bar]
    [midje.sweet :as m]))

(m/fact "bailey.app/setup registers events for app initialization"
        (let [ctx (app/setup)
              expected [:bailey.app/init :bailey.app/load-config  :bailey.app/server-failed-start
                        :bailey.app/server-restart :bailey.app/server-start :bailey.app/server-stop :bailey.app/stop
                        :bailey.request/new :bailey.request/auth :bailey.dispatch/dispatch :bailey.dispatch/dispatch-error
                        :bailey.dispatch/pre-dispatch :bailey.request/parse :bailey.request/parse-json
                        :bailey.request/parse-multipart :bailey.request/parse-xml :bailey.render/post-render
                        :bailey.render/pre-render :bailey.render/render :bailey.request/session
                        :bailey.request/session-store]
              found (bar/event-keys ctx)]
          ;(prn (sort found))
          ;(println)
          ;(prn (sort expected))
          found
          => (m/contains expected :in-any-order :gaps-ok)))

(m/fact "Events have docstrings"
        (bar/docs (app/setup) :bailey.app/server-start)
        => "Fires after the server successfully starts. Intended for debugging/logging.")

(m/fact "bailey.app/setup allows only valid jetty options"
        (:bailey.app/server-options (app/setup :port 8080))
        => (m/contains {:port 8080})

        (:bailey.app/server-options (app/setup :port 8080 :join? true))
        => (m/contains {:port 8080 :join? true} :in-any-order :gaps-ok)

        (:bailey.app/server-options (app/setup :jetty :foo))
        => (m/throws Exception "Unknown option(s): jetty"))

(m/fact "bailey.app setup sets up a default server-restart handler"
        (let [ctx (app/setup :port 8088)]
          (bar/handler-keys ctx :bailey.app/server-restart)
          => (m/contains [:bailey.app/default-restart-handler])))

(m/fact "default restart handler fires the right events in the right order"
        (try
          (let [coll (atom [])
                   mock (fn [key] (fn [ctx data] (swap! coll conj key) (bar/ok data)))
                   ctx (app/setup :port 8089 :join? false)
                   ctx (bar/add-handler ctx :bailey.app/server-stop :ss-handler (mock :ss))
                   ctx (bar/add-handler ctx :bailey.app/stop :stop-handler (mock :stop))
                   ctx (bar/add-handler ctx :bailey.app/load-config :load-handler (mock :load))
                   ctx (bar/add-handler ctx :bailey.app/init :init-handler (mock :init))
                   ctx (bar/add-handler ctx :bailey.app/server-start :start-handler (mock :start))
                   ctx (app/start-server ctx)
                   _ (reset! coll [])
                   result (bar/fire ctx :bailey.app/server-restart)
                   results-vec @coll
                   ctx (app/stop-server* ctx)]

               (:status result)
               => :ok

               results-vec
               => (m/exactly [:ss :stop :load :init :start]))
          (catch Exception e (app/stop-server* {}))))