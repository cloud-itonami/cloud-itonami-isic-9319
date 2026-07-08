# ADR-0001: SportsEventOps-LLM ⊣ Event Integrity Governor architecture

## Status

Accepted. `cloud-itonami-isic-9319` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-9319` publishes an OSS business blueprint for
other sports activities not elsewhere classified: independent race
organizing, sports officiating and event-timing services. Like every
prior actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph
StateGraph + independent Governor + Phase 0→3 rollout pattern
established by `cloud-itonami-isic-6511` (life insurance) and applied
across seventy prior siblings, most recently `cloud-itonami-isic-
7490` (other professional/scientific/technical n.e.c.).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README/business-model.md/operator-guide.md
consistently name only ONE real-world act: "finalizing an official
ruling or result." Matching `leasing`/`underwriting`/`testlab`/
`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/`entertainment`/
`facility`/`consulting`/`advertising`/`polling`/`research`/`design`/
`sports`/`alliedhealth`/`photo`/`personalservice`/`edsupport`/
`cultural`/`proserv`'s single-actuation shape, `high-stakes` here is
a one-member set, `#{:actuation/finalize-ruling}`.

### Decision 2: entity and op shape

The primary entity is a `participant`, matching the business-
model.md's own Offer language ("event/participant intake"). Four
ops: `:participant/intake` (directory upsert, no capital risk),
`:ruling/verify` (per-jurisdiction sports-officiating/anti-doping
evidence checklist, never auto), `:antidoping/screen` (anti-doping-
control screening, unconditional-evaluation discipline, never auto),
and `:actuation/finalize-ruling` (POSITIVE, high-stakes -- finalizing
a real official ruling or result). This vertical is genuinely
distinct from `sports`/8541 (sports/recreation EDUCATION -- coaching
and instructional certification): this build covers event
officiating, timing and results integrity, not instruction.

### Decision 3: `anti-doping-control-unresolved-violations` -- the 56th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for "doping" and "anti-doping" -- zero hits,
confirming this is a genuinely new unconditional-evaluation concept,
avoiding the false-precedent-claim risk `leasing`'s ADR-0001
documents. `anti-doping-control-unresolved-violations` reuses the
unconditional-evaluation DISCIPLINE (`casualty.governor/sanctions-
violations`'s original fix) for the 56th distinct application
overall, continuing the count established across this fleet's builds
(most recently `proserv.governor/chain-of-title-unresolved-
violations` at 55th). Grounded in real anti-doping law: WADA Code
Article 5/7 (testing and results management), requiring a mandatory
doping-control resolution before an official result can be ratified.
Gates `:antidoping/screen` and the actuation.

### Decision 4: `timing-calibration-overdue?` -- an honest twelfth MAXIMUM-ceiling instance, not claimed as new

`facility`/`school`/`card`/`recovery`/`care`/`navigator`/
`advertising`/`nursing`/`holdco`/`headoffice`/`reserve` established
the first eleven instances of this fleet's MAXIMUM-ceiling check
family. `sportsevent.registry/timing-calibration-overdue?` is the
TWELFTH, and specifically reuses `navigator.registry/eligibility-
window-elapsed-exceeds-validity?`'s own elapsed-time-exceeds-
validity-window sub-pattern (the sixth MAXIMUM-ceiling instance
established that sub-pattern; this build's reuse is the SECOND
instance of it) -- comparing a participant's own recorded days since
timing-device calibration against its own recorded maximum-permitted
calibration interval. Grounded in real sports-timing certification
practice (World Athletics Technical Rules Rule 17, photo-finish/
timing-system calibration requirements). Gates only the actuation (a
pure ground-truth recompute, no dedicated screening op needed).

### Decision 5: dedicated double-actuation-guard boolean

`:ruling-finalized?` is a dedicated boolean on the `participant`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`sportsevent.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/
sportsevent/store_contract_test.clj` -- the same seam every sibling
actor uses so swapping the SSoT backend is a configuration change,
not a rewrite. The protocol's per-entity accessor is named
`participant` directly -- not a Clojure special form, so no `-of`
suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:participant/intake`
(no capital risk). `:ruling/verify` and `:antidoping/screen` are
never auto-eligible at any phase (matching every sibling's
screening/verification-op posture), and `:actuation/finalize-ruling`
is permanently excluded from every phase's `:auto` set -- a
structural fact, not a rollout milestone, enforced by BOTH
`sportsevent.phase` and `sportsevent.governor`'s `high-stakes` set
independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`sportsevent.sportseventadvisor` provides `mock-advisor`
(deterministic, default everywhere -- the actor graph and governor
contract run offline) and `llm-advisor` (backed by `langchain.model/
ChatModel`, with a defensive EDN-proposal parser so a malformed LLM
response degrades to a safe low-confidence noop rather than ever
auto-finalizing a ruling).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `photo`/7420's, `personalservice`/9609's, `edsupport`/8550's,
`headoffice`/7010's, `residential`/8790's, `cultural`/8542's,
`reserve`/6411's and `proserv`/7490's own experience, this repo's
`blueprint.edn` already had the correct `isic-` prefixed `:id` and
correctly populated `:required-technologies`/`:optional-
technologies` matching the `kotoba-lang/industry` registry's own
entry for `"9319"` exactly -- only the `:maturity` field itself
needed adding.

## Alternatives considered

- **A dual-actuation shape** (splitting "ruling" and "result" into
  two acts). Rejected: the blueprint's own text consistently names
  only ONE real-world act ("finalizing an official ruling or
  result"); inventing a second would not be grounded in the
  blueprint's own text.
- **Reusing `sports`/8541's check-family design.** Rejected: `sports`/
  8541 covers instructional certification (attendance hours,
  background checks for coaching staff); `sportsevent`/9319 covers
  event officiating/timing integrity -- genuinely distinct real-world
  concerns warranting distinct checks.
- **Framing `timing-calibration-overdue?` as a wholly new concept.**
  Rejected: it is structurally identical to `navigator`/8691's
  elapsed-time-exceeds-validity-window shape; honestly characterizing
  it as a reuse (with a domain-appropriate rename) matches this
  fleet's precedent-verification discipline.

## Consequences

- Seventy-first actor in this fleet (70 implemented before this
  build).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (anti-doping-control-unresolved), grep-verified absent from
  every prior sibling before the claim was finalized.
- Documents an honest TWELFTH instance of the MAXIMUM-ceiling check
  family, and the SECOND instance of `navigator`/8691's specific
  elapsed-time-exceeds-validity-window sub-pattern, not claimed as
  new.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/sportsevent/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-9319/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-9319/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"9319"`)
