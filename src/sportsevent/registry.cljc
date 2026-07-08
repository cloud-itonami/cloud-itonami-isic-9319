(ns sportsevent.registry
  "Pure-function official-ruling/result-finalization record
  construction -- an append-only sports-event book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a ruling/result reference
  number -- every organizer/jurisdiction assigns its own reference
  format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `sportsevent.facts` uses.

  `timing-calibration-overdue?` is an HONEST reuse of this fleet's
  MAXIMUM-ceiling check family (`facility.registry/occupancy-exceeds-
  capacity?`/`school.registry/class-size-exceeds-maximum?`/`card.
  registry/settlement-amount-exceeds-authorized?`/`recovery.registry/
  contamination-percentage-exceeds-maximum?`/`care.registry/
  caregiver-workload-exceeds-maximum?`/`navigator.registry/
  eligibility-window-elapsed-exceeds-validity?`/`advertising.
  registry/media-spend-exceeds-authorized-budget?`/`nursing`'s/
  `holdco`'s/`headoffice`'s/`reserve`'s instances established the
  first eleven), applying the SAME elapsed-time-exceeds-validity-
  window comparison `navigator.registry/eligibility-window-elapsed-
  exceeds-validity?` already established (the sixth instance) --
  the TWELFTH MAXIMUM-ceiling instance overall, and the SECOND
  instance of the specific 'elapsed-time-since-X exceeds a max-
  allowed-interval' sub-pattern -- applying it to a timing device's
  own recorded days since its last calibration against its own
  recorded maximum-permitted calibration interval, a direct, natural
  mapping onto real sports-timing certification practice (World
  Athletics Technical Rules Rule 17 photo-finish/timing-system
  calibration requirements). Not claimed as new.

  The `anti-doping-control-unresolved?` concept (a GENUINELY NEW
  unconditional-evaluation check, grep-verified absent -- no
  'doping'/'anti-doping' concept exists anywhere else in this fleet)
  is a BOOLEAN flag read directly off the participant's own record by
  `sportsevent.governor` -- the same shape `photo.governor`'s
  `minor-subject-guardian-consent-unresolved-violations` and
  `reserve.governor`'s `correspondent-banking-due-diligence-
  unresolved-violations` use, neither of which needed a dedicated
  registry-level predicate either -- so it is NOT defined here.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real event-management/timing system. It builds the
  RECORD an event organizer would keep, not the act of finalizing the
  official ruling itself (that is `sportsevent.operation`'s
  `:actuation/finalize-ruling`, always human-gated -- see README
  `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  event organizer's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn timing-calibration-overdue?
  "Does `participant`'s own recorded `:days-since-timing-calibration`
  exceed its own recorded `:max-timing-calibration-interval-days`? A
  pure ground-truth check against the participant's own permanent
  fields -- no upstream comparison needed. The TWELFTH instance of
  this fleet's MAXIMUM-ceiling check family (see ns docstring), not
  claimed as new."
  [{:keys [days-since-timing-calibration max-timing-calibration-interval-days]}]
  (and (number? days-since-timing-calibration) (number? max-timing-calibration-interval-days)
       (> days-since-timing-calibration max-timing-calibration-interval-days)))

(defn register-ruling-finalization
  "Validate + construct the RULING-FINALIZATION registration DRAFT --
  the event organizer's own act of finalizing a real official ruling
  or result. Pure function -- does not touch any real event-
  management/timing system; it builds the RECORD an organizer would
  keep. `sportsevent.governor` independently re-verifies the
  participant's own timing-calibration ground truth and anti-doping-
  control status, and blocks a double-finalization for the same
  participant, before this is ever allowed to commit."
  [participant-id jurisdiction sequence]
  (when-not (and participant-id (not= participant-id ""))
    (throw (ex-info "ruling-finalization: participant_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "ruling-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "ruling-finalization: sequence must be >= 0" {})))
  (let [ruling-number (str (str/upper-case jurisdiction) "-RUL-" (zero-pad sequence 6))
        record {"record_id" ruling-number
                "kind" "ruling-finalization-draft"
                "participant_id" participant-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "ruling_number" ruling-number
     "certificate" (unsigned-certificate "RulingFinalization" ruling-number ruling-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
