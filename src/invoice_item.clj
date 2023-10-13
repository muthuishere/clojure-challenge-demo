(ns invoice-item)

(def TAX_RATE 19)
(def RETENTION_RATE 1)
(defn- discount-factor [{:invoice-item/keys [discount-rate]
                         :or                {discount-rate 0}}]
  (- 1 (/ discount-rate 100.0)))

(defn subtotal
  [{:invoice-item/keys [precise-quantity precise-price discount-rate]
    :as                item
    :or                {discount-rate 0}}]
  (* precise-price precise-quantity (discount-factor item)))

;Given the function subtotal defined in invoice-item.clj in this repo, write at least five tests using clojure core deftest that demonstrates its correctness. This subtotal function calculates the subtotal of an invoice-item taking a discount-rate into account. Make sure the tests cover as many edge cases as you can!

;Problem 1

(defn xor [a b]
  (not= a b))
(defn has-tax? [item tax-rate]
  (some #(and (= (:tax/category %) :iva)
              (= (:tax/rate %) tax-rate))
        (:taxable/taxes item)))

(defn has-retention? [item  retention-rate]
  (some #(and (= (:retention/category %) :ret_fuente)
              (= (:retention/rate %) retention-rate))
        (:retentionable/retentions item)))

(defn has-only-tax-or-retention? [item  tax-rate  retention-rate]
  (xor (has-tax? item  tax-rate)
       (has-retention? item  retention-rate)))

(defn filter-items-by-tax-and-retention [invoice]
  (let [items (:invoice/items invoice)]
    (->> items
         (filter #(has-only-tax-or-retention? %1  TAX_RATE  RETENTION_RATE))
         (seq)
         (when (and (some #(has-tax? %  TAX_RATE) items)
                    (some #(has-retention? %  RETENTION_RATE) items))))))

(comment

  (def invoice (clojure.edn/read-string (slurp "invoice.edn")))

  (filter-items-by-tax-and-retention  invoice))