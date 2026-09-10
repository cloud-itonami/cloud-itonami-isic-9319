(ns sportsevent.phase
  "Phase 0->3 staged rollout -- the sports-event analog of
  `cloud-itonami-isic-8620`'s `clinic.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- participant intake allowed, every
                                 write needs human approval.
    Phase 2  assisted-verify  -- adds ruling verification + anti-
                                 doping-control screening writes,
                                 still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:participant/intake` (no capital
                                 risk yet) may auto-commit.
                                 `:actuation/finalize-ruling` NEVER
                                 auto-commits, at any phase.

  `:actuation/finalize-ruling` is deliberately ABSENT from every
  phase's `:auto` set, including phase 3 -- a permanent structural
  fact, not a rollout milestone still to come. Finalizing a real
  official ruling or result is the ONE real-world act this actor
  performs; it is always a human licensed-official call.
  `sportsevent.governor`'s `:actuation/finalize-ruling` high-stakes
  gate enforces the same invariant independently -- two layers, not
  one, agree on this. `:antidoping/screen` is likewise never auto-
  eligible, at any phase -- the same posture every sibling's
  screening op has. Phase 3's `:auto` set here has only ONE member
  (`:participant/intake`) -- this domain has no separate no-capital-
  risk 'file' lifecycle distinct from the participant itself.")

(def read-ops  #{})
(def write-ops #{:participant/intake :ruling/verify :antidoping/screen
                 :actuation/finalize-ruling})

;; NOTE the invariant: `:actuation/finalize-ruling` is a member of
;; `write-ops` (governor-gated like any write) but is NEVER a member
;; of any phase's `:auto` set below. Do not add it there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                       :auto #{}}
   1 {:label "assisted-intake"  :writes #{:participant/intake}                                     :auto #{}}
   2 {:label "assisted-verify"  :writes #{:participant/intake :ruling/verify :antidoping/screen}    :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:participant/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/finalize-ruling` is never auto-eligible at any phase,
    so it always escalates once the governor clears it (or holds if
    the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map an Event Integrity Governor verdict to a base disposition
  before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
