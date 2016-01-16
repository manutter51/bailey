(ns bailey.dispatch
  (require [clojure.string :as str]
           [clout.core :as clout]
           [barnum.api :as bar]))

(def base-events
  {:pre-dispatch
   "The :pre-dispatch event is fired immediately before the :dispatch
event to give your application a chance to add any custom pre-processing
directives it wishes to add. Your handler should examine the incoming
request, determine if any additional pre-dispatch events need to be fired,
and then fire them."
   :dispatch
   "The :dispatch event is fired when all request pre-processing has been
completed and it is time to dispatch the request to whatever handler is
going to implement the business logic for this particular request.
TODO: define how handling/routing works in Bailey!"
   :dispatch-error
   "The :dispatch error event is fired by the dispatch handler whenever a
request cannot be handled. The event data must contain two keys: :error-code
and :error-message, corresponding to the HTTP error code and a user friendly
error message."})

(defn match-any [_]
  true)

(defn match-one [k req]
  (prn "match one " k req (if (= k (:request-method req)) "MATCH" "No match"))
  (= k (:request-method req)))

(defn match-get [req]
  (match-one :get req))

(defn match-post [req]
  (match-one :post req))

(defn match-get-or-post [req]
  (#{:get :post} (:request-method req)))

(defn match-put [req]
  (match-one :put req))

(defn match-delete [req]
  (match-one :delete req))

(defn match-head [req]
  (match-one :head req))

(defn match-options [req]
  (match-one :options req))

(defn make-verb-matcher [verb]
  (condp = verb
    :any match-any
    :get match-get
    :post match-post
    :put match-put
    :delete match-delete
    :head match-head
    :options match-options
    [:get :post] match-get-or-post
    [:post :get] match-get-or-post
    (let [verb-set (into #{} (if (seq? verb) verb [verb]))]
      (fn [req]
        (let [meth (:request-method req)]
          (verb-set meth))))))

(defmacro on
  "Builds a Bailey-compatible dispatch event handler, given a request method,
  a URL string, optional Clout-style options map, a 2-element vector of
  function arguments, and the handler body."
  [verb url clout-options? fn-args? & body?]
  (let [has-clout-options? (map? clout-options?)
        clout-options (if has-clout-options? clout-options? nil)
        clout-compiler (if has-clout-options? `(clout/route-compile ~url ~clout-options)
                                              `(clout/route-compile ~url))
        fn-args (if has-clout-options? fn-args? clout-options?)
        [ctx data] fn-args
        invalid-args-msg (if has-clout-options? "Expected a vector of 2 fn args [ctx data] after clout options map"
                                                "Expected a vector of 2 fn args [ctx data] after url")
        body (if has-clout-options? body? (conj body? fn-args?))]
    ; Quick validity checks
    (if-not (string? url)
      (throw (IllegalArgumentException. "URL must be a string")))
    (if-not (and (vector? fn-args) (= 2 (count fn-args)))
      (throw (IllegalArgumentException. invalid-args-msg)))
    (if (zero? (count body))
      (throw (IllegalArgumentException. "Body must contain at least one form")))
    ; wrap the fn we want inside another immediate-execute fn so we can get a one-time let binding
    `((fn []
        (let [verb-matcher# (make-verb-matcher ~verb)
              route-matcher# ~clout-compiler]
          ; got our closures set up, now return the handler fn
          (fn [~ctx ~data]
            (if-not (verb-matcher# ~data)
              (bar/ok (assoc ~data :last-match-fail "Verb mismatch"))
              (let [params# (clout/route-matches route-matcher# ~data)]
                (if (nil? params#)
                  (bar/ok (assoc ~data :last-match-fail "URL mismatch"))
                  (let [~data (assoc ~data :match-uri ~url :url-params params#)]
                    ~@body))))))))))
