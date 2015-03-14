(ns bailey.render
  (require [barnum.api :as bar]))

(def base-events
  {:pre-render
   "The :pre-render event is fired after dispatch handling is complete, and
is intended to allow you to do any pre-rendering of your response data prior
to rendering it for output. The default handler action is to escape any HTML
entities in the response data."
   :render
   "The :render event is fired in order to do the rendering/templating/layout
on the response data before it is returned by the server. The default handler
action is to return the raw data of the response as a single string."
   :post-render
   "The :post-render event is fired to allow your application to apply any
particular post-processing, such as encryption or compression, that you might
want to apply. This is also the last event to fire before the response is
returned by the server, so it's a good place to store any logging/metrics
data you might be accumulating per-request."})