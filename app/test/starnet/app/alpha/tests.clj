(ns starnet.app.alpha.tests
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test :refer [is run-all-tests testing deftest run-tests] :as t]

   [starnet.app.alpha.spec]
   [starnet.common.alpha.spec]

   [starnet.common.sample-tests]
   [starnet.common.alpha.tests]
   [starnet.app.sample-tests]
   [starnet.app.alpha.streams.game-test]
   [starnet.app.alpha.streams.user-test]
   [starnet.app.alpha.streams.core-test]
   [starnet.app.alpha.spec-test]
   [starnet.common.alpha.core :refer [rand-uuid]]))

(defn -main []
  (run-all-tests #"app.+tests?$|common.+tests?$")
  (System/exit 0))

(comment

  (stest/check)
  (tc/quick-check)

  (run-tests)
  (run-tests
   'starnet.common.sample-tests
   'starnet.app.sample-tests)

  ;;
  )

(deftest common-deps
  (testing "generating random uuid via reader conditionals in .cljc"
    (is (uuid? (rand-uuid)))))