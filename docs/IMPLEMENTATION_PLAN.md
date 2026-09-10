# NutriTrainer AI — Implementation Plan

Derived from `Conversational_Nutrition_App_PRD.pdf` (v1.0, 8 Sep 2026). This plan
translates the PRD into an engineering roadmap for the Kotlin Multiplatform
codebase in this repo. Where the PRD and this plan disagree, the PRD wins —
raise the conflict rather than guessing.

---

## 1. Product in one paragraph

An **offline-first** conversational nutrition + fitness companion for **Android
and iOS**. The user describes meals and workouts in natural voice or chat; the
app turns that into an **editable structured record**, computes nutrition with
**deterministic software** (never the LLM), and gives culturally-relevant,
**India-wide** guidance without lab-grade input. The **AI engine is replaceable**
(on-device system models, deterministic parser fallback, optional Gemma later).

### The one architectural invariant

> **Coach (conversation) and Today (tracker) are two synchronized views of one
> local record.** The AI interprets language and explains results. It does **not**
> own calories, BMI, BCA math, recipe arithmetic, history, or final
> recommendation rules — those are deterministic modules.

Everything below serves that invariant.

---

## 2. Target module structure

The repo today is a starter: `:shared` (all code) + `:androidApp` + `iosApp/`.
PRD §9 defines the destination. Migrate incrementally — do **not** big-bang this.

| Module (Gradle path) | Contents | Depends on |
|---|---|---|
| `:shared:domain` | Entities, unit/nutrient ontology, deterministic calculations, goals, precedence policies. Pure Kotlin, no Android/iOS, no I/O. | — |
| `:shared:data` | SQLDelight schema + typed migrations, repositories, Ktor API clients, response cache, provenance tracking. | `:shared:domain` |
| `:shared:ai` | Model-neutral `NutritionLanguageEngine` interface, versioned `NutritionIntent`/response schemas, validators, evaluation-corpus runner. | `:shared:domain` |
| `:shared:ui` | Compose Multiplatform screens, design system, state holders (`ViewModel`s). | `:shared:domain`, `:shared:ai` (contracts only) |
| `:androidApp` | Android entry point + `androidPlatform` impls: Gemini Nano (ML Kit GenAI), `SpeechRecognizer`, ML Kit barcode/OCR, Health Connect, Keystore. | all shared |
| `iosApp/` + `:shared` iosMain | iOS entry point + `iosPlatform` impls: Apple Foundation Models, Speech, Vision, HealthKit, Keychain. | all shared |
| `content-pipeline/` | **Standalone JVM tool**, not shipped in the app. Imports (OFF/USDA/IFCT), normalization, duplicate detection, pack signing, versioned data-pack output. | `:shared:domain` (for schema) |

Interim step (before splitting `:shared`): create Kotlin **packages**
`domain/`, `data/`, `ai/`, `ui/` inside the current `:shared` module and keep
the dependency direction clean. Split into real modules once boundaries are
proven (target: end of Prototype phase).

### Cross-cutting rules

- `:shared:domain` never imports `:shared:data` or platform code. Calculations
  are testable with zero mocks.
- Platform capabilities (speech, on-device LLM, barcode, OCR, health, secure
  storage) are `expect`/`actual` **or** an interface in `:shared:domain` +
  DI-injected `actual` implementation. Prefer the interface for anything with
  more than one method.
- Every AI engine implements `NutritionLanguageEngine`. Swapping Gemini Nano →
  Apple Foundation Models → Gemma must not touch storage, calculations, or UI.
- Invalid AI output is **never written directly** — validate against the schema,
  reject, and ask the user to confirm.

---

## 2A. Architecture pattern — Clean layering + MVI

**Decision:** Clean Architecture layering (realized as the modules in §2) with
**MVI** at the presentation layer. Not plain MVVM — the PRD's central
requirements make MVI the better fit.

### Why MVI here

| PRD requirement | Consequence |
|---|---|
| "Coach and Today are two synchronized views of **one** local record"; "Coach and Today totals match" (meal DoD) | One source of truth (SQLite) exposed as `Flow`; each screen derives an **immutable** state object from it. A single state + pure reducer makes "they always agree" a unit-testable property. |
| Coach intents: Consumed / Plan / Correct / Remove / Repeat / Ask / Suggest / Finalize (§7) | These *are* Model-View-**Intent**. A sealed `CoachIntent` + reducer is a direct encoding of §4.2. |
| Corrections **replace** not duplicate; revision history; undo | Unidirectional flow + explicit events + immutable snapshots. Hard to keep correct with imperative setters. |
| Replaceable AI engine; deterministic calc; versioned schemas | Clean-Architecture boundaries: interfaces in `domain`, implementations in `data` / `ai` / platform. |
| Offline-first, SQLite is source of truth | Repository pattern over a reactive DB (SQLDelight emits `Flow`). |
| Compose Multiplatform | Compose is unidirectional data flow + hoisted immutable state — MVI is idiomatic. |

