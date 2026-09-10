(ns sportsevent.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean participant
  through intake -> ruling verification -> anti-doping-control
  screening -> ruling-finalization proposal (always escalates) ->
  human approval -> commit, then shows four HARD holds (a
  jurisdiction with no spec-basis, a participant whose own recorded
  timing-calibration status has exceeded its own recorded maximum
  interval, a participant whose own recorded anti-doping-control
  concern has NOT been resolved [screened directly via
  `:antidoping/screen` -- never via an actuation op against an
  unscreened participant -- see this actor's own governor ns
  docstring / the lesson `parksafety`'s ADR-2607071922 Decision 5,
  `eldercare`'s, `museum`'s, `conservation`'s, `salon`'s,
  `entertainment`'s, `casework`'s, `hospital`'s, `facility`'s,
  `school`'s, `association`'s, `leasing`'s, `behavioral`'s,
  `secondary`'s, `card`'s, `water`'s, `telecom`'s, `aerospace`'s,
  `recovery`'s, `consulting`'s, `union`'s, `congregation`'s, `fab`'s,
  `energy`'s, `care`'s, `navigator`'s, `learning`'s, `banking`'s,
  `advertising`'s, `polling`'s, `research`'s, `design`'s, `nursing`'s,
  `sports`'s, `alliedhealth`'s, `laundry`'s, `holdco`'s, `photo`'s,
  `personalservice`'s, `edsupport`'s, `headoffice`'s, `residential`'s,
  `cultural`'s, `reserve`'s and `proserv`'s ADR-0001s already
  recorded], and a double finalization of an already-processed
  participant) that never reach a human at all, and prints the audit
  ledger + the draft ruling-finalization records."
  (:require [langgraph.graph :as g]
            [sportsevent.store :as store]
            [sportsevent.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :licensed-official :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== participant/intake participant-1 (JPN, clean; calibration current, anti-doping resolved) ==")
    (println (exec! actor "t1" {:op :participant/intake :subject "participant-1"
                                :patch {:id "participant-1" :participant-name "Sato Kenji"}} operator))

    (println "== ruling/verify participant-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :ruling/verify :subject "participant-1"} operator))
    (println (approve! actor "t2"))

    (println "== antidoping/screen participant-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :antidoping/screen :subject "participant-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/finalize-ruling participant-1 (always escalates -- actuation/finalize-ruling) ==")
    (let [r (exec! actor "t4" {:op :actuation/finalize-ruling :subject "participant-1"} operator)]
      (println r)
      (println "-- human licensed official approves --")
      (println (approve! actor "t4")))

    (println "== ruling/verify participant-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :ruling/verify :subject "participant-2" :no-spec? true} operator))

    (println "== ruling/verify participant-3 (escalates -- human approves; sets up the calibration test) ==")
    (println (exec! actor "t6" {:op :ruling/verify :subject "participant-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/finalize-ruling participant-3 (calibration 45 days > max 30 days -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/finalize-ruling :subject "participant-3"} operator))

    (println "== antidoping/screen participant-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :antidoping/screen :subject "participant-4"} operator))

    (println "== actuation/finalize-ruling participant-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/finalize-ruling :subject "participant-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft ruling-finalization records ==")
    (doseq [r (store/ruling-history db)] (println r))))
