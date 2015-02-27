(ns bailey.app
  (require [barnum.api :as bar]
           [bailey.req :as req]
           [bailey.dispatch :as disp]
           [bailey.render :as rend]))

(defn parse-opts [opts]
  (let [server-type-opts (filter #{:jetty :servlet} opts)]
    (if (> (count server-type-opts) 1)
      (throw (Exception. "Server type must be either :jetty or :servlet, not both.")))
    {::server-type (first (conj (vec server-type-opts) :jetty))}))

(def base-events
  {::load-config
   "Fires just before :bailey.app/init, to let you load any
application-specific configuration files."
   ::init
   "Fires after :bailey.app/load-config and before launching the
server, to let you connect to any database servers, caches, or other
dependencies."
   ::server-start
   "Fires after the server successfully starts. Intended for debugging/logging."
   ::server-failed-start
   "Fires if the server fails to start for any reason."
   ::server-stop
   "Fires after the server is stopped. Intended for debugging/logging."
   ::stop
   "Fired after the server stops and after the ::server-stop event, to let
you shut down database connections or other resources that require some kind
of cleanup on shutdown."
   ::server-restart
   "Fire this event to trigger a server restart. Default handler action
is to stop the server and then fire the ::server-stop, ::stop, ::load-config,
and ::init events, and then start the server."})

(defn setup-events [ctx events-map]
  (loop [[event docstring] (first events-map)
         todo (next events-map)
         ctx ctx]
    (if-not event
      ctx
      (recur (first todo) (next todo) (bar/add-event ctx event docstring)))))

(defn setup
  "Sets up built-in events and default event handlers. The setup function
takes an optional list of keyword options that modify how Bailey behaves
at run time:

  * :jetty [default] Launches the ring-jetty server to handle web requests
  * :servlet Launches the web server as a ring-servlet

Use either the :jetty or the :servlet options, not both."
  [& opts]
  (let [ctx (-> {}
                (setup-events base-events)
                (setup-events req/base-events)
                (setup-events disp/base-events)
                (setup-events rend/base-events))
        opts (parse-opts opts)]
    (assoc ctx ::server-type (::server-type opts))))