§9's module table **is** a Clean Architecture layering; each layer is a Gradle module.

### Layer responsibilities

| Layer / module | Holds | Never holds |
|---|---|---|
| `:shared:domain` | Entities, value objects, **use cases** (interactors), repository *interfaces*, deterministic calculation services, precedence/policy rules | I/O, Android/iOS types, coroutine dispatchers, framework deps |
| `:shared:data` | Repository *implementations*: SQLDelight (source of truth) + Ktor remote + caches; DB/DTO ↔ domain mapping; provenance | UI state, Compose |
| `:shared:ai` | `NutritionLanguageEngine` interface, `NutritionIntent`/response schemas, validators, corpus runner | Prose generation of nutrition facts; direct DB writes |
| `:shared:ui` | One state holder per screen; design system; navigation | Business rules, SQL, network |
| platform modules | `actual` impls for speech, on-device LLM, barcode, OCR, health, secure storage | Domain logic |

Dependency direction: `ui → domain ← data`, `ui → ai ← platform`, `data → domain`,
`ai → domain`. **Nothing depends on `data` or platform except through a `domain`
interface.**

### MVI contract (presentation layer)

Each screen has a `ViewModel` (KMP `androidx.lifecycle.ViewModel`) exposing:

- `val state: StateFlow<XxxUiState>` — one immutable data class; the entire screen render input.
- `fun onIntent(intent: XxxIntent)` — the single entry point; `XxxIntent` is a sealed interface; a **pure reducer** `(state, result) -> state` computes the next state.
- `val effects: Flow<XxxEffect>` — one-off navigation / snackbar / permission-request signals (via `Channel` receiveAsFlow), never part of state.

Use cases are `suspend` (single result) or return `Flow` (streams). The
`ViewModel` runs them in `viewModelScope`; the domain stays dispatcher-agnostic
(dispatchers are injected at the data-layer edge).

- **Hand-rolled MVI** — no Orbit / MVIKotlin dependency.
- Trivial screens (Profile sub-pages) use a thin state holder, not a full reducer. MVI is the consistent pattern, applied pragmatically.
- **Koin** provides use cases → repositories → ViewModels.

### Testing consequences (matches §8 quality gates)

- Domain: pure unit + golden tests, zero mocks.
- Presentation: `given intent → assert state sequence` — reducers are trivially testable.
- "Coach and Today totals match": integration test with an in-memory fake repository feeding both ViewModels.

### Still open

1. **Navigation** — official `androidx.navigation` Compose Multiplatform (leaning yes) vs Decompose. Decide when the first multi-screen flow lands.
2. **ViewModel sharing** — fully shared in `:shared:ui`; drop to platform only if a screen needs native APIs the abstraction can't cover.

---

## 3. Domain model (PRD §9 "Core entities")

Implement in `:shared:domain` as immutable Kotlin data classes with stable IDs;
display labels come from versioned localization packs, never hardcoded.

| Entity | Key fields |
|---|---|
| `UserProfile` | units, height, goals, primary food category, allergies, language, current region |
| `FoodPreference` | state/UT, sub-regions, food traditions, cuisines, non-veg details, exclusions, schedule |
| `DietaryOverride` | date, fasting rule, veg/vegan-day, egg rule, **expiry** |
| `RegionTag` | country, state/UT, sub-region, aliases, local scripts, content-pack version |
| `Food` | canonical name, aliases, nutrients, **basis** (per 100 g / 100 ml / serving / prepared), source, confidence, category tags, regions |
| `Product` | GTIN, brand, variant, pack, label version, ingredients, nutrients |
| `Recipe` | ingredients, preparation, raw weights, **cooked yield**, category tags, regions, owner |
| `MealItem` | meal, **status (consumed / planned)**, food, quantity, modifiers, range, **revision** |
| `Workout` | type, duration, intensity, source |
| `Measurement` | weight, waist, date, conditions, source |
| `BodyComposition` | device, fat, muscle, visceral metric, original report |
| `DailySummary` | consumed totals, planned totals (separate), calculation version |
| `OnboardingState` | see §6 |

Also model as first-class value types: `NutrientVector` (kcal **and** kJ kept
separate), `Quantity` (grams / millilitres / household unit / kg / lb),
`ConfidenceBand`, `SourceRef` (with provenance: label version, capture date, URL).

---

## 4. Deterministic calculation engine (PRD §5)

`:shared:domain`, pure functions, golden-test everything.

