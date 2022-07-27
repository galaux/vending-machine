(ns vending-machine.db.core-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [vending-machine.util :refer [user-role-buyer]]
   [luminus-migrations.core :as migrations]
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [vending-machine.config :refer [env]]
   [vending-machine.db.core :as db :refer [*db*]]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'vending-machine.config/env
      #'vending-machine.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-transaction
    [t-conn *db* {:rollback-only true}]
    (is (= 1
           (db/create-user!
             t-conn
             {:id       "1"
              :username "Sam"
              :password "pass"
              :role     user-role-buyer
              :deposit  50}
             {})))
    (is (= {:id       "1"
            :username "Sam"
            :password "pass"
            :role     user-role-buyer
            :deposit  50}
           (db/get-user t-conn {:username "Sam"} {})))))
