(ns vending-machine.util
  (:require
   [clojure.string :as s]))

(def user-role-buyer 0)
(def user-role-seller 1)

(defn alphanum?
  [s]
  (when s
    (some? (re-matches #"^[A-Za-z0-9]+" s))))

(defn non-blank?
  [s]
  ((complement s/blank?) s))

(defn nat-int-multiple-of-5?
  [i]
  (and (nat-int? i)
       (= (mod i 5) 0)))

(defn accepted-coin?
  [coin]
  (some? (#{100 50 20 10 5} coin)))

(defn rand-str
  []
  (->> #(char (+ (rand 26) 65))
       repeatedly
       (take 20)
       (apply str)))
