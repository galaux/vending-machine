(ns vending-machine.domain.product-test
  (:require
   [vending-machine.domain.product :as sut]
   [clojure.test :refer [is deftest]]))

(deftest change-for
  (is (= [] (sut/change-for 0)))
  (is (= [5] (sut/change-for 5)))
  (is (= [10] (sut/change-for 10)))
  (is (= [10 5] (sut/change-for 15)))
  (is (= [50 5] (sut/change-for 55)))
  (is (= [100 50 20 10 5] (sut/change-for 185))))
