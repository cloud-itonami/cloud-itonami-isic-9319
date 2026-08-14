(ns sportsevent.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2: this repo had a product face
  (`docs/index.html`) but NO operator console and no generator at all.

  This namespace drives the REAL actor stack -- `sportsevent.operation`
  (a langgraph-clj StateGraph) -> `sportsevent.governor` (the Event
  Integrity Governor) -> `sportsevent.store` -- through a scenario built
  ONLY out of this repo's own seeded participants
  (`sportsevent.store/demo-data`: participant-1..participant-4) and
  renders whatever the governor actually decided. Nothing on the page is
  typed by hand: every id, name, day count, jurisdiction, violation
  rule, violation detail, ruling number and ledger fact is read back out
  of the store or out of the graph's own final state after the run.

  The scenario deliberately reaches BOTH directions, because a console
  that only ever shows green is not evidence that the governor works:

    - five COMMITTED ops, four of them through a real human approval
      handoff (`interrupt-before #{:request-approval}` -> resume with
      `{:approval {:status :approved :by \"op-1\"}}`);
    - six HARD `:governor-hold`s, covering ALL FIVE of the governor's
      un-overridable rules (`:no-spec-basis`, `:evidence-incomplete`,
      `:timing-calibration-overdue`, `:anti-doping-control-unresolved`,
      `:already-finalized`), one of which fires two rules at once;
    - one human REJECTION (the approver said no), and
    - one phase-gate hold (phase 0 is read-only), which is deliberately
      NOT counted as a HARD hold -- the two layers are shown separately.

  `-main` counts the HARD holds it rendered and THROWS if that count is
  zero, so 'the un-overridable path is exercised' is a build-time
  invariant of this repo rather than a convention someone has to
  remember.

  Determinism: no timestamps, no randomness, no reliance on map
  iteration order (every map is sorted explicitly before rendering).
  Two consecutive runs are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jp-go-dds.skin]
            [langgraph.graph :as g]
            [sportsevent.facts :as facts]
            [sportsevent.operation :as op]
            [sportsevent.phase :as phase]
            [sportsevent.registry :as registry]
            [sportsevent.store :as store]))

;; ----------------------------- the scenario -----------------------------

(def ^:private operator
  "The same operator context this repo's own `sportsevent.sim` uses."
  {:actor-id "op-1" :actor-role :licensed-official :phase 3})

(def ^:private read-only-operator
  "Phase 0 -- `sportsevent.phase` allows no writes at all here."
  (assoc operator :phase 0))

(def ^:private assisted-operator
  "Phase 2 -- intake is writable but NOT auto-eligible, so an intake here
  goes through the human approval handoff instead of auto-committing.
  That is the only way to observe what the SSoT does with an approver on
  a `:participant/upsert`, which phase 3 never asks a human about."
  (assoc operator :phase 2))

(def ^:private approve-by-op-1 {:status :approved :by "op-1"})
(def ^:private reject-by-op-1  {:status :rejected :by "op-1"})

