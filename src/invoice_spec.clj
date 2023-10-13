(ns invoice-spec
  (:require
   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [clojure.string :as str])
  (:import
   (java.time LocalDate)
   (java.time ZoneId)
   (java.util Date)
   (java.time.format DateTimeFormatter)))

(defn not-blank? [value] (-> value clojure.string/blank? not))
(defn non-empty-string? [x] (and (string? x) (not-blank? x)))

(s/def :customer/name non-empty-string?)
(s/def :customer/email non-empty-string?)
(s/def :invoice/customer (s/keys :req [:customer/name
                                       :customer/email]))

(s/def :invoice/order-reference non-empty-string?)
(s/def :invoice/payment-date inst?)
(s/def :invoice/payment-means non-empty-string?)
(s/def :invoice/payment-means-type non-empty-string?)
(s/def :invoice/number non-empty-string?)
(s/def ::retention ::tax)
(s/def :invoice/retentions (s/coll-of ::retention :kind vector?))

(s/def :tax/rate double?)

(s/def :tax/category #{:iva :ret_fuente :ret_iva}) ; <-- Updated spec

(s/def ::tax (s/keys :req [:tax/category
                           :tax/rate]))
(s/def :invoice-item/taxes (s/coll-of ::tax :kind vector? :min-count 1))

(s/def :invoice-item/price double?)
(s/def :invoice-item/quantity double?)
(s/def :invoice-item/sku non-empty-string?)

(s/def ::invoice-item
  (s/keys :req [:invoice-item/price
                :invoice-item/quantity
                :invoice-item/sku
                :invoice-item/taxes]))

(s/def :invoice/issue-date inst?)
(s/def :invoice/items (s/coll-of ::invoice-item :kind vector? :min-count 1))

(s/def ::invoice
  (s/keys :req [:invoice/issue-date
                :invoice/order-reference
                :invoice/payment-date
                :invoice/payment-means
                :invoice/payment-means-type
                :invoice/number
                :invoice/customer
                :invoice/items
                :invoice/retentions]))

(defn to-date-inst [date-string]
  (let [formatter (DateTimeFormatter/ofPattern "dd/MM/yyyy")]
    (-> (LocalDate/parse date-string formatter)
        (.atStartOfDay (ZoneId/systemDefault))
        (.toInstant)
        (Date/from))))
(defn parse-json [filename]
  (with-open [reader (io/reader filename)]
    (json/read reader :key-fn keyword)))

(defn transform-invoice-from-file [filename]
  (let [parsed-invoice (parse-json filename)
        invoice  (:invoice parsed-invoice)]
    {:invoice/issue-date (to-date-inst (:issue_date invoice))
     :invoice/order-reference (:order_reference invoice)
     :invoice/payment-date (to-date-inst (:payment_date invoice))
     :invoice/payment-means (:payment_means invoice)
     :invoice/payment-means-type (:payment_means_type invoice)
     :invoice/number (:number invoice)
     :invoice/customer {:customer/name (:company_name (:customer invoice))
                        :customer/email (:email (:customer invoice))}
     :invoice/items (vec (map (fn [item]
                                {:invoice-item/price (:price item)
                                 :invoice-item/quantity (:quantity item)
                                 :invoice-item/sku (:sku item)
                                 :invoice-item/taxes (vec (map (fn [tax]
                                                                 {:tax/category (keyword (str/lower-case (:tax_category tax)))
                                                                  :tax/rate (double (:tax_rate tax))})
                                                               (:taxes item)))})

                              (:items invoice)))

     :invoice/retentions (vec (map (fn [retention]
                                     {:tax/category (keyword (str/lower-case (:tax_category retention)))
                                      :tax/rate (:tax_rate retention)})
                                   (:retentions invoice)))}))

(comment

  (let [transformed-invoice (transform-invoice-from-file "invoice.json")]
    (if (s/valid? ::invoice transformed-invoice)
      transformed-invoice
      (do
        (println "Invoice:" transformed-invoice)
        (s/explain ::invoice transformed-invoice)
        (throw (Exception. "The transformed invoice does not match the spec"))))))