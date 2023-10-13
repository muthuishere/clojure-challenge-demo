(ns invoice-item-test
  (:require [clojure.test :refer :all]
            [invoice-item :refer [filter-items-by-tax-and-retention subtotal]]))

(deftest filter-items-by-tax-and-retention-test
  (let [filename "invoice.edn"
        expected-ids ["ii3" "ii4"]
        actual-ids (->>  (clojure.edn/read-string (slurp filename))
                         filter-items-by-tax-and-retention
                         (map :invoice-item/id))]
    (assert (= (count expected-ids) (count actual-ids)))

    (assert (every? #(some #{%} actual-ids) expected-ids))))

(deftest test-subtotal-with-different-scenarios

  (testing "Subtotal with precise-quantity, precise-price, and discount-rate"
    (is (= 90.0 (subtotal {:invoice-item/precise-quantity 10
                           :invoice-item/precise-price 10
                           :invoice-item/discount-rate 10})))) ; 10 items * 10 price * 0.9 (because of 10% discount) = 90

  (testing "Subtotal with precise-quantity and precise-price (default discount)"
    (is (= 100.0 (subtotal {:invoice-item/precise-quantity 10
                            :invoice-item/precise-price 10})))) ; 10 items * 10 price = 100

  (testing "Subtotal with 100% discount-rate"
    (is (= 0.0 (subtotal {:invoice-item/precise-quantity 10
                          :invoice-item/precise-price 10
                          :invoice-item/discount-rate 100})))) ; 10 items * 10 price * 0 (because of 100% discount) = 0

  (testing "Subtotal with precise-quantity or precise-price as 0"
    (is (= 0.0 (subtotal {:invoice-item/precise-quantity 0
                          :invoice-item/precise-price 10})))
    (is (= 0.0 (subtotal {:invoice-item/precise-quantity 10
                          :invoice-item/precise-price 0}))))

  (testing "Subtotal with negative precise-quantity or precise-price"
    (is (= -90.0 (subtotal {:invoice-item/precise-quantity -10
                            :invoice-item/precise-price 10
                            :invoice-item/discount-rate 10})))
    (is (= -90.0 (subtotal {:invoice-item/precise-quantity 10
                            :invoice-item/precise-price -10
                            :invoice-item/discount-rate 10})))))
