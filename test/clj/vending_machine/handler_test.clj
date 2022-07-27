(ns vending-machine.handler-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [luminus-migrations.core :as migrations]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [ring.mock.request :refer [header json-body request]]
   [vending-machine.config :refer [env]]
   [vending-machine.db.core :as db]
   [vending-machine.handler :refer [app]]
   [vending-machine.middleware :refer [token]]
   [vending-machine.middleware.formats :as formats]
   [vending-machine.password]))

(defn parse-json
  [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'vending-machine.config/env
                 #'vending-machine.handler/app-routes
                 #'vending-machine.db.core/*db*
                 #'vending-machine.password/encoder
                 #'vending-machine.password/strength-checker)
    (f)))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(use-fixtures
  :each
  (fn [f]
    (reset-db)
    (f)
    (reset-db)))

(deftest base-edge-cases

  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 301 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest user

  (testing "weak password"
    (let [rq (-> (request :post "/api/user")
                 (json-body {:username "rick"
                             :password "1234"
                             :role     1}))
          {:keys [status body]} ((app) rq)]
      (is (= 400 status))
      (is (= "Weak password" body))))

  (testing "create user OK"
    (let [rq (-> (request :post "/api/user")
                 (json-body {:username "Rick"
                             :password "X6q9zQADffGVPuuiZhvk"
                             :role     1}))
          {:keys [status] :as response} ((app) rq)]
      (is (= 200 status))
      (is (= {:username "Rick" :role 1 :deposit 0}
             (dissoc (m/decode-response-body response) :id)))))

  (testing "update user OK"
    (let [rq (-> (request :post "/api/user")
                 (header "Authorization"
                         (str "Token " (token {:username "Rick" :role 0})))
                 (json-body {:role 0}))
          {:keys [status] :as response} ((app) rq)]
      (is (= 200 status))
      (is (= {:username "Rick" :role 0}
             (m/decode-response-body response))))))

(deftest integration-test
  ;; Seller creates profile
  (let [rq     (-> (request :post "/api/user")
                   (json-body {:username "Rick"
                               :password "X6q9zQADffGVPuuiZhvk"
                               :role     1}))
        {:keys [status] :as response} ((app) rq)
        seller (m/decode-response-body response)]
    (is (= 200 status))
    (is (= {:username "Rick" :role 1 :deposit 0}
           (dissoc seller :id)))
    ;; Seller logs in
    (let [rq (-> (request :post "/api/login")
                 (json-body {:username "Rick"
                             :password "X6q9zQADffGVPuuiZhvk"}))
          {:keys [status] :as response} ((app) rq)
          seller-token (:token (m/decode-response-body response))]
      (is (= 200 status))
      (is (some? seller-token))
      ;; Seller creates product
      (let [rq (-> (request :post "/api/product")
                   (header "Authorization" (str "Token " seller-token))
                   (json-body {:product_name "almond"
                               :amount_available 10
                               :cost         25}))
            {:keys [status] :as response} ((app) rq)
            product-almond (m/decode-response-body response)]
        (is (= 200 status))
        (is (= {:seller_id        (:id seller)
                :amount_available 10
                :cost             25
                :product_name     "almond"}
               (dissoc product-almond :id)))
        ;; Buyer creates profile
        (let [rq    (-> (request :post "/api/user")
                        (json-body {:username "Morty"
                                    :password "r2l1ChtUfP4kR6d3WX9m"
                                    :role     0}))
              {:keys [status] :as response} ((app) rq)
              buyer (m/decode-response-body response)]
          (is (= 200 status))
          (is (= {:username "Morty" :role 0 :deposit 0}
                 (dissoc buyer :id)))
          ;; Buyer logs in
          (let [rq (-> (request :post "/api/login")
                       (json-body {:username "Morty"
                                   :password "r2l1ChtUfP4kR6d3WX9m"}))
                {:keys [status] :as response} ((app) rq)
                buyer-token (:token (m/decode-response-body response))]
            (is (= 200 status))
            (is (some? buyer-token))
            ;; Buyer deposits 100
            (let [rq         (-> (request :post "/api/deposit")
                                 (header "Authorization"
                                         (str "Token " buyer-token))
                                 (json-body {:coin 100}))
                  {:keys [status] :as response} ((app) rq)
                  buyer-info (m/decode-response-body response)]
              (is (= 200 status))
              (is (= {:id (:id buyer) :username "Morty" :role 0 :deposit 100}
                     buyer-info)))
            ;; Seller tries to buy
            (let [rq (-> (request :post "/api/buy")
                         (header "Authorization"
                                 (str "Token " seller-token))
                         (json-body {:id     (:id product-almond)
                                     :amount 2}))
                  {:keys [status body]} ((app) rq)]
              (is (= 400 status))
              (is (= "User doesn't have buyer role" body)))
            ;; Buyer tries to buy more than available
            (let [rq (-> (request :post "/api/buy")
                         (header "Authorization"
                                 (str "Token " buyer-token))
                         (json-body {:id     (:id product-almond)
                                     :amount 150}))
                  {:keys [status body]} ((app) rq)]
              (is (= 400 status))
              (is (= "Not enough product in stock" body)))
            ;; Buyer tries to buy but not enough cash
            (let [rq (-> (request :post "/api/buy")
                         (header "Authorization"
                                 (str "Token " buyer-token))
                         (json-body {:id     (:id product-almond)
                                     :amount 5}))
                  {:keys [status body]} ((app) rq)]
              (is (= 400 status))
              (is (= "Insufficient deposit" body)))
            ;; Buyer successfully buys product
            (let [rq         (-> (request :post "/api/buy")
                                 (header "Authorization"
                                         (str "Token " buyer-token))
                                 (json-body {:id     (:id product-almond)
                                             :amount 3}))
                  {:keys [status] :as response} ((app) rq)
                  buyer-info (m/decode-response-body response)]
              (is (= 200 status))
              (is (= {:total_spent 75
                      :purchased
                      {:amount  3
                       :product
                       {:id (:id product-almond)
                        :product_name "almond"}}
                      :change      [20 5]}
                     buyer-info)))
            ;; Buyer profile is correct
            (let [rq         (-> (request :get "/api/user")
                                 (header "Authorization"
                                         (str "Token " buyer-token)))
                  {:keys [status] :as response} ((app) rq)
                  buyer-info (m/decode-response-body response)]
              (is (= 200 status))
              (is (= {:id       (:id buyer)
                      :username "Morty"
                      :role     0
                      :deposit  0}
                     buyer-info)))))))))

