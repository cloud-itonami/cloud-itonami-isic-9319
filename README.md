# cloud-itonami-isic-9319

Open Business Blueprint for **ISIC Rev.5 9319**: Other sports
activities.

This repository publishes a sports-event actor -- participant intake,
per-jurisdiction sports-officiating/anti-doping regulatory
assessment, anti-doping-control screening and official-ruling/result
finalization -- as an OSS business that any qualified, licensed
operator can fork, deploy, run, improve and sell, so a community or
independent operator never surrenders competitor/spectator data and
ledgers to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420),
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609),
[`8550`](https://github.com/cloud-itonami/cloud-itonami-isic-8550),
[`7010`](https://github.com/cloud-itonami/cloud-itonami-isic-7010),
[`8790`](https://github.com/cloud-itonami/cloud-itonami-isic-8790),
[`8542`](https://github.com/cloud-itonami/cloud-itonami-isic-8542),
[`6411`](https://github.com/cloud-itonami/cloud-itonami-isic-6411),
[`7490`](https://github.com/cloud-itonami/cloud-itonami-isic-7490)) --
here it is **SportsEventOps-LLM ⊣ Event Integrity Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> participant-intake summary, normalizing records, and checking
> whether a participant's own recorded timing-calibration status
> actually stays within its own recorded maximum interval -- but it
> has **no notion of which jurisdiction's sports-officiating/anti-
> doping law is official, no license to finalize a real official
> ruling or result, and no way to know on its own whether a mandatory
> anti-doping-control process has actually stayed resolved**. Letting
> it finalize a ruling directly invites fabricated regulatory
> citations, a result being ratified on top of an overdue timing-
> device calibration, and an unresolved anti-doping-control concern
> being quietly overlooked -- and liability, and competitive-
> integrity risk, for whoever runs it. This project seals the
> SportsEventOps-LLM into a single node and wraps it with an
> independent **Event Integrity Governor**, a human **approval
> workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers participant intake through sports-officiating/anti-
doping regulatory assessment, anti-doping-control screening and
official-ruling/result finalization. It does **not**, by itself, hold
any license required to operate as a race organizer, officiating body
or timing service in a given jurisdiction, and it does not claim to.
It also does not perform the actual officiating/timing work itself,
or judge sporting merit -- `sportsevent.registry/timing-calibration-
overdue?` is a pure ground-truth recompute against the participant's
own recorded fields, not an officiating judgment. Whoever deploys and
operates a live instance (a licensed race/event organizer) supplies
any jurisdiction-specific license, the real officiating/timing
delivery and the real event-management-system integrations, and
bears that jurisdiction's liability -- the software supplies the
governed, spec-cited, audited execution scaffold so that organizer
does not have to build the compliance layer from scratch.

### Actuation

**Finalizing a real official ruling or result is never autonomous, at
any phase, by construction.** Two independent layers enforce this
(`sportsevent.governor`'s `:actuation/finalize-ruling` high-stakes
gate and `sportsevent.phase`'s phase table, which never puts
`:actuation/finalize-ruling` in any phase's `:auto` set) -- see
`sportsevent.phase`'s docstring and `test/sportsevent/phase_test.clj`'s
`finalize-ruling-never-auto-at-any-phase`. The actor may draft, check
and recommend; a human licensed official is always the one who
actually finalizes a ruling. Matching `leasing`'s/`underwriting`'s/
`testlab`'s/`clinic`'s/`veterinary`'s/`funeral`'s/`parksafety`'s/
`salon`'s/`entertainment`'s/`facility`'s/`consulting`'s/
`advertising`'s/`polling`'s/`research`'s/`design`'s/`sports`'s/
`alliedhealth`'s/`photo`'s/`personalservice`'s/`edsupport`'s/
`cultural`'s/`proserv`'s single-actuation shape, grounded directly in
this blueprint's own README text ("No automated proposal, by itself,
can complete the following without governor approval and audit
evidence: finalizing an official ruling or result") -- a POSITIVE
actuation (finalizing a real record), matching this fleet's majority
actuation shape (`3600`/`6190` are the fleet's two NEGATIVE-actuation
exceptions).

## The core contract

```
participant intake + jurisdiction facts (sportsevent.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ SportsEventOps-LLM    │ ─────────────▶ │ Event Integrity                │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · anti-doping-        │
          │                         │ control-unresolved                  │
    record + ledger        escalate ┼ (unconditional, NEW) · timing-        │
          │              (ALWAYS for│ calibration-overdue (MAXIMUM-           │
          │               :actuation│ ceiling, honest reuse) ·                 │
          │               /finalize-│ already-finalized                          │
          ▼               ruling)   └───────────────────────┘
      human approval
```

**The SportsEventOps-LLM never finalizes a ruling the Event Integrity
Governor would reject, and never does so without a human sign-off.**
Hard violations (fabricated regulatory requirements; unsupported
evidence; an unresolved anti-doping-control concern; an overdue
timing calibration; a double finalization) force **hold** and
*cannot* be approved past; a clean finalization proposal still always
routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a timing/scoring capture
robot assists physical event-data collection, under the actor, gated
by the independent **Event Integrity Governor**. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions
require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Event Integrity Governor, ruling-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`9319`). This vertical's participant/operational records are
practice-specific rather than a shared cross-operator data contract,
so `sportsevent.*` runs on the generic robotics/identity/forms/dmn/
bpmn/audit-ledger stack only -- no bespoke domain capability lib to
reference at all.

## Layout

| File | Role |
|---|---|
| `src/sportsevent/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + ruling-finalization history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded participant, and the double-actuation guard checks a dedicated `:ruling-finalized?` boolean rather than a `:status` value |
| `src/sportsevent/registry.cljc` | Ruling-finalization draft records, plus `timing-calibration-overdue?` -- an HONEST reuse of this fleet's MAXIMUM-ceiling check family (the TWELFTH instance, the second instance of `navigator`/8691's specific elapsed-time-exceeds-validity-window sub-pattern), not claimed as new |
| `src/sportsevent/facts.cljc` | Per-jurisdiction sports-officiating/anti-doping catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/sportsevent/sportseventadvisor.cljc` | **SportsEventOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/ruling-verification/anti-doping-control-screening/ruling-finalization proposals |
| `src/sportsevent/governor.cljc` | **Event Integrity Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · anti-doping-control-unresolved, unconditional evaluation, GENUINELY NEW, the 56th grounding of this discipline · timing-calibration-overdue, MAXIMUM-ceiling reuse, the 12th instance, not claimed as new · already-finalized guard) + 1 soft (confidence/actuation gate) |
| `src/sportsevent/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (ruling finalization always human; participant intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/sportsevent/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/sportsevent/sim.cljc` | demo driver |
| `test/sportsevent/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers participant intake through sports-officiating/anti-
doping regulatory assessment, anti-doping-control screening and
official-ruling/result finalization -- the core governed lifecycle
this blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Participant intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:participant/intake`/`:ruling/verify`) | Real event-management-system integration, real officiating/timing work itself (see `sportsevent.facts`'s docstring) |
| Anti-doping-control screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:antidoping/screen`) | Any sporting-merit judgment itself -- deliberately outside this actor's competence |
| Official-ruling/result finalization, HARD-gated on full evidence, a resolved anti-doping-control status and a current timing calibration, plus a double-finalization guard (`:actuation/finalize-ruling`) | |
| Immutable audit ledger for every intake/verification/screening/finalization decision | |

Extending coverage is additive: add the next gate (e.g. a protest/
appeal-resolution check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world
act" pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`sportsevent.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `sportsevent.facts/catalog`
-- currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `sportsevent.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `SportsEventOps-LLM` + `Event Integrity Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, modeled closely on
the seventy prior actors' architecture. See `docs/adr/0001-
architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
