(ns bailey.test-server
  (:require [bailey.app :as app]
            [bailey.dispatch :as disp]
            [bailey.render :as rndr]
            [barnum.api :as bar]
            [clojure.pprint :refer [pprint]]))

(def ctx-atom (atom {}))

(def echo-fn
  (disp/on :get "/echo"
           [ctx data]
           (bar/ok-return {:body    (with-out-str (pprint data))
                           :headers {"Content-Type" "text/plain"}})))

(defn run []
  (let [ctx (-> (app/setup :port 8088)
                (bar/add-handler :dispatch :echo-fn #'echo-fn))]
    (reset! ctx-atom ctx)
    (app/start ctx)))

(defn stop []
  (let [result (app/stop @ctx-atom)]
    (:status result)))

(defn restart []
  (stop)
  (run))
