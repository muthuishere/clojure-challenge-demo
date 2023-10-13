(ns invoice-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [invoice-spec :as invoice-spec]))

(deftest transform-invoice-from-file-test
  (let [transformed-invoice (invoice-spec/transform-invoice-from-file "invoice.json")]
    (assert (s/valid? ::invoice-spec/invoice transformed-invoice))))