| Calculation | Rule |
|---|---|
| Ingredient | reference nutrients per 100 g × edible grams ÷ 100 |
| Recipe batch | sum ingredients + explicit cooking fats; retain cooked yield |
| Recipe serving | batch nutrients × serving grams ÷ final cooked yield |
| BMI | weight kg ÷ (height m)² |
| Daily total | sum **confirmed consumed** items only (never planned) |
| Weight trend | rolling 7-day average when enough data |
| BCA comparison | compare compatible fields; flag device/unit changes |

Guards: detect implausible macros and serving sizes; preserve both original and
normalized values; version and roll back food-database changes.

**Household calibration:** foods start from regional defaults; app occasionally
offers a one-time calibration (e.g. weigh three cooked fulkas together); after
confirmation the household record becomes that user's default. Size / restaurant
/ added-fat modifiers create per-entry overrides.

---

## 5. Precedence rules (two separate ladders — do not conflate)

### 5a. Personalization / recommendation precedence (PRD §4.1)

1. **Safety exclusions** — allergy, clinical restriction (hard filter)
2. **Current-day rule** — fasting, vegan-today, no-egg day
3. **Primary food pattern** — vegetarian / veg+non-veg / vegan / veg+eggs
4. **Multi-select food details** — animal foods eaten, household exclusions
5. **Household defaults** — usual milk, roti/rice, recipes, portions
6. **State / sub-region / food culture** — ranking signals only
7. **Additional cuisines** — foods enjoyed outside the home cuisine

`veg+non-veg` **never** means every meal is non-veg — the ranking engine freely
recommends vegetarian meals and learns actual frequency. Food culture is
**never** used to infer religion, caste, or dietary restrictions.

### 5b. Nutrition source precedence (PRD §6)

1. User-confirmed label or recipe
2. Exact barcode / manufacturer label (after product + pack confirmation)
3. Licensed authoritative composition data
4. Curated regional recipe (community default)
5. Open community database (retain provenance)
6. Heuristic / LLM estimate — last resort, ranged, low confidence

**Confidence bands:** very high → value + source · high → value + tolerance ·
medium → range, no interruption · low → wide range + quick edit · unresolved →
require selection before saving. **Never encode confidence by colour alone.**

---

## 6. Onboarding (PRD §4.1.2 / §4.1.3)

Single-task screen per decision, "Step _n_ of 10", persistent non-destructive
Back, each answer saved locally as a **draft** (termination never forces a
restart). **Only the food pattern is mandatory**; every culture field is
skippable. `Skip` stores an explicit `skipped` state, not an empty value.

| ID | Screen | Stored result |
|---|---|---|
| OB-01 | Welcome / value (no input; no Sign in in offline-first MVP) | `onboardingVersion` |
| OB-02 | Food pattern (4 single-select cards) | `foodPattern` |
| OB-03 | Food details (multi-select, conditional on OB-02) | `allowedAnimalFoods`, `dairyAllowed`, `foodDetailIds` |
| OB-04 | Safety exclusions (allergy list + "foods I avoid", kept separate) | `allergyIds`, `avoidanceText` |
| OB-05 | State / UT (search + grouped full-India list; never preselected) | `countryCode`, `stateUtCode` |
| OB-06 | Food traditions (searchable multi-select; Browse all / Describe mine / Skip) | `traditionIds`, `customTraditionText` |
| OB-07 | Home food profile (staples, oils, dairy, spice, meal schedule, fasting) | `HouseholdFoodProfile` draft |
| OB-08 | Goal + body profile (goal cards incl. "Lose fat + gain muscle"; units, age band, height, optional weight) | `primaryGoal`, `bodyProfile` |
| OB-09 | Voice + language (app display language, then logging mode; mic permission only after "Tap to try") | `appLocale`, `loggingLanguageCodes[]`, permission result |
| OB-10 | Review (editable summary, category first) | commit one transaction; `onboardingStatus=complete` |

`OnboardingState` fields: version, currentStep, completedStepIds, foodPattern,
allowedAnimalFoods[], dairyAllowed, allergies, avoidances, countryCode,
stateUtCode, subRegionIds, traditionIds, customTraditionText, householdProfile,
primaryGoal, bodyProfile, appLocale, loggingLanguageCodes[], skippedFields,
consentTimestamps, completedAt.

Coverage requirement: **all 28 states + 8 union territories** searchable, with
aliases in local scripts and transliteration. Ship state/UT tags in the base
pack; curated sub-regional packs land later, independently versioned. New
community labels require editorial review and must not auto-assign diet rules.

Acceptance: clean install + no network → user completes onboarding and enters
Today; no recommendation incompatible with `foodPattern` / selected animal foods
/ allergies ever appears; screen-reader + 200% text + keyboard/focus +
back/resume tests pass on both platforms; a user can finish in under two minutes
via food pattern + skip + "Track only".

---

## 7. The five tabs (PRD §3–4)

