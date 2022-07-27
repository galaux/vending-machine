(ns vending-machine.domain.user
  (:require
   [next.jdbc :as jdbc]
   [clojure.string :as s]
   [ring.util.http-response :as resp]
   [vending-machine.db.core :as db]
   [vending-machine.middleware :refer [token]]
   [vending-machine.password :as password]
   [vending-machine.util :refer [alphanum? rand-str]]))

(defn get-user
  [username]
  (if-let [user (db/get-user {:username username})]
    (resp/ok (dissoc user :password))
    (resp/bad-request "Unknown user")))

(defn login
  [{{{input-username :username input-password :password} :body} :parameters}]
  (let [user (db/get-user {:username input-username})]
    (if (and (some? user)
             (password/matches? input-password (:password user)))
      (resp/ok {:token (token (dissoc user :password))})
      (resp/unauthorized))))

(defn modify
  [username {:keys [role]}]
  (if (not (nat-int? role))
    (resp/bad-request "Bad input")
    (let [user {:username username
                :role     role}]
      (if (= (db/update-user! user) 1)
        (resp/ok (dissoc user :password))
        (resp/internal-server-error)))))

(defn create
  [{:keys [username password role]}]
  (cond
    (or (not (alphanum? username))
        (s/blank? password)
        (not (nat-int? role)))
    (resp/bad-request "Bad input")
    ;;
    (not (password/strong? password))
    (resp/bad-request "Weak password")
    ;;
    :else
    (let [user (-> {:id       (rand-str)
                    :username username
                    :password (password/encode password)
                    :role     role
                    :deposit  0})]
      (if (= (db/create-user! user) 1)
        (resp/ok (dissoc user :password))
        (resp/internal-server-error)))))

(defn deposit
  [request]
  (let [coin    (get-in request [:body-params :coin])
        user-id (get-in request [:identity :user :id])]
    (jdbc/with-transaction
      [conn db/*db*]
      (let [{:keys [deposit] :as user} (db/get-user-by-id {:id user-id})]
        (if (nil? user)
          (resp/bad-request "Unknow user")
          (let [result (db/set-user-deposit!
                         {:id      user-id
                          :deposit (+ deposit coin)})]
            (if (= result 1)
              (resp/ok (dissoc (db/get-user-by-id {:id user-id}) :password))
              (resp/internal-server-error))))))))

(defn reset
  [request]
  (let [user-id (get-in request [:identity :user :id])
        result  (db/set-user-deposit!
                  {:id      user-id
                   :deposit 0})]
    (if (= result 1)
      (resp/ok (dissoc (db/get-user-by-id {:id user-id}) :password))
      (resp/internal-server-error))))
