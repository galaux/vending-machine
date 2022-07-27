(ns vending-machine.env
  (:require
   [selmer.parser :as parser]
   [clojure.tools.logging :as log]
   [vending-machine.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info
       "\n-=[vending-machine started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[vending-machine has shut down successfully]=-"))
   :middleware wrap-dev})