| Tab | Purpose | Notes |
|---|---|---|
| **Coach** | Conversational entry + guidance | Persistent composer: keyboard, mic, camera, barcode. Live transcript editable before submit. Confirmation shows meal, status, estimated totals, confidence, "Edit items". Corrections **replace**, never duplicate. Responses stay concise: understanding → result → observation → next action. |
| **Today** | Authoritative daily record | Consumed totals vs targets (calories, protein, carbs, fibre, fat). Planned values shown **separately**, never inflating consumed. Timeline groups workout / meals / snacks / water / measurements. Each item reveals source, serving basis, confidence, range reason. "Finalize day" = snapshot + editable revision history. |
| **Progress** | Trends | Default: 7-day weight average. BMI / waist / body fat / muscle only when available. Compare BCA readings from same device + similar conditions. Every chart has an accessible text summary. |
| **Library** | Foods + recipes | Search English + Indian scripts + transliteration + alias + brand + barcode. Indicate verified label / authoritative reference / community recipe / personal recipe / estimate. Store household recipe ingredients + cooked batch yield. Confirm exact product + pack size before saving a barcode result. |
| **Profile** | Goals + controls | Targets, community, diet, permissions, backup, AI engine selection. |

### Coach intents (PRD §4.2)

| Intent | Example | Effect |
|---|---|---|
| Consumed | "I had two fulka and dal." | create consumed items |
| Plan | "I may eat dal dhokli." | create planned items, excluded from actual totals |
| Correct | "Rice was 120 g, not 200." | replace quantity + recalculate |
| Remove | "Remove the peanuts." | soft-delete with undo |
| Repeat | "Same breakfast as Tuesday." | preview cloned items before saving |
| Ask | "Are my carbs too high?" | no data mutation |
| Suggest | "What should I eat next?" | rank validated candidates; save only after selection |
| Finalize | — | day snapshot + revision history |

Voice flow: mic → platform speech recognition → editable text → AI classifies
intent + extracts foods/quantities → resolver matches household defaults +
identifies **one** material ambiguity → user confirms or answers one question →
record saves, Today updates immediately. "Just estimate" resolves optional
questions at lower confidence.

---

## 8. AI architecture (PRD §8)

| Component | Responsible for | NOT responsible for |
|---|---|---|
| Speech recognition | voice → editable text | nutrition meaning |
| Language engine | intent, entities, clarification, phrasing | calories / BMI / BCA / history |
| Resolver | aliases, identity, household defaults | generating prose |
| Calculation engine | recipes, nutrients, totals, trends | guessing missing facts |
| Recommendation engine | filtering + ranking validated meals | diagnosis |
| Local database | authoritative user + food records | inference |

**Engine strategy:** Android → Gemini Nano via ML Kit Prompt API / AICore (when
supported); iOS → Apple Foundation Models (when available + locale-supported);
both fall back to the **deterministic parser**; optional Gemma E2B/E4B later
(no official E3B). Shared: versioned `NutritionIntent` + response schemas;
reject invalid output and request confirmation.

**Prompt contract:** send only the current utterance, a compact daily summary,
relevant household defaults, and retrieved candidates. Require a schema with:
intent, status, foods, quantities, units, modifiers, confidence, clarification.

**Quality gates (release corpus):** intent accuracy ≥ 95% · food+quantity exact
match ≥ 90% (high-frequency launch languages + English) · plan vs consumed
≥ 98% · correction semantics ≥ 98% without duplicate totals · schema validity
≥ 99.5% after validation · **no silent high-confidence fabrication**.

**Evaluation corpus:** consented or synthetic, across English + launch Indian
languages/scripts, transliteration, code-switching, typos, household units,
raw/cooked ambiguity, products, corrections, plans, safety requests. Freeze a
private regression set; run it after every prompt / API / OS change.

---

## 9. Food data + open APIs (PRD §7) — external dependencies

Every imported record needs provenance, retrieval date, licence metadata, and
normalized nutrients. Publicly readable ≠ freely reusable in a commercial DB.

| Source | Use | Constraint / action |
|---|---|---|
| **Open Food Facts** | primary open barcode lookup + contribution | ODbL DB + separate contents/image licences; display attribution; keep licence-sensitive sources **separable** until legal review |
| **USDA FoodData Central** | secondary generic / international reference | API key — **never embed in the app binary**; use a minimal proxy; retain FDC IDs |
| **GS1 Digital Link** | product identity + manufacturer-page discovery | resolver access varies |
| **ICMR NIN IFCT 2017** (528 Indian foods) | Indian composition data | **not an open API; written permission required before electronic storage in a commercial product** — contact NIN early (blocking for full Indian coverage) |
| **FSSAI** | label-regulation reference + validation | regulatory context, not a nutrition API |
| **Manufacturer pages** | current label when published | terms vary, scraping may be prohibited — controlled verification with capture date |

