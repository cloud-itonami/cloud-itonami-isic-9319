(ns sportsevent.facts
  "Per-jurisdiction sports-officiating/anti-doping/event-timing
  regulatory catalog -- the G2-style spec-basis table the Event
  Integrity Governor checks every `:ruling/verify` proposal against
  ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's sports-officiating/anti-doping framework, or did it
  invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official national
  anti-doping organization / sports-governing-body authority (see
  `:provenance`); they are a STARTING catalog, not a from-scratch
  survey of all ~194 jurisdictions. Extending coverage is additive:
  add one map to `catalog`, cite a real source, done -- never invent
  a jurisdiction's requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the
  participant-registration-consent/event-schedule/anti-doping-
  control/timing-calibration evidence set this blueprint's own Offer
  names; `:legal-basis` / `:owner-authority` / `:provenance` are the
  G2 citation the governor requires before any `:actuation/finalize-
  ruling` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "日本アンチ・ドーピング機構 (Japan Anti-Doping Agency, JADA)"
          :legal-basis "世界アンチ・ドーピング規程 (World Anti-Doping Code) 第5条・第7条 (検査・結果管理) -- 日本スポーツ協会競技者規程"
          :national-spec "競技結果確定前のドーピング検査結果管理手続き、および計時機器の較正証明義務"
          :provenance "https://www.playtruejapan.org/entry_img/wada_code_2021_jp_20210421.pdf"
          :required-evidence ["参加者登録同意記録 (participant-registration-consent-record)"
                              "競技日程記録 (event-schedule-record)"
                              "ドーピング検査記録 (anti-doping-control-record)"
                              "計時機器較正記録 (timing-calibration-record)"]}
   "USA" {:name "United States"
          :owner-authority "United States Anti-Doping Agency (USADA)"
          :legal-basis "World Anti-Doping Code Art. 5/7 (testing and results management) -- World Athletics Technical Rules Rule 17 (photo-finish/timing systems)"
          :national-spec "Results-management procedure for pending doping-control resolution, and certified-timing-equipment calibration requirements before ratifying an official result"
          :provenance "https://www.usada.org/wp-content/uploads/World_Anti-Doping_Code.pdf"
          :required-evidence ["Participant registration-consent record"
                              "Event-schedule record"
                              "Anti-doping-control record"
                              "Timing-calibration record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "UK Anti-Doping (UKAD)"
          :legal-basis "World Anti-Doping Code (as implemented via UK National Anti-Doping Policy) -- UK Athletics Rules for Competition"
          :national-spec "Doping-control results-management resolution and certified-timing-system calibration requirements before an official result is ratified"
          :provenance "https://www.ukad.org.uk/anti-doping-policies"
          :required-evidence ["Participant registration-consent record"
                              "Event-schedule record"
                              "Anti-doping-control record"
                              "Timing-calibration record"]}
   "DEU" {:name "Germany"
          :owner-authority "Nationale Anti-Doping Agentur Deutschland (NADA)"
          :legal-basis "Welt-Anti-Doping-Code Art. 5/7 -- Deutscher Leichtathletik-Verband (DLV) Wettkampfbestimmungen"
          :national-spec "Dopingkontroll-Ergebnismanagement und Kalibrierungspflicht für Zeitmessanlagen vor Ergebnisratifizierung"
          :provenance "https://www.nada.de/nationaler-anti-doping-code"
          :required-evidence ["Einwilligungsprotokoll (participant-registration-consent-record)"
                              "Wettkampfplanprotokoll (event-schedule-record)"
                              "Dopingkontrollprotokoll (anti-doping-control-record)"
                              "Kalibrierungsprotokoll (timing-calibration-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  ruling on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9319 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `sportsevent.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
