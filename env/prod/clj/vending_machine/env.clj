(ns vending-machine.env
  (:require
   [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[vending-machine started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[vending-machine has shut down successfully]=-"))
   :middleware identity})