(def ^:private scenario
  "One entry = one supervised actor run. Every `:subject` is a seeded
  participant id from `sportsevent.store/demo-data`; every expectation
  in the `:label` is re-derived from real output at render time rather
  than trusted."
  [{:thread "t01"
    :label "participant-1 intake -- governor clean, phase-3 auto-commit"
    :request {:op :participant/intake :subject "participant-1"
              :patch {:id "participant-1" :participant-name "Sato Kenji"}}}

   {:thread "t02"
    :label "participant-1 ruling verification -- JPN spec-basis cited, human approves"
    :request {:op :ruling/verify :subject "participant-1"}
    :approval approve-by-op-1}

   {:thread "t03"
    :label "participant-1 anti-doping screening -- resolved, human approves"
    :request {:op :antidoping/screen :subject "participant-1"}
    :approval approve-by-op-1}

   {:thread "t04"
    :label "participant-1 ruling finalization -- actuation, always human-gated"
    :request {:op :actuation/finalize-ruling :subject "participant-1"}
    :approval approve-by-op-1}

   {:thread "t05"
    :label "participant-2 ruling verification -- its own seeded jurisdiction has no spec-basis"
    :request {:op :ruling/verify :subject "participant-2"}}

   {:thread "t06"
    :label "participant-2 ruling finalization -- no evidence checklist on file"
    :request {:op :actuation/finalize-ruling :subject "participant-2"}}

   {:thread "t07"
    :label "participant-3 ruling verification -- JPN spec-basis cited, human approves"
    :request {:op :ruling/verify :subject "participant-3"}
    :approval approve-by-op-1}

   {:thread "t08"
    :label "participant-3 ruling finalization -- timing calibration overdue"
    :request {:op :actuation/finalize-ruling :subject "participant-3"}}

   {:thread "t09"
    :label "participant-4 anti-doping screening -- the screening op holds on its own finding"
    :request {:op :antidoping/screen :subject "participant-4"}}

   {:thread "t10"
    :label "participant-4 ruling finalization -- two HARD rules fire at once"
    :request {:op :actuation/finalize-ruling :subject "participant-4"}}

   {:thread "t11"
    :label "participant-1 ruling finalization AGAIN -- double finalization"
    :request {:op :actuation/finalize-ruling :subject "participant-1"}}

   {:thread "t12"
    :label "participant-3 anti-doping screening -- governor clean, human REJECTS"
    :request {:op :antidoping/screen :subject "participant-3"}
    :approval reject-by-op-1}

   {:thread "t13"
    :label "participant-2 intake at phase 0 -- rollout gate, not a governor rule"
    :context read-only-operator
    :request {:op :participant/intake :subject "participant-2"
              :patch {:id "participant-2" :participant-name "Atlantis Doe"}}}

   {:thread "t14"
    :label "participant-3 intake at phase 2 -- approved intake, so upsert provenance is observable"
    :context assisted-operator
    :request {:op :participant/intake :subject "participant-3"
              :patch {:id "participant-3" :participant-name "鈴木花子"}}
    :approval approve-by-op-1}])

(defn- run-step!
  "Runs one scenario entry against `actor`, resuming through the human
  approval handoff when the entry supplies one. Returns the graph's own
  final state, untouched."
  [actor {:keys [thread request context approval]}]
  (let [ctx (or context operator)
        first-run (g/run* actor {:request request :context ctx} {:thread-id thread})
        resumed (when approval
                  (g/run* actor {:approval approval} {:thread-id thread :resume? true}))]
    {:context ctx :state (:state (or resumed first-run))}))

(defn run-demo!
  "Runs the whole scenario against a freshly seeded MemStore. Returns
  `{:db <store> :steps [..]}` where each step carries the graph's real
  final state -- the only input the renderer below is allowed to use."
  []
  (let [db (store/seed-db)
        actor (op/build db)
        steps (into []
                    (map-indexed
                     (fn [i entry]
                       (merge {:n (inc i)} entry (run-step! actor entry)))
                     scenario))]
    {:db db :steps steps}))

;; ----------------------------- derivation -----------------------------