Unknown-product workflow: scan barcode locally → search personal cache + bundled
catalogue + OFF → if online + unresolved, query controlled sources / manufacturer
pages → match barcode/brand/variant/flavour/pack → OCR front + nutrition panels
→ show extracted values for confirmation → save a user-verified version.

---

## 10. Functional requirements → phase mapping (PRD §5 FR table)

| FR | Requirement | Priority | Phase |
|---|---|---|---|
| FR01 | text + voice input, preserve original transcript | Must | Prototype (text) / Alpha (voice) |
| FR02 | classify log/plan/correct/remove/repeat/ask/suggest/finalize | Must | Prototype |
| FR03 | extract food/qty/unit/prep/meal/status into strict schema | Must | Prototype |
| FR04 | resolve personal→household→local→regional→online in precedence order | Must | Prototype → Alpha |
| FR05 | deterministic ingredient/recipe/serving nutrition | Must | Prototype |
| FR06 | separate consumed vs planned totals | Must | Prototype |
| FR07 | revision history, undo, soft deletion | Must | Prototype |
| FR08 | show confidence, source, serving basis | Must | Prototype |
| FR09 | barcode + nutrition-label photo scan | Must | Alpha |
| FR10 | core logging/calc/history offline | Must | Prototype |
| FR11 | workouts + optional wearable data | Should | Beta |
| FR12 | weight, BMI, measurements, BCA fields | Must | Alpha |
| FR13 | culturally-relevant validated meal recommendations | Must | Alpha → Beta |
| FR14 | one food pattern + multi-select details/state/traditions/day overrides | Must | Prototype (onboarding) |
| FR15 | apply safety/pattern/allowed-food/household/regional precedence everywhere | Must | Prototype → Alpha |
| FR16 | all 28 states + 8 UTs, searchable aliases, expandable packs | Must | Alpha → Launch |
| FR17 | encrypted backup export/import | Should | Beta |
| FR18 | Health Connect + HealthKit integration | Should | Beta |
| FR19 | optional common Gemma engine | Could | Later |

---

## 11. Delivery phases (PRD §12)

| Phase | Duration | Scope | Exit criteria |
|---|---|---|---|
| **Discovery** | 3–4 wk | Naming, user interviews, regional corpus, AI spikes on both platforms behind one contract | Validated end-to-end loop, device matrix, licensing path (incl. NIN contact opened) |
| **Prototype** | 4–6 wk | Coach + Today, **text only**, ~100 foods, deterministic calculations, onboarding, offline DB | End-to-end logs + corrections pass golden tests |
| **Private alpha** | 6–8 wk | Voice, **both platforms**, recipes, barcode, OCR, Progress | 90% successful interpretation with 30 testers |
| **Beta** | 6–10 wk | Open Food Facts integration, health integration, encrypted export, content tools | Performance, privacy, and store-readiness bars met |
| **Launch** | — | India-wide taxonomy + quality-gated regional packs | Quality gates sustained over time |
| **Later** | — | Optional Gemma, more content packs, managed sync, partnerships | Independent evaluation + licence approval |

### MVP scope (must all ship together)

Voice + chat logging · Coach + Today · consumed/planned/corrected states · local
foods + household defaults · deterministic calories + macros · weight + BMI
trends · India-wide region selection + regional suggestions · barcode + label
capture · system LLM + parser fallback · editable history with source +
confidence.

### Definition of done — a logged meal

Original input retained · intent + items pass schema validation · every food
resolves to a source **or** explicit estimate · consumed state correct ·
calculation version + confidence saved · **Coach and Today totals match** ·
edit / undo / delete work · cached-food path works offline.

---

## 12. Privacy, safety, accessibility (PRD §10) — non-negotiable

**Privacy:** explain local vs online processing; record mic only during explicit
listening with a visible stop control; process photos locally when possible,
consent before upload; request only the health fields a feature needs; never
train on personal data without separate opt-in; deletion removes history, cached
images, backups, personalization; do not launch for minors before age-specific
design + consent.

**Safety:** no diagnosis, medication advice, or unsafe calorie restriction;
escalate eating-disorder signals, severe symptoms, urgent concerns; allergies
are hard filters (disclose incomplete data + cross-contamination risk);
pregnancy / diabetes / kidney / clinical contexts get conservative guidance +
professional-referral language; never present consumer BCA as clinically exact;
separate informational ranges from personalized medical targets.

**Accessibility / localization:** platform touch targets (44×44 pt iOS /
48×48 dp Android), dynamic type without clipped cards, charts with text
summaries + selectable values, voice with transcript review + cancel + keyboard
alternative, confidence never by colour alone, local script + transliteration
preserved under language-neutral IDs, support g / ml / household units / kg / lb,
WCAG AA contrast, reduced motion.

