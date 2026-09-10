(ns sportsevent.registry-test
  (:require [clojure.test :refer [deftest is]]
            [sportsevent.registry :as r]))

;; ----------------------------- timing-calibration-overdue? -----------------------------

(deftest not-overdue-when-within-interval
  (is (not (r/timing-calibration-overdue?
            {:days-since-timing-calibration 10 :max-timing-calibration-interval-days 30})))
  (is (not (r/timing-calibration-overdue?
            {:days-since-timing-calibration 30 :max-timing-calibration-interval-days 30}))))

(deftest overdue-when-past-interval
  (is (r/timing-calibration-overdue?
       {:days-since-timing-calibration 45 :max-timing-calibration-interval-days 30})))

(deftest missing-fields-are-not-treated-as-overdue
  (is (not (r/timing-calibration-overdue? {})))
  (is (not (r/timing-calibration-overdue? {:days-since-timing-calibration 45}))))

;; ----------------------------- register-ruling-finalization -----------------------------

(deftest finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-ruling-finalization "participant-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest finalization-assigns-ruling-number
  (let [result (r/register-ruling-finalization "participant-1" "JPN" 7)]
    (is (= (get result "ruling_number") "JPN-RUL-000007"))
    (is (= (get-in result ["record" "participant_id"]) "participant-1"))
    (is (= (get-in result ["record" "kind"]) "ruling-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest finalization-validation-rules
  (is (thrown? Exception (r/register-ruling-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-ruling-finalization "participant-1" "" 0)))
  (is (thrown? Exception (r/register-ruling-finalization "participant-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-ruling-finalization "participant-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-ruling-finalization "participant-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-RUL-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-RUL-000001" (get-in hist2 [1 "record_id"])))))
