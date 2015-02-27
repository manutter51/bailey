(ns bailey.dispatch
  (require [barnum.api :as bar]))

(def base-events
  {::pre-dispatch
   "The ::pre-dispatch event is fired immediately before the ::dispatch
event to give your application a chance to add any custom pre-processing
directives it wishes to add. Your handler should examine the incoming
request, determine if any additional pre-dispatch events need to be fired,
and then fire them."
   ::dispatch
   "The ::dispatch event is fired when all request pre-processing has been
completed and it is time to dispatch the request to whatever handler is
going to implement the business logic for this particular request.
TODO: define how handling/routing works in Bailey!"
   ::dispatch-error
   "The ::dispatch error event is fired by the dispatch handler whenever a
request cannot be handled. The event data must contain two keys: :error-code
and :error-message, corresponding to the HTTP error code and a user friendly
error message."})