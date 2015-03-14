(ns bailey.request
  (require [barnum.api :as bar])
  (import [java.net URLDecoder]))

(def base-events
  {:new
   "A new request has come in. Fired before any other
 request processing occurs. Event data contains the full,
 raw request map from Ring."
   :session
   "The :session event fires immediately after the :new
 event. Your handler should load the session data, if any
 (and assuming you want session handling). Default handling
 is to do nothing, i.e. no sessions."
   :auth
   "The :auth even fires immediately after the :session
 event, and is intended for you to supply any authentication
 and/or authorization handling based on the current request.
 Default handling is to do nothing, i.e. no AUTHN/AUTHZ."
   :parse
   "The :parse event fires immediately after the :auth
 event and is intended for any body parsing that needs to
 be done. The default handler checks the content type for
 xml, json, or multipart form data, and fires the :parse-xml,
 :parse-json, or :parse-multipart events as appropriate."
   :parse-query-string
   "The :parse-query-string event is fired by the default :parse event
handler whenever the incoming request contains a non-empty
query string. The default operation is for the handler to add
a :query-params map to the request data, containing the URL-decoded
keys and values parsed from the query string."
   :parse-xml
   "The :parse-xml event is fired by the default :parse event
handler whenever the incoming request contains XML data. The
default :parse-xml handler action is to upload the XML to a
temporary file and then add an XML stream handle to the request
object under the key :xml-stream."
   :parse-json
   "The :parse-json event is fired by the default :parse event
handler whenever the incoming request contains JSON data. The
default :parse-json handler action is to parse the JSON and add
the resulting map to the request object under the key :json."
   :parse-multipart
   "The :parse-multipart event is fired by the default :parse
event handler whenever the incoming request contains multipart form
data. The default :parse-multipart handler action is to upload the
file(s) then add a map to the request object under the key :form.
The keys in the map will correspond to the field names in the multipart
form, and will contain either strings or file handles, depending on
the contents of the corresponding multipart form."
   ;; :dispatch events are in their own namespace
   :session-store
   "The :session-store event is fired after dispatch processing is complete,
to allow your application to save any session data you might want to persist
between requests."})

(defn string-to-pair [s & [encoding]]
  (let [encoding (if encoding encoding "UTF-8")
        pair (clojure.string/split s #"=")
        k (URLDecoder/decode (first pair) encoding)
        v (URLDecoder/decode (second pair) encoding)
        v (if (re-matches #"^.*\[\]$" k) [v] v)
        k (clojure.string/replace k #"[^A-Za-z0-9_*-]" "")]
    {(keyword k) v}))

(defn combine [a b]
  (cond
    (and (coll? a) (coll? b)) (vec (concat a b))
    (coll? a) (conj a b)
    (coll? b) (vec (conj (seq b) a))
    :else [a b]))

(defn parse-query-string [data]
  (if-let [query-string (:query-string data)]
    (let [pairs (clojure.string/split query-string #"\&")
          query-params (apply merge-with combine {} (map string-to-pair pairs))]
      (assoc data :query-params query-params))
    ; else
    data))