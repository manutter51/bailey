(ns bailey.dispatch
  (require [clojure.string :as str]
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

(defn to-named-group [s]
  (if (= \: (first s))
    (str "([^/]+)")
    s))

(defn to-key [s]
  (if (= \: (first s))
    (keyword (apply str (rest s)))))

(defn str->re [url]
  (let [groups (str/split url #"/")
        keyed (map to-named-group groups)
        url (str/join "/" keyed)]
    (re-pattern (str "^" url "$"))))

(defmacro make-matcher [url]
  (let [re (str->re url)
        groups (str/split url #"/")
        keys (filter identity (map to-key groups))]
    `(fn [live-url#]
      (let [matches# (re-find ~re live-url#)
            url-params# (into {} (map (fn [a# b#] [a# b#]) [~@keys] (rest matches#)))]
        (if matches# {:url-match  ~url
                     :url-params url-params#})))))

(defmacro make-verb-matcher [verb]
  (if (= :any verb)
    `(fn [ignored#] true)
    (if (coll? verb)
      (let [in-verbs? (into #{} verb)]
        `(fn [v#] (not= nil (~in-verbs? v#))))
      `(fn [v#] (= v# ~verb)))))

(defmacro on [verb url fn-args & body]
  (if (zero? (count body))
    (throw (Exception. "Body must contain at least one form.")))
  (if-not (and (vector? fn-args) (= 2 (count fn-args)))
    (throw (Exception. "Third argument to bailey.dispatch/on must be a vector containing two symbols.")))
  (let [[ctx-sym data-sym] fn-args]
    `(fn [~ctx-sym ~data-sym]
       (if-not ((make-verb-matcher ~verb) (:request-method ~data-sym))
         (barnum.api/ok ~data-sym)
         (let [match-data# ((make-matcher ~url) (:uri ~data-sym))
               ~data-sym (merge ~data-sym match-data#)]
           (if (nil? match-data#)
             (barnum.api/ok ~data-sym)
             (do ~@body)))))))