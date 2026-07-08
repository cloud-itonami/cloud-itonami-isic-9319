(ns sportsevent.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [sportsevent.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Kenji" (:participant-name (store/participant s "participant-1"))))
      (is (= "JPN" (:jurisdiction (store/participant s "participant-1"))))
      (is (= 10 (:days-since-timing-calibration (store/participant s "participant-1"))))
      (is (false? (:anti-doping-control-unresolved? (store/participant s "participant-1"))))
      (is (= 45 (:days-since-timing-calibration (store/participant s "participant-3"))))
      (is (true? (:anti-doping-control-unresolved? (store/participant s "participant-4"))))
      (is (false? (:ruling-finalized? (store/participant s "participant-1"))))
      (is (= ["participant-1" "participant-2" "participant-3" "participant-4"]
             (mapv :id (store/all-participants s))))
      (is (nil? (store/antidoping-screen-of s "participant-1")))
      (is (nil? (store/ruling-of s "participant-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/ruling-history s)))
      (is (zero? (store/next-sequence s "JPN")))
      (is (false? (store/participant-already-finalized? s "participant-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :participant/upsert
                                 :value {:id "participant-1" :participant-name "Sato Kenji"}})
        (is (= "Sato Kenji" (:participant-name (store/participant s "participant-1"))))
        (is (= 10 (:days-since-timing-calibration (store/participant s "participant-1"))) "unrelated field preserved"))
      (testing "ruling / antidoping payloads commit and read back"
        (store/commit-record! s {:effect :ruling/set :path ["participant-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/ruling-of s "participant-1")))
        (store/commit-record! s {:effect :antidoping/set :path ["participant-1"]
                                 :payload {:participant-id "participant-1" :anti-doping-control-unresolved? false}})
        (is (= {:participant-id "participant-1" :anti-doping-control-unresolved? false} (store/antidoping-screen-of s "participant-1"))))
      (testing "ruling finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :participant/mark-finalized :path ["participant-1"]})
        (is (= "JPN-RUL-000000" (get (first (store/ruling-history s)) "record_id")))
        (is (= "ruling-finalization-draft" (get (first (store/ruling-history s)) "kind")))
        (is (true? (:ruling-finalized? (store/participant s "participant-1"))))
        (is (= 1 (count (store/ruling-history s))))
        (is (= 1 (store/next-sequence s "JPN")))
        (is (true? (store/participant-already-finalized? s "participant-1")))
        (is (false? (store/participant-already-finalized? s "participant-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/participant s "nope")))
    (is (= [] (store/all-participants s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/ruling-history s)))
    (is (zero? (store/next-sequence s "JPN")))
    (store/with-participants s {"x" {:id "x" :participant-name "n"
                                     :days-since-timing-calibration 10 :max-timing-calibration-interval-days 30
                                     :anti-doping-control-unresolved? false
                                     :ruling-finalized? false :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:participant-name (store/participant s "x"))))))
