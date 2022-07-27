(ns vending-machine.middleware
  (:require
   [buddy.auth.accessrules :refer [error wrap-access-rules]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.core.nonce :refer [random-bytes]]
   [buddy.sign.jwt :as jwt]
   [buddy.sign.util :refer [to-timestamp]]
   [ring.util.response :refer [get-header]]
   [ring.adapter.undertow.middleware.session :refer [wrap-session]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.flash :refer [wrap-flash]]
   [vending-machine.env :refer [defaults]])
  (:import
   (java.util Base64 Calendar Date)))

(def secret
  "Secret used for authentication"
  (random-bytes 32))

(def secret-str
  "Base64 string version of the secret for convenient testing"
  (.encodeToString (Base64/getEncoder) secret))

(defn on-error
  [request response]
  {:status  403
   :headers {}
   :body    (str "Access to " (:uri request) " is not authorized")})

(def ^:private any-access (constantly true))

(def ^:private re-token
  #"^Token (.*)$")

(defn request->token
  [request]
  (some->> (get-header request "Authorization")
           (re-find re-token)
           second))

(defn- validate-token
  [request]
  (when-let [token (request->token request)]
    (try
      (jwt/unsign token secret)
      true
      (catch Exception _
        (error "Bad authentication")))))

(defn- authenticated-user
  [request]
  (or (validate-token request)
      (error "Only authenticated users allowed")))

(defn- if-token-then-validate
  [request]
  (if (request->token request)
    (validate-token request)
    true))

(def ^:private rules
  [{:pattern        #"^/api/login$"
    :request-method :post
    :handler        any-access}
   {:pattern        #"^/api/user$"
    :request-method :post
    :handler        if-token-then-validate}
   {:pattern        #"^/api/product$"
    :request-method :get
    :handler        any-access}
   ;; Just for the purpose of the interview
   {:pattern #"^/api/api-docs/.*"
    :handler any-access}
   {:pattern #"^/api/swagger.json"
    :handler any-access}
   ;; End of "Just for the purpose of the interview"
   {:pattern #"^/api/.*"
    :handler authenticated-user}])

(defn wrap-restricted
  [handler]
  (let [options {:rules rules :on-error on-error}]
    (wrap-access-rules handler options)))

(def token-backend
  (jws-backend {:secret secret}))

(defn token
  [user]
  (let [claims {:user (select-keys user #{:id :username :role})
                :exp  (to-timestamp
                        (.getTime
                          (doto (Calendar/getInstance)
                            (.setTime (Date.))
                            (.add Calendar/HOUR_OF_DAY 1))))}]
    (jwt/sign claims secret)))

(defn wrap-auth
  [handler]
  (let [backend token-backend]
    (-> handler
        wrap-restricted
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-base
  [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))))
