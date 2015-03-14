(ns bailey.app-test
  (require
    [bailey.app :as app]
    [barnum.api :as bar]
    [midje.sweet :as m]))

(m/fact "bailey.app/setup registers events for app initialization"
        (let [ctx (app/setup)
              expected [:init :load-config  :server-failed-start
                        :server-restart :server-start :server-stop :stop
                        :new :auth :dispatch :dispatch-error
                        :pre-dispatch :parse :parse-json :parse-query-string
                        :parse-multipart :parse-xml :post-render
                        :pre-render :render :session
                        :session-store]
              found (bar/event-keys ctx)]
          ;(prn (sort found))
          ;(println)
          ;(prn (sort expected))
          found
          => (m/contains expected :in-any-order :gaps-ok)))

(m/fact "Events have docstrings"
        (bar/docs (app/setup) :server-start)
        => "Fires after the server successfully starts. Intended for debugging/logging.")

(m/fact "bailey.app/setup allows only valid jetty options"
        (:server-options (app/setup :port 8080))
        => (m/contains {:port 8080})

        (:server-options (app/setup :port 8080 :join? true))
        => (m/contains {:port 8080 :join? true} :in-any-order :gaps-ok)

        (:server-options (app/setup :jetty :foo))
        => (m/throws Exception "Unknown option(s): jetty"))

(m/fact "bailey.app setup sets up a default server-restart handler"
        (let [ctx (app/setup :port 8088)]
          (bar/handler-keys ctx :server-restart)
          => (m/contains [:default-restart-handler])))

(m/fact "default restart handler fires the right events in the right order"
        (try
          (let [coll (atom [])
                   mock (fn [key] (fn [ctx data] (swap! coll conj key) (bar/ok data)))
                   ctx (app/setup :port 8089 :join? false)
                   ctx (bar/add-handler ctx :server-stop :ss-handler (mock :ss))
                   ctx (bar/add-handler ctx :stop :stop-handler (mock :stop))
                   ctx (bar/add-handler ctx :load-config :load-handler (mock :load))
                   ctx (bar/add-handler ctx :init :init-handler (mock :init))
                   ctx (bar/add-handler ctx :server-start :start-handler (mock :start))
                   ctx (app/start ctx)
                   _ (reset! coll [])
                   result (bar/fire ctx :server-restart {})
                   results-vec @coll
                   ctx (app/stop-server* ctx)]

               (:status result)
               => :ok

               results-vec
               => (m/exactly [:ss :stop :load :init :start]))
          (catch Exception e (app/stop-server* {}))))