**Analytics:** privacy-preserving events only (input mode, duration, confidence
band, reason category, engine, latency, coded errors). **Never** put transcripts,
exact foods, question text, prompts/responses, unprotected barcodes, user text,
images, or health data in analytics payloads.

**Legal review checklist:** OFF ODbL compliance + source separation · product-
image attribution + trademarks · IFCT written permission · USDA + third-party
terms · HealthKit + Health Connect policies · India DPDP obligations · wellness
claims + store health declarations.

---

## 13. Immediate next steps (PRD §13 + repo state)

**From the PRD:**
1. Approve product boundary + MVP scope.
2. Choose launch languages + regional content-pack sequence.
3. Specify the nutrient ontology + calculation rules precisely.
4. Create the multilingual regional evaluation corpus.
5. Prototype both system LLMs behind one contract.
6. **Contact ICMR NIN about IFCT permission** (long lead time — start now).
7. Test Coach→Today corrections with 10 target users.
8. Benchmark KMP storage, speech, and inference on representative devices.

**In this repo (engineering foundation):**
1. ✅ Deps in `gradle/libs.versions.toml`: coroutines 1.9.0, kotlinx-serialization 1.7.3 (+plugin), kotlinx-datetime 0.7.1 wired into commonMain; Koin 4.0.2, Ktor 3.0.3, SQLDelight 2.0.2 wired and verified (shared tests + iOS compile + Android APK). Versions aligned to Compose MP's transitive set. **DI = Koin, storage = SQLDelight.**
2. ◐ Packages `domain/model`, `domain/calc`, `ai/`, `data/db`, `di/` created with the dependency direction (`ai → domain`, `data → domain`). Still to do: `ui/`, ktlint/detekt.
3. ✅ `NutrientVector`, `Quantity`, `ConfidenceBand`/`Ranged`, `SourceType`/`SourceRef`, `MeasurementBasis`, `MealItemStatus`; `NutritionMath` (ingredient/recipe-serving/BMI/daily-consumed/rolling-weight). 19 golden tests. Core *entities* (UserProfile, Food, Recipe, MealItem…) still to model.
4. ✅ `NutritionIntent` schema (versioned) + `IntentKind`/`ParsedItem`/`Clarification`, `NutritionLanguageEngine` + `InterpretRequest`/`InterpretResult`, `NutritionIntentValidator`. 12 validator tests.
5. ✅ `DeterministicNutritionParser` — rule-based `NutritionLanguageEngine`, no model dependency, always available. Classifies the 8 intents, extracts items/quantities/units/meal from English, always `LOW` confidence, unparseable → `UNKNOWN` + clarification. `EnglishLexicon` split out for future regional lexicons. 15 tests over the PRD §4.2 / Appendix A examples. Registered in Koin (`DeterministicEngine` qualifier).
6. ✅ Schema: `MealItemEntity.sq`, `MeasurementEntity.sq` (weight trend / waist). Domain: `MealItem`, `Measurement` entities + `MealLogRepository` / `MeasurementRepository` interfaces + `GetWeightTrendUseCase`. Data: mappers + `SqlDelightMealLogRepository` / `SqlDelightMeasurementRepository` (`Flow` via `asFlow().mapToList`, soft-delete, upsert-replaces-by-id). Tests: in-memory JDBC driver (`sqldelight-sqlite-driver` in `androidHostTest`), `inMemoryNutriDb()` helper, 13 tests (CRUD, ordering, Flow emission, mapper round-trip, weight-trend vs hand calc, schema harness). All repos in `sharedModule`. Remaining entities (Food, Product, Recipe, UserProfile, FoodPreference, RegionTag, Workout, BodyComposition, DailySummary) + real `.sqm` migrations still to model.
7. ◐ Domain: `OnboardingState` (full field set), `OnboardingStep` (OB-01…OB-10; only OB-02 + OB-08 required), `FoodPattern`/`AnimalFood`/`PrimaryGoal`/`BodyProfile`/… `IndiaRegionCatalog` — all **28 states + 8 UTs**, zones, aliases, offline `search()`/`grouped()`. Draft persistence: `OnboardingDraftEntity.sq` (JSON blob) + `SqlDelightOnboardingDraftRepository`. MVI: `OnboardingUiState` / 20 `OnboardingIntent`s / pure `OnboardingReducer` (pattern-change revalidation, skip rules, back preservation, resume-at-first-incomplete-required) / `OnboardingViewModel` (autosaves every reduction). Compose: `OnboardingScaffold` (Step n of 10 per §4.1.3) + `OnboardingHost`; OB-02/05/08/10 built, OB-01/03/04/06/07/09 are placeholders — **screen content is the next increment**. 28 tests (reducer, catalog, draft repo).
8. ✅ `domain/resolve/`: `FoodResolver` + `SeedFoodResolver` (11-food seed, LOW confidence), `QuantityResolver` (units → grams). `domain/usecase/ObserveDayUseCase` — the **one shared stream** (`DayView` + `DayTotals`, consumed vs planned kept separate). `ai/pipeline/LogParsedIntentUseCase` — intent → resolve → `NutritionMath` → `MealLogRepository`; CONSUMED/PLAN add, CORRECT replaces the single match in place (revision+1), REMOVE soft-deletes, unresolved → unconfirmed row out of totals. `TodayViewModel` + `CoachViewModel` (both derive totals from `ObserveDayUseCase`) + minimal Compose screens. DI: resolvers/use-cases as singletons, VMs as day-parameterized factories. **27 tests** incl. `CoachTodayParityTest` — the meal DoD: Coach log shows identical totals in both views; correction replaces without duplicating; planned never inflates consumed.