(defn- fact-of [state t]
  (last (filter #(= t (:t %)) (:audit state))))

(defn- hard-hold?
  "A HARD hold is a `:governor-hold` carrying at least one governor rule
  violation. A phase-gate hold also writes a `:governor-hold` fact, but
  with an EMPTY `:violations` vector -- it is a rollout decision, not an
  un-overridable compliance ruling, so it must not be counted here."
  [f]
  (and (= :governor-hold (:t f)) (boolean (seq (:violations f)))))

(defn hard-holds
  "The HARD `:governor-hold` facts in the persisted ledger. This is the
  same collection the HARD-hold section renders and the same one
  `-main` asserts on."
  [db]
  (into [] (filter hard-hold?) (store/ledger db)))

(defn- stored-approver
  "MEASURES what the SSoT actually retained for the record a committed
  step produced -- by reading the record back out of the store, per
  effect. Derived at render time, so the page self-corrects the day the
  actor stops dropping the approver: nothing about the current
  behaviour is written down here."
  [db effect subject]
  (case effect
    :participant/upsert         (:approved-by (store/participant db subject))
    :ruling/set                 (:approved-by (store/ruling-of db subject))
    :antidoping/set             (:approved-by (store/antidoping-screen-of db subject))
    :participant/mark-finalized (some-> (first (filter #(= subject (get % "participant_id"))
                                                       (store/ruling-history db)))
                                        (get "approved_by"))
    nil))

(defn- step-view
  "Everything the page shows about one run, derived from that run's own
  final state plus a read-back of the store."
  [db {:keys [n label request context state] :as step}]
  (let [verdict (:verdict state)
        record (:record state)
        granted (fact-of state :approval-granted)
        rejected (fact-of state :approval-rejected)
        hold (fact-of state :governor-hold)
        disposition (:disposition state)
        subject (:subject request)
        effect (:effect record)]
    (assoc step
           :op (:op request)
           :subject subject
           :phase (:phase context operator)
           :verdict verdict
           :effect effect
           :violations (vec (:violations hold))
           :phase-reason (:phase-reason hold)
           :hold-fact hold
           :approver (:by granted)
           :rejected-by (:by rejected)
           :disposition disposition
           :outcome (cond
                      (and (= :commit disposition) granted) :approved-commit
                      (= :commit disposition) :auto-commit
                      rejected :human-rejected
                      (hard-hold? hold) :hard-hold
                      (:phase-reason hold) :phase-hold
                      :else :hold)
           :retained-approver (when (and (= :commit disposition) effect)
                                (stored-approver db effect subject))
           :label label
           :n n)))

;; ----------------------------- html helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- lbl
  "Ledger `:basis` mixes keywords (`:antidoping-check`) with plain
  strings (a jurisdiction's legal-basis text and provenance URL), so
  `name` alone would throw."
  [v]
  (if (keyword? v) (name v) (str v)))

(defn- joined [coll] (str/join ", " (map lbl coll)))

(defn- code [v] (str "<code>" (esc v) "</code>"))

(defn- span [cls v] (str "<span class=\"" cls "\">" v "</span>"))

(defn- yes-no [truthy yes-cls yes no-cls no]
  (if truthy (span yes-cls yes) (span no-cls no)))

(defn- row [& cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>" (str/join (map #(str "<th>" (esc %) "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       "    <p class=\"muted\">" lede "</p>\n"
       body
       "  </section>\n"))

;; ----------------------------- sections -----------------------------

(defn- participant-rows [db]
  (for [p (store/all-participants db)]
    (row (code (:id p))
         (esc (:participant-name p))
         (str (code (:jurisdiction p)) " "
              (if (facts/spec-basis (:jurisdiction p))
                (span "ok" "spec-basis on file")
                (span "critical" "no spec-basis")))
         (str (esc (:days-since-timing-calibration p)) " / " (esc (:max-timing-calibration-interval-days p)) " days "
              (if (registry/timing-calibration-overdue? p)
                (span "critical" "overdue")
                (span "ok" "current")))
         (yes-no (:anti-doping-control-unresolved? p) "critical" "unresolved" "ok" "resolved")
         (if (:ruling-finalized? p)
           (str (span "ok" "finalized") " " (code (:ruling-number p)))
           (span "muted" "not finalized")))))

(defn- outcome-cell [{:keys [outcome approver rejected-by violations phase-reason]}]
  (case outcome
    :auto-commit     (span "ok" "auto-committed (governor clean)")
    :approved-commit (str (span "ok" "committed") " after approval by " (code approver))
    :human-rejected  (str (span "warn" "held") " &mdash; approver " (code rejected-by) " rejected")
    :hard-hold       (span "critical" (str "HARD hold &middot; " (esc (joined (map :rule violations)))))
    :phase-hold      (span "warn" (str "phase gate &middot; " (esc (lbl phase-reason))))
    (span "muted" "held")))

(defn- verdict-cell [{:keys [verdict]}]
  (str/join " "
            [(yes-no (:hard? verdict) "critical" "hard" "muted" "no hard violation")
             (when (:escalate? verdict) (span "warn" "escalate"))
             (when (:high-stakes? verdict) (span "warn" "high-stakes"))
             (span "muted" (str "confidence " (esc (:confidence verdict))))]))

(defn- timeline-rows [views]
  (for [v views]
    (row (esc (:n v))
         (code (:op v))
         (code (:subject v))
         (esc (:phase v))
         (esc (:label v))
         (verdict-cell v)
         (outcome-cell v))))

(defn- hard-hold-rows [holds]
  (apply concat
         (for [h holds
               vio (:violations h)]
           [(row (code (:op h))
                 (code (:subject h))
                 (span "critical" (esc (lbl (:rule vio))))
                 (esc (:detail vio))
                 (esc (:confidence h)))])))

(defn- rule-tally-rows [ledger]
  (let [fired (->> ledger
                   (filter hard-hold?)
                   (mapcat :violations)
                   (map :rule)
                   frequencies)]
    (for [[rule n] (sort-by (comp lbl key) fired)]
      (row (code rule)
           (span "critical" "HARD &middot; un-overridable")
           (esc n)))))

(defn- phase-rows []
  (for [[n {:keys [label writes auto]}] (sort-by key phase/phases)]
    (row (esc n)
         (esc label)
         (if (seq writes) (joined (sort (map lbl writes))) (span "muted" "none"))
         (if (seq auto) (joined (sort (map lbl auto))) (span "muted" "none"))
         (yes-no (contains? auto :actuation/finalize-ruling)
                 "critical" "auto-eligible"
                 "ok" "never auto"))))

(defn- jurisdiction-rows []
  (for [[iso3 m] (sort-by key facts/catalog)]
    (row (code iso3)
         (esc (:name m))
         (esc (:owner-authority m))
         (esc (:legal-basis m))
         (esc (count (:required-evidence m))))))

(defn- approver-rows [views]
  (for [v views
        :when (= :commit (:disposition v))]
    (let [{:keys [approver retained-approver effect subject op]} v]
      (row (code op)
           (code subject)
           (code effect)
           (if approver (code approver) (span "muted" "no human in the loop (auto-commit)"))
           (cond
             (nil? approver)
             (span "muted" "n/a &mdash; nobody approved this record")

             (= approver retained-approver)
             (str (span "ok" "retained") " &mdash; " (code retained-approver))

             (nil? retained-approver)
             (span "critical" "DROPPED &mdash; the committed record carries no approver")

             :else
             (str (span "warn" "differs") " &mdash; " (code retained-approver)))))))

(defn- ledger-rows [ledger]
  (map-indexed
   (fn [i f]
     (row (esc i)
          (code (:t f))
          (code (:op f))
          (code (:subject f))
          (esc (lbl (:disposition f)))
          (esc (joined (:basis f)))))
   ledger))

(defn- finalization-rows [db]
  (for [r (store/ruling-history db)]
    (row (code (get r "record_id"))
         (esc (get r "kind"))
         (code (get r "participant_id"))
         (code (get r "jurisdiction"))
         (yes-no (get r "immutable") "ok" "immutable" "warn" "mutable")
         (if-let [a (get r "approved_by")]
           (str (span "ok" "recorded") " &mdash; " (code a))
           (span "critical" "no approver on the record")))))

;; ----------------------------- document -----------------------------

(defn render
  "Renders the whole console from a store that has already been through
  `run-demo!`, plus that run's step states."
  [db steps]
  (let [views (mapv (partial step-view db) steps)
        ledger (vec (store/ledger db))
        holds (hard-holds db)
        committed (filter #(= :commit (:disposition %)) views)
        approved (filter :approver views)
        seeded-jurisdictions (vec (sort (distinct (map :jurisdiction (store/all-participants db)))))
        coverage (facts/coverage seeded-jurisdictions)
        dropped (filter #(and (:approver %) (nil? (:retained-approver %))) views)
        kept (filter #(and (:approver %) (:retained-approver %)) views)
        effects-of (fn [vs] (str/join ", " (map code (sort (distinct (map :effect vs))))))]
    (str
     "<!DOCTYPE html>\n"
     "<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">"
     "<title>cloud-itonami-isic-9319 &middot; sports-event operator console</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Other sports activities (ISIC 9319) &mdash; Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample &middot; generated by <code>clojure -M:dev:render-html</code> &middot; "
     (count views) " supervised actor runs &middot; " (count holds) " HARD governor holds</span>\n"
     "</header>\n"
     "<main>\n"

     (section
      "How this page was made"
      (str "Every row below is output of a real run of this repo's own actor stack &mdash; "
           (code "sportsevent.operation") " (a langgraph-clj StateGraph) &rarr; "
           (code "sportsevent.governor") " (the Event Integrity Governor) &rarr; "
           (code "sportsevent.store") ". The participants are the four seeded in "
           (code "sportsevent.store/demo-data") "; no entity, day count, rule or ruling number on this "
           "page was typed by hand. <strong>" (count views) "</strong> supervised runs produced "
           "<strong>" (count committed) "</strong> commits (" (count approved)
           " of them through a human approval handoff), <strong>" (count holds)
           "</strong> HARD governor holds and <strong>" (count ledger)
           "</strong> append-only ledger facts. The build refuses to write this file if the "
           "HARD-hold count is zero.")
      "")

     (section
      "Participant directory (post-run SSoT state)"
      (str "Read back out of " (code "sportsevent.store") " after the scenario ran. "
           "Calibration status is recomputed here by " (code "sportsevent.registry/timing-calibration-overdue?")
           " &mdash; the same independent ground-truth check the governor makes, never the advisor's claim.")
      (table ["Participant" "Name" "Jurisdiction" "Timing calibration (elapsed / max)" "Anti-doping control" "Official ruling"]
             (participant-rows db)))

     (section
      "Operation timeline"
      (str "One row = one supervised actor run = one thread through "
           (code "langgraph.graph/run*") ". The verdict column is the Event Integrity Governor's own "
           "verdict map; the outcome column is the disposition the graph actually finished on.")
      (table ["#" "Op" "Participant" "Phase" "Scenario" "Governor verdict" "Outcome"]
             (timeline-rows views)))

     (section
      (str "HARD holds (" (count holds) ") &mdash; un-overridable")
      (str "A HARD hold never reaches a human: there is no approval that clears it. Each row is one "
           "rule violation from a real " (code ":governor-hold") " fact in the ledger, with the "
           "governor's own detail text. One run below fires two rules at once, so rows may outnumber holds.")
      (table ["Op" "Participant" "Rule" "Governor detail" "Advisor confidence"]
             (hard-hold-rows holds)))

     (section
      "Which governor rules actually fired"
      (str "Tallied from the ledger, not declared. A rule with no row here was not exercised by this "
           "scenario &mdash; that is a gap in the demo, not evidence the rule is absent.")
      (table ["Rule" "Class" "Times fired"]
             (rule-tally-rows ledger)))

     (section
      "Rollout phase gate (second, independent layer)"
      (str "Read straight out of " (code "sportsevent.phase/phases") ". The phase gate can only add "
           "caution: it can turn a clean commit into an approval or a hold, never the reverse. "
           (code ":actuation/finalize-ruling") " is absent from every phase's auto set &mdash; a "
           "structural fact, checked here at render time rather than asserted.")
      (table ["Phase" "Label" "Writable ops" "Auto-committable ops" "Ruling finalization"]
             (phase-rows)))

     (section
      "Jurisdiction spec-basis catalog"
      (str "The G2 citation table the governor checks every ruling proposal against ("
           (code "sportsevent.facts/catalog") "). Coverage is reported honestly: of the "
           (:requested coverage) " jurisdiction(s) actually present in the seeded participants ("
           (esc (str/join ", " seeded-jurisdictions)) "), " (:covered coverage)
           " have an official spec-basis"
           (if (seq (:missing-jurisdictions coverage))
             (str " and " (esc (str/join ", " (:missing-jurisdictions coverage)))
                  " " (span "critical" "has none &mdash; the governor HARD-holds rather than inventing one"))
             "")
           ".")
      (table ["ISO3" "Jurisdiction" "Owner authority" "Legal basis" "Required evidence items"]
             (jurisdiction-rows)))

     (section
      "Approver provenance on committed records"
      (str "Measured, not assumed: for each committed run the approver granted in the graph is compared "
           "against what the SSoT actually retained, by reading the record back through "
           (code "sportsevent.store") ". "
           (if (seq dropped)
             (str (span "critical" (str "This actor loses the approver on " (count dropped)
                                        " of " (count approved) " approved commit(s)"))
                  " &mdash; measured as lost under " (effects-of dropped)
                  (when (seq kept) (str ", and measured as kept under " (effects-of kept)))
                  ". A blank in the last column means the committed record carries no approver at "
                  "all, which is NOT the same as nobody having approved it &mdash; the previous "
                  "column shows who did. Note which effect is affected: "
                  (code ":participant/mark-finalized") " is the one real-world actuation this actor "
                  "performs and the one it can never auto-commit, so it is the record whose "
                  "provenance matters most.")
             (str (span "ok" "Every approved commit retained its approver")
                  " &mdash; measured under " (effects-of kept)
                  ". This section is derived at render time, so it will say so again only "
                  "as long as that stays true.")))
      (table ["Op" "Participant" "SSoT effect" "Approved by (graph)" "Approver retained in SSoT"]
             (approver-rows views)))

     (section
      "Draft ruling-finalization records"
      (str "Built by " (code "sportsevent.registry/register-ruling-finalization") " &mdash; a "
           "jurisdiction-scoped book-of-record draft, always unsigned: signature is the event "
           "organizer's own act, never this actor's.")
      (table ["Ruling number" "Kind" "Participant" "Jurisdiction" "Immutability" "Approver on record"]
             (finalization-rows db)))

     (section
      "Audit ledger (append-only)"
      (str "Every decision fact this run persisted, in order. Holds are recorded exactly like commits "
           "&mdash; a rejected proposal leaves the same evidence trail as an accepted one.")
      (table ["#" "Fact" "Op" "Participant" "Disposition" "Basis"]
             (ledger-rows ledger)))

     "</main>\n"
     "<footer class=\"muted\">\n"
     "  <p>cloud-itonami-isic-9319 &middot; Other sports activities &middot; AGPL-3.0-or-later. "
     "Regenerate with <code>clojure -M:dev:render-html</code>. This page contains no real "
     "participant data: it is this repo's own seeded demo directory.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db steps]} (run-demo!)
        holds (hard-holds db)
        html (render db steps)]
    ;; Build-time invariant, not a convention: a console that never shows
    ;; the un-overridable path is not evidence that this actor has one.
    (when (zero? (count holds))
      (throw (ex-info (str "render-html: refusing to write " out
                           " -- the scenario produced 0 HARD :governor-hold entries."
                           " The Event Integrity Governor's un-overridable path is the whole"
                           " point of this actor, so a console that cannot show it is not a"
                           " demo of it.")
                      {:hard-holds 0
                       :steps (count steps)
                       :ledger-facts (count (store/ledger db))})))
    (io/make-parents out)
    (spit out html)
    (println "wrote" out
             (str "(" (count steps) " actor runs, "
                  (count holds) " HARD governor holds, "
                  (count (store/ledger db)) " ledger facts, "
                  (count (store/ruling-history db)) " ruling-finalization drafts)"))))
