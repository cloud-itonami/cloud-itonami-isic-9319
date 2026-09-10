(ns sportsevent.governor-contract-test
  "The governor contract as executable tests -- the sports-event
  analog of `cloud-itonami-isic-6512`'s `casualty.governor-contract-
  test`. The single invariant under test:

    SportsEventOps-LLM never finalizes a ruling the Event Integrity
    Governor would reject, `:actuation/finalize-ruling` NEVER auto-
    commits at any phase, `:participant/intake` (no direct capital
    risk) MAY auto-commit when clean, and every decision (commit OR
    hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [sportsevent.store :as store]
            [sportsevent.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :licensed-official :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a ruling
  evidence checklist on file. Uses distinct thread-ids per call site
  by suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :ruling/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :participant/intake :subject "participant-1"
                   :patch {:id "participant-1" :participant-name "Sato Kenji"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Kenji" (:participant-name (store/participant db "participant-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest ruling-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :ruling/verify :subject "participant-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/ruling-of db "participant-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a ruling/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :ruling/verify :subject "participant-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/ruling-of db "participant-1")) "no ruling written"))))

(deftest finalize-ruling-without-ruling-verify-is-held
  (testing "actuation/finalize-ruling before any ruling verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/finalize-ruling :subject "participant-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest timing-calibration-overdue-is-held
  (testing "a participant whose own recorded timing calibration exceeds its own recorded maximum interval -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "participant-3")
          res (exec-op actor "t5" {:op :actuation/finalize-ruling :subject "participant-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:timing-calibration-overdue} (-> (store/ledger db) last :basis)))
      (is (empty? (store/ruling-history db))))))

(deftest anti-doping-control-unresolved-is-held-and-unoverridable
  (testing "an unresolved anti-doping-control concern on a participant -> HOLD, and never reaches request-approval -- exercised via :antidoping/screen DIRECTLY, not via the actuation op against an unscreened participant (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's, research's, design's, nursing's, sports's, alliedhealth's, laundry's, holdco's, photo's, personalservice's, edsupport's, headoffice's, residential's, cultural's, reserve's and proserv's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :antidoping/screen :subject "participant-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:anti-doping-control-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/antidoping-screen-of db "participant-4")) "no clearance written"))))

(deftest finalize-ruling-always-escalates-then-human-decides
  (testing "a clean, fully-assessed participant still ALWAYS interrupts for human approval -- actuation/finalize-ruling is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "participant-1")
          r1 (exec-op actor "t7" {:op :actuation/finalize-ruling :subject "participant-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, ruling-finalization record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:ruling-finalized? (store/participant db "participant-1"))))
          (is (= 1 (count (store/ruling-history db))) "one draft finalization record"))))))

(deftest double-finalization-is-held
  (testing "finalizing the same participant's ruling twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "participant-1")
          _ (exec-op actor "t8a" {:op :actuation/finalize-ruling :subject "participant-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/finalize-ruling :subject "participant-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-finalized} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/ruling-history db))) "still only the one earlier finalization"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :participant/intake :subject "participant-1"
                          :patch {:id "participant-1" :participant-name "Sato Kenji"}} operator)
      (exec-op actor "b" {:op :ruling/verify :subject "participant-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
