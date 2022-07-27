(ns vending-machine.routes.services
  (:require
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [vending-machine.domain.product :as product]
   [vending-machine.domain.user :as user]
   [vending-machine.middleware.formats :as formats]
   [vending-machine.util :refer
    [accepted-coin? alphanum? nat-int-multiple-of-5? non-blank?]]))

(def ^:private login
  {:summary    "Return a token if specified credentials are correct"
   :parameters {:body {:username alphanum?
                       :password non-blank?}}
   :handler    user/login})

(def ^:private get-user
  {:summary "Return the logged user details"
   :handler (fn [{{{:keys [username]} :user} :identity}]
              (user/get-user username))})

(def ^:private upsert-user
  {:summary
   "If credentials are provided, update logged user, create it otherwise"
   :handler (fn [{:keys [body-params identity]}]
              (if-let [username (get-in identity [:user :username])]
                (user/modify username body-params)
                (user/create body-params)))})

(def ^:private get-product
  {:summary    "Return product detail"
   :parameters {:query {:id alphanum?}}
   :handler    product/by-id})

(def ^:private upsert-product
  {:summary    "Upsert a product"
   :parameters {:body {:product_name non-blank?
                       :amount_available nat-int?
                       :cost         nat-int-multiple-of-5?}}
   :handler    product/upsert})

(def ^:private deposit
  {:summary    "Deposit a coin on logged user account"
   :parameters {:body {:coin accepted-coin?}}
   :handler    user/deposit})

(def ^:private reset
  {:summary "Reset logged user deposit"
   :handler user/reset})

(def ^:private buy
  {:summary    "Enable logged user to buy a product"
   :parameters {:body {:id     alphanum?
                       :amount nat-int?}}
   :handler    product/buy})

(defn service-routes
  []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id   ::api
                 :tags ["API"]}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   [""
    {:no-doc  true
     :swagger {:info {:title       "Vending machine"
                      :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/login"
    {:post login}]

   ["/user"
    {:get  get-user
     :post upsert-user}]

   ["/deposit"
    {:post deposit}]

   ["/reset"
    {:post reset}]

   ["/buy"
    {:post buy}]

   ["/product"
    {:get  get-product
     :post upsert-product}]])
