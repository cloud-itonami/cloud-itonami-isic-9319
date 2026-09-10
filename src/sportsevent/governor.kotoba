(ns sportsevent.governor
  "Event Integrity Governor -- the independent compliance layer that
  earns the SportsEventOps-LLM the right to commit. The LLM has no
  notion of jurisdictional sports-officiating/anti-doping law,
  whether a participant's own recorded anti-doping-control concern
  has actually stayed unresolved, whether a participant's own
  recorded timing-calibration status actually stays within its own
  recorded maximum interval, or when an act stops being a draft and
  becomes a real-world official-ruling/result finalization, so this
  MUST be a separate system able to *reject* a proposal and fall back
  to HOLD -- the sports-event analog of `cloud-itonami-isic-8620`'s
  ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  unresolved anti-doping-control concern, an overdue timing
  calibration, or a double finalization). The confidence/actuation
  gate is SOFT: it asks a human to look (low confidence / actuation),
  and the human may approve -- but see `sportsevent.phase`: for
  `:stake :actuation/finalize-ruling` (a real official-ruling/result
  finalization) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the ruling proposal cite an
                                       OFFICIAL source (`sportsevent.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       ruling`, has the participant
                                       actually been assessed with a
                                       full participant-registration-
                                       consent-record/event-schedule-
                                       record/anti-doping-control-
                                       record/timing-calibration-
                                       record evidence checklist on
                                       file?
    3. Anti-doping control
       unresolved                    -- reported by THIS proposal
                                       itself (an `:antidoping/screen`
                                       that just found an unresolved
                                       concern), or already on file for
                                       the participant (`:antidoping/
                                       screen`/`:actuation/finalize-
                                       ruling`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op) so the screening op
                                       itself can HARD-hold on its own
                                       finding. A GENUINELY NEW concept
                                       in this fleet (grep-verified
                                       absent -- no dedicated anti-
                                       doping CHECK FUNCTION exists
                                       anywhere else in this fleet),
                                       the 56th distinct application of
                                       the unconditional-evaluation
                                       discipline overall (`casualty.
                                       governor/sanctions-violations`'s
                                       original fix; most recently
                                       `proserv.governor/chain-of-
                                       title-unresolved-violations` at
                                       55th). Grounded in real anti-
                                       doping law (WADA Code Article
                                       5/7 testing and results
                                       management, requiring a mandatory
                                       doping-control resolution before
                                       an official result can be
                                       ratified).
    4. Timing calibration overdue  -- for `:actuation/finalize-ruling`,
                                       INDEPENDENTLY recompute whether
                                       the participant's own recorded
                                       days since timing calibration
                                       exceed its own recorded maximum-
                                       permitted calibration interval
                                       (`sportsevent.registry/timing-
                                       calibration-overdue?`) -- needs
                                       no proposal inspection at all.
                                       An HONEST reuse of this fleet's
                                       MAXIMUM-ceiling check family
                                       (the TWELFTH instance, and the
                                       SECOND instance of `navigator.
                                       registry/eligibility-window-
                                       elapsed-exceeds-validity?`'s own
                                       elapsed-time-exceeds-validity-
                                       window sub-pattern), grounded in
                                       real sports-timing certification
                                       practice (World Athletics
                                       Technical Rules Rule 17), not
                                       claimed as new.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-ruling` (a REAL
                                       official-ruling/result
                                       finalization) -> escalate.

  One more guard, double-finalization prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-finalized-violations` refuses to
  finalize a ruling for the SAME participant twice, off a dedicated
  `:ruling-finalized?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [sportsevent.facts :as facts]
            [sportsevent.registry :as registry]
            [sportsevent.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real official ruling or result is the ONE real-world
  actuation event this actor performs -- a single-member set,
  matching `cloud-itonami-isic-6511`'s/`6621`'s/`6629`'s/`6612`'s/
  `6492`'s/`7120`'s/`8620`'s/`proserv`'s single-actuation shape."
  #{:actuation/finalize-ruling})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:ruling/verify` (or `:actuation/finalize-ruling`) proposal with
  no spec-basis citation is a HARD violation -- never invent a
  jurisdiction's sports-officiating/anti-doping requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:ruling/verify :actuation/finalize-ruling} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は裁定基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-ruling`, the jurisdiction's required
  participant-registration-consent-record/event-schedule-record/anti-
  doping-control-record/timing-calibration-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-ruling)
    (let [p (store/participant st subject)
          ruling (store/ruling-of st subject)]
      (when-not (and ruling
                     (facts/required-evidence-satisfied?
                      (:jurisdiction p) (:checklist ruling)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(参加者登録同意記録/競技日程記録/ドーピング検査記録/計時機器較正記録等)が充足していない状態での提案"}]))))

(defn- anti-doping-control-unresolved-violations
  "An unresolved anti-doping-control concern -- reported by THIS
  proposal (e.g. an `:antidoping/screen` that itself just found an
  unresolved concern), or already on file in the store for the
  participant (`:antidoping/screen`/`:actuation/finalize-ruling`) --
  is a HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not
  scoped to a specific op) so the screening op itself can HARD-hold
  on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (true? (get-in proposal [:value :anti-doping-control-unresolved?]))
        participant-id (when (contains? #{:antidoping/screen :actuation/finalize-ruling} op) subject)
        hit-on-file? (and participant-id (true? (:anti-doping-control-unresolved? (store/participant st participant-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :anti-doping-control-unresolved
        :detail "ドーピング検査結果が未解決の状態での裁定確定提案は進められない"}])))

(defn- timing-calibration-overdue-violations
  "For `:actuation/finalize-ruling`, INDEPENDENTLY recompute whether
  the participant's own recorded days since timing calibration exceed
  its own recorded maximum-permitted calibration interval via
  `sportsevent.registry/timing-calibration-overdue?` -- needs no
  proposal inspection at all."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-ruling)
    (let [p (store/participant st subject)]
      (when (registry/timing-calibration-overdue? p)
        [{:rule :timing-calibration-overdue
          :detail (str subject " の計時機器較正経過日数(" (:days-since-timing-calibration p)
                      ")が較正有効期間(" (:max-timing-calibration-interval-days p) ")を超過")}]))))

(defn- already-finalized-violations
  "For `:actuation/finalize-ruling`, refuses to finalize a ruling for
  the SAME participant twice, off a dedicated `:ruling-finalized?`
  fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-ruling)
    (when (store/participant-already-finalized? st subject)
      [{:rule :already-finalized
        :detail (str subject " は既に裁定確定済み")}])))

(defn check
  "Censors a SportsEventOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (anti-doping-control-unresolved-violations request proposal st)
                           (timing-calibration-overdue-violations request st)
                           (already-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