**Section 13 foundation (steps 1–8) complete.** 108 tests green; iOS Kotlin compile + `androidApp:assembleDebug` clean throughout.

### Post-foundation progress

- **App shell** ✅ — `NutriTheme` (M3, calm greens, light/dark); `NutriApp` = onboarding gate → `OnboardingRoute` or `HomeScaffold` (5-tab NavigationBar: Coach/Today/Progress/Library/Profile). Koin boot: `initKoin()` + `platformModule()` expect/actual (Android `androidContext()`, iOS none), `NutriApplication`, iOS `startKoin()`. `App.kt` → `NutriApp()`; sample `Greeting` removed.
- **Onboarding screens** ✅ — all 10 built (OB-01/03/04/06/07/09 fleshed out with `FilterChip`/`FlowRow`; `UpdateHouseholdProfile` intent added).
- **Progress / Library / Profile** ✅ — Progress: 7-day trend via `GetWeightTrendUseCase` + log-weight field. Library: searchable seed-food list. Profile: onboarding read-back + Settings + "Redo onboarding".
- **Theme + Settings** ✅ — `ThemePreference` (System / Light / Dark) in an `appSettingEntity` KV table; `AppSettingsRepository.themePreference(): Flow` so the app re-themes instantly; `SYSTEM` resolves to `isSystemInDarkTheme()` live. `SettingsScreen` (Appearance) reached from Profile. 4 tests.
- **Localised UI text** ✅ — every screen string moved to Compose string resources (`composeResources/values/strings.xml`, `Res.string.*`); adding a language is now a `values-<lang>/strings.xml` drop-in. Enum→label maps return `StringResource`; chip lists are `(stableId, StringResource)`. Data-driven labels (region/food names) stay keyed by ID. **Standing rule** — no literals in composables.
- **Device-verified (Motorola Edge 30 / Android 14)** ✅ — full onboarding → home → Coach log → Today → Profile → Settings walked with screenshots. Fixes from that pass: Today `MealRow` layout (kcal text was wrapping one char per line → `weight(1f)` + 2-line ellipsis + "—" for unresolved); Library row same; Progress weight now up to 3 decimals (was rounding 97.25 → 97.3); `\'` in strings.xml rendered literally under Compose resources → replaced with plain `'`. **OB-08 now collects body profile** — Units (Metric kg/cm ↔ Imperial lb/in, converts to cm/kg), age band, height, optional weight. Deterministic parser: run-on logs with no separators ("almond 5 boiled egg 2 white 30 g milk 250") now split at amount boundaries into separate items; food name capped to last 4 words. 114 unit tests.
- Env note: `linkDebugFrameworkIosSimulatorArm64` needs a full Xcode (this machine has only Command Line Tools — `xcode-select -s /Applications/Xcode.app` fixes it); iOS Kotlin itself compiles.

- **UI tests** ✅ — `androidDeviceTest` wired (`compose ui-test-junit4` + androidx `ui-test-manifest` 1.11.2). `OnboardingHostUiTest` renders the real screen and drives clicks through the real reducer (3 tests). `./gradlew :shared:connectedAndroidDeviceTest` → 86 instrumented tests pass on device.

### "Learn a food as you go" (PRD §6 rank 1, §7) ✅ Increment 1–2

- `Food` entity + `FoodRepository` + `FoodEntity.sq` + `SqlDelightFoodRepository`. `1.sqm` migration, schema v1 → v2.
- `FoodResolver.resolve` is `suspend`; `CompositeFoodResolver` = user-confirmed first, then seed.
- `AddUserFoodUseCase.add()` / `.addAndResolve()` — persist + re-resolve the pending entry so it flips from UNRESOLVED/0 kcal to counted; future logs resolve instantly.
- Coach surfaces `pendingFood` → `AddFoodSheet` (name/brand/pack/basis/kcal+macros/"how much"); Library reworked (scrollable, "Your foods" + seed, expandable macro rows, "Add a food").
- Device-verified end to end. 123 host + 86 device tests.

