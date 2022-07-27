(ns vending-machine.domain.product
  (:require
   [next.jdbc :as jdbc]
   [ring.util.http-response :as resp]
   [vending-machine.db.core :as db]
   [vending-machine.util :refer [rand-str user-role-buyer user-role-seller]]))

(defn by-id
  [{{id :id} :params}]
  (resp/ok (db/get-product {:id id})))

(defn- update-product
  [known-product product_name amount_available cost]
  (let [product (merge
                  known-product
                  (when product_name
                    {:product_name product_name})
                  (when amount_available
                    {:amount_available amount_available})
                  (when cost
                    {:cost cost}))]
    (if (= (db/update-product! product) 1)
      (resp/ok product)
      (resp/internal-server-error))))

(defn- create-product
  [product_name amount_available cost seller-id]
  (let [product {:id           (rand-str)
                 :product_name product_name
                 :amount_available amount_available
                 :cost         cost
                 :seller_id    seller-id}]
    (if (= (db/create-product! product) 1)
      (resp/ok product)
      (resp/internal-server-error))))

(defn upsert
  [request]
  (let [{seller-id :id} (get-in request [:identity :user])
        {seller-id :id seller-role :role :as seller}
        (db/get-user-by-id {:id seller-id})
        {:keys [product_name amount_available cost]} (:body-params request)
        known-product   (db/get-product-by-name {:product_name product_name})]
    (cond
      (nil? seller)
      (resp/forbidden "Unknown user")
      ;;
      (not= seller-role user-role-seller)
      (resp/forbidden "User doesn't have seller role")
      ;;
      (some? known-product)
      (update-product known-product product_name amount_available cost)
      :else
      (create-product product_name amount_available cost seller-id))))

(defn change-for
  [amount]
  (let [one-hundreds (int (/ amount 100))
        fifties      (int (/ (- amount (* one-hundreds 100))
                             50))
        twenties     (int (/ (- amount
                                (+ (* one-hundreds 100)
                                   (* fifties 50)))
                             20))
        dimes        (int (/ (- amount
                                (+ (* one-hundreds 100)
                                   (* fifties 50)
                                   (* twenties 20)))
                             10))
        fivers       (int (/ (- amount
                                (+ (* one-hundreds 100)
                                   (* fifties 50)
                                   (* twenties 20)
                                   (* dimes 10)))
                             5))]
    (concat (repeat one-hundreds 100)
            (repeat fifties 50)
            (repeat twenties 20)
            (repeat dimes 10)
            (repeat fivers 5))))

(defn buy
  [{:keys [body-params] :as request}]
  (jdbc/with-transaction
    [conn db/*db*]
    (let [{user-id :id} (get-in request [:identity :user])
          ;;
          {user-id :id user-role :role deposit :deposit :as user}
          (db/get-user-by-id {:id user-id})
          ;;
          {amount_requested :amount product_id :id} body-params
          ;;
          {:keys [cost amount_available] :as product}
          (db/get-product {:id product_id})]
      (cond
        (nil? user)
        (resp/bad-request "Unknow user")
        ;;
        (not= user-role user-role-buyer)
        (resp/bad-request "User doesn't have buyer role")
        ;;
        (nil? product)
        (resp/bad-request "Unknow product")
        ;;
        (< amount_available amount_requested)
        (resp/bad-request "Not enough product in stock")
        ;;
        (< deposit (* amount_requested cost))
        (resp/bad-request "Insufficient deposit")
        ;;
        :else
        (let [user-res    (db/set-user-deposit! {:id user-id :deposit 0})
              product-res (db/update-product!
                            (update product
                                    :amount_available
                                    #(- % amount_requested)))]
          (if (and (= user-res 1) (= product-res 1))
            (resp/ok
              {:total_spent (* amount_requested cost)
               :purchased   {:product (select-keys product #{:id :product_name})
                             :amount  amount_requested}
               :change      (change-for (- deposit (* amount_requested cost)))})
            (resp/internal-server-error)))))))
