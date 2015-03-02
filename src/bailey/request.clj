(ns bailey.request
  (require [barnum.api :as bar]))

(def base-events
  {::new
   "A new request has come in. Fired before any other
 request processing occurs. Event data contains the full,
 raw request map from Ring."
   ::session
   "The ::session event fires immediately after the ::new
 event. Your handler should load the session data, if any
 (and assuming you want session handling). Default handling
 is to do nothing, i.e. no sessions."
   ::auth
   "The ::auth even fires immediately after the ::session
 event, and is intended for you to supply any authentication
 and/or authorization handling based on the current request.
 Default handling is to do nothing, i.e. no AUTHN/AUTHZ."
   ::parse
   "The ::parse event fires immediately after the ::auth
 event and is intended for any body parsing that needs to
 be done. The default handler checks the content type for
 xml, json, or multipart form data, and fires the ::parse-xml,
 ::parse-json, or ::parse-multipart events as appropriate."
   ::parse-xml
   "The ::parse-xml event is fired by the default ::parse event
handler whenever the incoming request contains XML data. The
default ::parse-xml handler action is to upload the XML to a
temporary file and then add an XML stream handle to the request
object under the key ::xml-stream."
   ::parse-json
   "The ::parse-json event is fired by the default ::parse event
handler whenever the incoming request contains JSON data. The
default ::parse-json handler action is to parse the JSON and add
the resulting map to the request object under the key ::json."
   ::parse-multipart
   "The ::parse-multipart event is fired by the default ::parse
event handler whenever the incoming request contains multipart form
data. The default ::parse-multipart handler action is to upload the
file(s) then add a map to the request object under the key ::form.
The keys in the map will correspond to the field names in the multipart
form, and will contain either strings or file handles, depending on
the contents of the corresponding multipart form."
   ;; ::dispatch events are in their own namespace
   ::session-store
   "The ::session-store event is fired after dispatch processing is complete,
to allow your application to save any session data you might want to persist
between requests."})