Remaining: `LabelOcr` expect/actual (ML Kit / Apple Vision) + photo picker → prefill the form; persist `pendingFood` / add a "resolve" action on unresolved Today rows across restarts.

### Online food lookup (Open Food Facts) ✅ priority 1

- `OnlineFoodSource` port + `FoodSearchResult`; `OpenFoodFactsSource` (Ktor, `/cgi/search.pl` + `/api/v2/product`), fail-soft to empty. `INTERNET` permission; core still offline.
- Wired into the `AddFoodSheet` (search box + tap-to-fill result cards + ODbL attribution) from both the Coach unknown-food prompt and Library → Add a food. `MockEngine` tests. Device-verified (Nutella search returned real OFF hits, tap prefilled macros).

### Today delete + undo (PRD FR07) ✅

- `MealLogRepository.restore(id)` + `restore:` query (no schema change). `TodayViewModel` gains `onIntent(TodayIntent.Delete/UndoDelete)` over `softDelete`/`restore`.
- Each `MealRow` has a **Remove** button; deletes show a snackbar with **Undo**. Device-verified (row removed, totals held, Undo brought it back). 128 host tests.
- `NutriApp` now resolves Coach/Today via `koinViewModel` (was `koinInject` on a Koin `factory`), so the **Coach transcript and Today state survive tab switches** (previously the factory handed back a fresh empty ViewModel each time). Recreated only on process death — chat-history persistence to the DB is still a follow-up.

### Backlog from the "not happy with design" round (user, in order)

1. Online lookup ✅.
2. Region-specific search ✅ — OFF country subdomain; "cow milk" → Chitale / Gokul / Amul.
3. **Product images + detail view** ✅ — Coil 3 (KMP) + `coil-network-ktor3`, `ImageLoader` set in `NutriApp`. `FoodSearchResult` carries `thumbnailUrl` + `imageUrls: List<ProductImage>` (FRONT / INGREDIENTS / NUTRITION). Result rows show a thumbnail; tapping opens `OnlineFoodDetail` — a `HorizontalPager` gallery with Material dots + captions, the full macro table, ODbL line, and an explicit **Use this food**. Search only returns the front photo, so the detail view fetches the full product by barcode (`/api/v2/product`) to add the ingredients + nutrition-panel photos. Device-verified: Amul Taaza shows the readable nutrition-label photo on page 2.
4. **Conversational Coach** ✅ (first pass) — `CoachScreen` is now a real chat: user turns as right-aligned primary bubbles, Coach replies as left-aligned surface bubbles with asymmetric corners, auto-scroll to newest, empty-state hint. `CoachTurn` moved to `domain/model` (`@Serializable`); new `coachTurnEntity` table + `2.sqm` (schema v2→v3), `CoachTranscriptRepository` + SQLDelight impl. `CoachViewModel` seeds `turns` from `transcript.observeDay(day)` and appends each turn — so the conversation survives tab switches **and** a full process restart (device-verified: force-stop + relaunch keeps the transcript; v2→v3 migration ran clean on the installed DB). Still ahead: inline clarifying questions / ASK-SUGGEST answers, and an on-device LLM behind `NutritionLanguageEngine` for genuinely conversational replies (today it's still the deterministic parser).

### Next — richer nutrient model (the user asked for amino acids, fat breakdown, micros)

`NutrientVector` currently holds kcal/kJ + protein/carb/fat/fibre only. Expand to: sugars, added sugars, saturated / mono / poly / trans fat, sodium, cholesterol, key micros (iron, calcium, B12, vitamin D…), and an optional per-food amino-acid profile. This touches the schema (new columns / a `nutrient_detail` table), `NutritionMath` scaling, the resolver DTO, `AddFoodSheet`, and every nutrient display. Needs its own migration + a "show all" food-detail view.

Then: the real content pipeline (Open Food Facts live + USDA / IFCT bulk imports, §7), on-device engines (Gemini Nano / Apple Foundation Models behind `NutritionLanguageEngine`), remaining entities, real navigation lib, deeper Progress/Coach UI, a committed user-profile entity (draft-clear on OB-10).

---

## 14. Key risks (PRD §13)

| Risk | Mitigation |
|---|---|
| System LLM unavailable | capability check → parser fallback → optional Gemma |
| Cross-model inconsistency | strict schemas, shared validation, frozen corpus |
| Regional language weakness | aliases, transcript editing, language-specific evaluation |
| Database licence exposure | separate sources, counsel review, NIN permission |
| False precision | ranges, confidence, serving basis surfaced everywhere |
| Health-advice harm | scope limits, deterministic guards, escalation paths |
| Missing products | barcode + OCR + user verification + moderation |
| Data loss | encrypted export first; managed backup only after validation |
| OS model regression | version prompts, continuous retest against frozen corpus |
