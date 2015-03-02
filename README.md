# Bailey, an event-based web application server

Using the event-management capabilities of the Barnum library, Bailey
provides an event-based architecture for building, testing, and deploying
web applications as a loosely-coupled network of nanoservices. Nanoservices
are like microservices in that they provide access to well-defined
functionality via published API interfaces. Unlike microservices, however,
nanoservices exist within a single web application, and communicate with
each other via events rather than via HTTP. This results in faster inter-service
communication, possibly at the expense of scalability. Due to the event-based
architecture, however, scalability is still obtainable: just send events
over the network to other servers. Yes, this brings in all the caveats that
microservices are subject to, but the point is that with nanoservices, you
can make intelligent decisions about which services to put on the network and
which to keep in-app.

## Overview

Bailey applications typically run through a three-step process:

  * Setup -- call `bailey.app/setup` so Bailey can register its built-in events and default handlers
  * Registration -- use the Barnum library to register your handlers and any custom events
  * Execution -- call `bailey.app/run` to start event-based processing

You may be tempted to do more than just register handlers and events during the Registration step,
such as setting up database connections and so on. Don't. Bailey has special initialization events
for things like setting up your dependencies, reading configuration files, and so on. A corresponding
set of "teardown" events will also be fired whenever the application shuts down or restarts. By putting
all your dependency-management code inside event handlers, you can easily shutdown or restart your
application, or even just reload configuration settings, simply by firing the right event. You can even
do this from the REPL, assuming you've set one up.

## Dispatch

There's one Bailey event in particular that you will need to write handlers for, and that's the `::dispatch`
event. The `::dispatch` event is the event that fires when incoming request data has been pre-processed and
it is now time to handle the actual request. The format of a `dispatch` handler is similar to Compojure URL
routing, with some minor variations.  Here's an example.

    (bailey.dispatch/handle GET "/path/resource/:id" ctx data
      (let [row (db/get id)]
        (barnum.results/ok (select-keys [:id :first_name :last_name] row))))

The `bailey.dispatch` namespace provides a special `handle` macro you can use to write dispatch handlers. The
syntax is straightforward: `(bailey.dispatch/handle VERB url-pattern & body)`, where `VERB` is one of the
standard HTTP verbs like GET and POST, `url-pattern` is a string corresponding to the URL you want this handler
to respond to, `ctx` is the bailey "context" for the current request, `data` is the current request data (if any)
and `body` is a list of one or more forms that implement the business logic for this particular handler. Your
handler should use one of the `barnum.results` functions to return data from your handler along with a status
marker indicating whether any errors occurred, whether to keep executing any additional handlers for the current
event, or whether to jump to a new event.

Use ANY as the HTTP verb to match any incoming HTTP verb (including DELETE).

The data you return should not include any raw HTML, and in fact any HTML in your data at this point should be
converted to HTML Entities to avoid client-side JS injection attacks. To convert your response to HTML, use
a handler for the `render` event, described below.

Your dispatch handler will be given a `ctx` context object containing the complete context of the current request,
including the `:request` and `:response` keys containing the HTTP request map and HTTP response map, respectively.
All data in the `ctx` context should be considered read-only. Any local modifications you make to `ctx` will be
lost as soon as your handler returns. To modify the current state of your application, fire the appropriate event.

## Render

The second primary event your application will want to handle is the `::render` event. The default rendering action
is to return the data map as a JSON string. To return data in any other format, such as edn, HTML, or XML, attach a
handler for it on the `::render` event. The `bailey.render/handle` macro sets up a render handler for you much like
the `bailey.dispatch/handler` function, except with a mime type instead of an HTTP verb.

    (bailey.render/handle "text/html" "/path/resource/:id" ctx data
      (let [{:keys [first_name last_name} data]
        (html
          [:div
            [:h1 "Person"]
            [:p "The person you are looking for is " first_name " " last_name "."]] )))



## TODO

  - Add launch/shutdown events for starting/stopping by firing events from the REPL?
  - Implement dispatch functions
  - Implement render functions
  - Start building libraries of basic handlers for session, auth, parse, etc
      -- actually, the parse handler should be built-in, based on the incoming content type.
  - Wrap base handler in a try/catch
  - Start building foundational error-handling in base handler

## License

Copyright © 2015 Mark Nutter

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
