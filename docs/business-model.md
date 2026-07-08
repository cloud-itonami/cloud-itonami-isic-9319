# Business Model: Other sports activities

## Classification

- Repository: `cloud-itonami-isic-9319`
- ISIC Rev.5: `9319`
- Activity: other sports activities not elsewhere classified (e.g. independent race organizing, sports officiating and event-timing services)
- Social impact: cultural/recreational access, data sovereignty, transparent audit

## Customer

- independent race/event organizers
- cooperative officiating collectives
- community sporting-event programs

## Offer

- event/participant intake
- schedule/officiating-assignment proposal
- official-ruling/result proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per organizer
- support: monthly retainer with SLA
- migration: import from an incumbent event-management system
- per-event fee

## Trust Controls

- no official ruling or result is finalized without human sign-off (a licensed official)
- a fabricated timing/scoring record forces a hold, not an override
- every ruling path is auditable
- emergency manual override paths remain outside LLM control
- an unresolved anti-doping-control concern, or an overdue timing
  calibration, forces a hold, not an override
- ruling finalization is logged and escalated, and cannot be
  finalized twice for the same participant: a double-finalization
  attempt is held off this actor's own participant facts alone, with
  no upstream comparison needed

## Event Integrity Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:event-
integrity-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (finalizing a
real official ruling or result) must pass. The governor sits between
the SportsEventOps-LLM and execution, per the README's Core Contract:

```text
SportsEventOps-LLM -> Event Integrity Governor -> hold, proceed, or human approval
```

**Approves**: routine sports-event actions proposed against a
participant that already has a consented ruling evidence checklist
on file, satisfied required evidence, a resolved anti-doping-control
status, and a current timing calibration. These proceed straight to
the participant ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a ruling on its own authority when any of the following hold
-- a fabricated jurisdiction spec-basis; incomplete evidence; an
unresolved anti-doping-control concern; an overdue timing
calibration; a double-finalization attempt. A clean finalization
proposal still always routes to a human -- `:actuation/finalize-
ruling` is never auto-committed, at any rollout phase.
