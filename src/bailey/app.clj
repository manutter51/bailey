(ns bailey.app
  (require [barnum.api :as bar]
           [ring.adapter.jetty :as jetty]
           [bailey.request :as req]
           [bailey.dispatch :as disp]
           [bailey.render :as rend]))

(def ring-server (atom nil))

(defn base-handler [ctx request]
  (let [result (bar/fire-all ctx
                             [:new :session :auth :parse
                              :pre-dispatch :dispatch
                              :pre-render :render :post-render]
                             request)
        status (:status result)
        response (:data result)]
    (if (= status :ok)
      response
      {:status 500
       :headers {"Content-Type" "text/plain"}
       :body "Internal server error"})))

(defn start-server* [ctx]
  ;; should probably check here for running server in @ring-server, and do something
  (let [opts (:server-options ctx)
        handler (fn [request]
                  (base-handler ctx request))]
    (reset! ring-server
            (jetty/run-jetty handler opts))
    ctx))

(defn start [ctx]
  (try
    (let [ctx (start-server* ctx)
          result (bar/fire ctx :server-start {})]
      (:status result))
    (catch Exception e
      (let [result (bar/fire ctx :server-failed-start {:exception e})]
        (:status result)))))

(defn stop-server* [ctx]
  (when-let [server @ring-server]
    (.stop server)
    (reset! ring-server nil))
  ctx)

(defn stop [ctx]
  (let [ctx (stop-server* ctx)]
    (bar/fire-all ctx [:server-stop :stop])))

(def valid-options
  #{:configurator :port :host :join? :daemon? :ssl? :ssl-port :keystore :key-password
    :truststore :trust-password :max-threads :max-queued :min-threads :max-idle-time
    :client-auth})

(def default-options
  {:port 8080
   :host "127.0.0.1"
   :join? false
   :ssl? false})

(defn parse-opts [opts]
  (if-let [unknown (seq (filter #(nil? (valid-options %)) (keys opts)))]
    (throw (Exception. (apply str "Unknown option(s): " (map name unknown))))
    opts))

(def base-events
  {:load-config
   "Fires just before :bailey.app/init, to let you load any
application-specific configuration files."
   :init
   "Fires after :bailey.app/load-config and before launching the
server, to let you connect to any database servers, caches, or other
dependencies."
   :server-start
   "Fires after the server successfully starts. Intended for debugging/logging."
   :server-failed-start
   "Fires if the server fails to start for any reason."
   :server-stop
   "Fires after the server is stopped. Intended for debugging/logging."
   :stop
   "Fired after the server stops and after the :server-stop event, to let
you shut down database connections or other resources that require some kind
of cleanup on shutdown."
   :server-restart
   "Fire this event to trigger a server restart. Default handler action
is to stop the server and then fire the :server-stop, :stop, :load-config,
and :init events, and then start the server."})

(defn setup-events [ctx events-map]
  (loop [[event docstring] (first events-map)
         todo (next events-map)
         ctx ctx]
    (if-not event
      ctx
      (recur (first todo) (next todo) (bar/add-event ctx event docstring)))))

(defn setup-restart-handler [ctx]
  (bar/add-handler
    ctx :server-restart :default-restart-handler
    (fn [ctx data]
      (let [ctx (stop-server* ctx)
            result (barnum.api/fire-all ctx [:server-stop :stop :load-config :init] data)
            status (:barnum.api/status result)
            ctx (:barnum.api/context result)
            data (:barnum.api/data result)]
        (if (= :ok status)
          (let [ctx (start-server* ctx)]
            (assoc (bar/ok data) :barnum.api/context ctx))
          ;; else return whatever the failure status was
          result)))))

#_(defn setup-handlers [ctx handler-setups]
  (reduce (fn [c f] (f c)) ctx handler-setups))

(defn setup
  "Sets up built-in events and default event handlers. The setup function
takes an optional list of standard Jetty configuration options, as keyword
/ value pairs. It is not necessary to wrap the pairs inside curly braces."
  [& opts]
  (let [ctx (-> {}
                (setup-events base-events)
                (setup-events req/base-events)
                (setup-events disp/base-events)
                (setup-events rend/base-events)
                (setup-restart-handler))
        opts (apply hash-map opts)
        opts (merge default-options opts)
        opts (parse-opts opts)]
    (assoc ctx :server-options opts)))

