(ns puppetlabs.puppetdb.http.server
  "REST server

   Consolidates our disparate REST endpoints into a single Ring
   application."
  (:require [clojure.tools.logging :as log]
            [puppetlabs.puppetdb.http :as http]
            [puppetlabs.puppetdb.http.v4 :refer [v4-app]]
            [puppetlabs.puppetdb.http.experimental :refer [experimental-app]]
            [puppetlabs.puppetdb.middleware :refer
             [wrap-with-debug-logging wrap-with-authorization wrap-with-certificate-cn
              wrap-with-globals wrap-with-metrics wrap-with-default-body]]
            [net.cgrand.moustache :refer [app]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect header]]))

(defn deprecated-app
  [app msg request]
  (let [result (app request)]
    (log/warn msg)
    (header result "X-Deprecation" msg)))

(defn experimental-warning
  [app msg request]
  (let [result (app request)]
    (log/warn msg)
    (header result "Warning" msg)))

(defn- refuse-retired-api
  [version]
  (constantly
   (http/error-response
    (format "The %s API has been retired; please use v4" version)
    404)))

(defn routes
  [url-prefix]
  (app
   ["v4" &] {:any v4-app}
   ["experimental" &] {:any experimental-app}
   ["v1" &] {:any (refuse-retired-api "v1")}
   ["v2" &] {:any (refuse-retired-api "v2")}
   ["v3" &] {:any (refuse-retired-api "v3")}
   [""] {:get (constantly
               (redirect (format "%s/dashboard/index.html" url-prefix)))}))

(defn build-app
  "Generate a Ring application that handles PuppetDB requests

  `globals` is a map containing global state useful
   to request handlers which may contain the following:

  * `authorizer` - a function that takes a request and returns a
    :authorized if the request is authorized, or a user-visible reason if not.
    If not supplied, we default to authorizing all requests."
  [{:keys [authorizer url-prefix] :as globals}]
  (-> (routes url-prefix)
      (wrap-resource "public")
      wrap-params
      (wrap-with-authorization authorizer)
      wrap-with-certificate-cn
      wrap-with-default-body
      (wrap-with-metrics (atom {}) http/leading-uris)
      (wrap-with-globals globals)
      wrap-with-debug-logging))
