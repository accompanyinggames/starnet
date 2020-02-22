(ns app.main
  (:require [dev.nrepl :refer [start-nrepl-server]]
            [app.core]
            [app.kafka.wordcount-example]
            [app.kafka.streams-example]
            [app.kafka.transit-example]
            [app.kafka.repl-interface]
   ;
            ))

(defn -main  [& args]
  (start-nrepl-server "0.0.0.0" 7788))