(ns sportsevent.store
  "SSoT for the sports-event actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/sportsevent/store_contract_test.clj), which is the whole
  point: the actor, the Event Integrity Governor and the audit ledger
  never know which SSoT they run on.

  Like `clinic.store`'s/`proserv.store`'s simpler entities, a
  PARTICIPANT is acted on directly by the ONE actuation op -- no
  dynamically-filed sub-record, and the double-finalization guard
  checks a dedicated `:ruling-finalized?` boolean rather than a
  `:status` value, the same discipline `clinic.governor`'s/`proserv.
  governor`'s guards establish.

  NOTE on naming: the protocol's per-entity accessor is `participant`
  directly -- not a Clojure special form, so no `-of` suffix
  workaround was needed.

  The ledger stays append-only on every backend: 'which participant
  was screened for an unresolved anti-doping-control concern, which
  official ruling was finalized, on what jurisdictional basis,
  approved by whom' is always a query over an immutable log -- the
  audit trail a competitor/spectator trusting an event organizer
  needs, and the evidence an organizer needs if a ruling decision is
  later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [sportsevent.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (participant [s id])
  (all-participants [s])
  (antidoping-screen-of [s participant-id] "committed anti-doping-control screening verdict for a participant, or nil")
  (ruling-of [s participant-id] "committed ruling evidence assessment, or nil")
  (ledger [s])
  (ruling-history [s] "the append-only ruling-finalization history (sportsevent.registry drafts)")
  (next-sequence [s jurisdiction] "next ruling-number sequence for a jurisdiction")
  (participant-already-finalized? [s participant-id] "has this participant's ruling already been finalized?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-participants [s participants] "replace/seed the participant directory (map id->participant)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained participant set so the actor + tests run
  offline."
  []
  {:participants
   {"participant-1" {:id "participant-1" :participant-name "Sato Kenji"
                     :days-since-timing-calibration 10 :max-timing-calibration-interval-days 30
                     :anti-doping-control-unresolved? false
                     :ruling-finalized? false :jurisdiction "JPN" :status :intake}
    "participant-2" {:id "participant-2" :participant-name "Atlantis Doe"
                     :days-since-timing-calibration 10 :max-timing-calibration-interval-days 30
                     :anti-doping-control-unresolved? false
                     :ruling-finalized? false :jurisdiction "ATL" :status :intake}
    "participant-3" {:id "participant-3" :participant-name "鈴木花子"
                     :days-since-timing-calibration 45 :max-timing-calibration-interval-days 30
                     :anti-doping-control-unresolved? false
                     :ruling-finalized? false :jurisdiction "JPN" :status :intake}
    "participant-4" {:id "participant-4" :participant-name "田中一郎"
                     :days-since-timing-calibration 10 :max-timing-calibration-interval-days 30
                     :anti-doping-control-unresolved? true
                     :ruling-finalized? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-ruling!
  "Backend-agnostic `:participant/mark-finalized` -- looks up the
  participant via the protocol and drafts the ruling-finalization
  record, and returns {:result .. :participant-patch ..} for the
  caller to persist."
  [s participant-id]
  (let [p (participant s participant-id)
        seq-n (next-sequence s (:jurisdiction p))
        result (registry/register-ruling-finalization participant-id (:jurisdiction p) seq-n)]
    {:result result
     :participant-patch {:ruling-finalized? true
                         :ruling-number (get result "ruling_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (participant [_ id] (get-in @a [:participants id]))
  (all-participants [_] (sort-by :id (vals (:participants @a))))
  (antidoping-screen-of [_ id] (get-in @a [:antidoping-screens id]))
  (ruling-of [_ participant-id] (get-in @a [:rulings participant-id]))
  (ledger [_] (:ledger @a))
  (ruling-history [_] (:finalizations @a))
  (next-sequence [_ jurisdiction] (get-in @a [:sequences jurisdiction] 0))
  (participant-already-finalized? [_ participant-id] (boolean (get-in @a [:participants participant-id :ruling-finalized?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :participant/upsert
      (swap! a update-in [:participants (:id value)] merge value)

      :ruling/set
      (swap! a assoc-in [:rulings (first path)] payload)

      :antidoping/set
      (swap! a assoc-in [:antidoping-screens (first path)] payload)

      :participant/mark-finalized
      (let [participant-id (first path)
            {:keys [result participant-patch]} (finalize-ruling! s participant-id)
            jurisdiction (:jurisdiction (participant s participant-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:sequences jurisdiction] (fnil inc 0))
                       (update-in [:participants participant-id] merge participant-patch)
                       (update :finalizations registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-participants [s participants] (when (seq participants) (swap! a assoc :participants participants)) s))

(defn seed-db
  "A MemStore seeded with the demo participant set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :rulings {} :antidoping-screens {} :ledger [] :sequences {}
                           :finalizations []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (ruling/antidoping-screen payloads, ledger facts,
  finalization records) are stored as EDN strings so `langchain.db`
  doesn't expand them into sub-entities -- the same convention every
  sibling actor's store uses."
  {:participant/id                {:db/unique :db.unique/identity}
   :ruling/participant-id           {:db/unique :db.unique/identity}
   :antidoping/participant-id          {:db/unique :db.unique/identity}
   :ledger/seq                            {:db/unique :db.unique/identity}
   :finalization/seq                         {:db/unique :db.unique/identity}
   :sequence/jurisdiction                       {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- participant->tx [{:keys [id participant-name days-since-timing-calibration
                                max-timing-calibration-interval-days
                                anti-doping-control-unresolved? ruling-finalized?
                                jurisdiction status ruling-number]}]
  (cond-> {:participant/id id}
    participant-name                                     (assoc :participant/participant-name participant-name)
    days-since-timing-calibration                          (assoc :participant/days-since-timing-calibration days-since-timing-calibration)
    max-timing-calibration-interval-days                     (assoc :participant/max-timing-calibration-interval-days max-timing-calibration-interval-days)
    (some? anti-doping-control-unresolved?)                    (assoc :participant/anti-doping-control-unresolved? anti-doping-control-unresolved?)
    (some? ruling-finalized?)                                    (assoc :participant/ruling-finalized? ruling-finalized?)
    jurisdiction                                                   (assoc :participant/jurisdiction jurisdiction)
    status                                                           (assoc :participant/status status)
    ruling-number                                                     (assoc :participant/ruling-number ruling-number)))

(def ^:private participant-pull
  [:participant/id :participant/participant-name :participant/days-since-timing-calibration
   :participant/max-timing-calibration-interval-days
   :participant/anti-doping-control-unresolved? :participant/ruling-finalized?
   :participant/jurisdiction :participant/status :participant/ruling-number])

(defn- pull->participant [m]
  (when (:participant/id m)
    {:id (:participant/id m) :participant-name (:participant/participant-name m)
     :days-since-timing-calibration (:participant/days-since-timing-calibration m)
     :max-timing-calibration-interval-days (:participant/max-timing-calibration-interval-days m)
     :anti-doping-control-unresolved? (boolean (:participant/anti-doping-control-unresolved? m))
     :ruling-finalized? (boolean (:participant/ruling-finalized? m))
     :jurisdiction (:participant/jurisdiction m) :status (:participant/status m)
     :ruling-number (:participant/ruling-number m)}))

(defrecord DatomicStore [conn]
  Store
  (participant [_ id]
    (pull->participant (d/pull (d/db conn) participant-pull [:participant/id id])))
  (all-participants [_]
    (->> (d/q '[:find [?id ...] :where [?e :participant/id ?id]] (d/db conn))
         (map #(pull->participant (d/pull (d/db conn) participant-pull [:participant/id %])))
         (sort-by :id)))
  (antidoping-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?k :antidoping/participant-id ?pid] [?k :antidoping/payload ?p]]
              (d/db conn) id)))
  (ruling-of [_ participant-id]
    (dec* (d/q '[:find ?p . :in $ ?pid
                :where [?a :ruling/participant-id ?pid] [?a :ruling/payload ?p]]
              (d/db conn) participant-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (ruling-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :finalization/seq ?s] [?e :finalization/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :sequence/jurisdiction ?j] [?e :sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (participant-already-finalized? [s participant-id]
    (boolean (:ruling-finalized? (participant s participant-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :participant/upsert
      (d/transact! conn [(participant->tx value)])

      :ruling/set
      (d/transact! conn [{:ruling/participant-id (first path) :ruling/payload (enc payload)}])

      :antidoping/set
      (d/transact! conn [{:antidoping/participant-id (first path) :antidoping/payload (enc payload)}])

      :participant/mark-finalized
      (let [participant-id (first path)
            {:keys [result participant-patch]} (finalize-ruling! s participant-id)
            jurisdiction (:jurisdiction (participant s participant-id))
            next-n (inc (next-sequence s jurisdiction))]
        (d/transact! conn
                     [(participant->tx (assoc participant-patch :id participant-id))
                      {:sequence/jurisdiction jurisdiction :sequence/next next-n}
                      {:finalization/seq (count (ruling-history s)) :finalization/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-participants [s participants]
    (when (seq participants) (d/transact! conn (mapv participant->tx (vals participants)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:participants ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [participants]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-participants s participants))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo participant set -- the
  Datomic-backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
