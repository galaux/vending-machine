(ns vending-machine.password
  (:require
   [mount.core :as mount])
  (:import
   (org.springframework.security.crypto.argon2 Argon2PasswordEncoder)
   (me.gosimple.nbvcxz Nbvcxz)))

(mount/defstate encoder
  :start
  (Argon2PasswordEncoder.))

(defn encode
  [s]
  (.encode encoder s))

(defn matches?
  [s encoded]
  (when s
    (.matches encoder s encoded)))


(mount/defstate strength-checker
  :start
  (Nbvcxz.))

(defn strong?
  [p]
  (when p
    (.isMinimumEntropyMet (.estimate strength-checker p))))
