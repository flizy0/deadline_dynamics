# SITE HANDOFF: Deadline Dynamics

**Student Work Timing & Submission Study**

Current product iteration: 2-3 October 2026, collected-data report simplification. Read this before editing. Implementation and authorized primary import are integrated. Final clean build passed 95 Maven tests; isolated browser acceptance passed 379 layouts/170 canvases, and primary read-only acceptance passed 48 layouts/96 canvases. Measured evidence belongs in [`docs/VERIFICATION.md`](docs/VERIFICATION.md). Earlier verification totals describe the preceding iteration.

## Product Boundaries

The study asks how assignment start time relates to submission by the original deadline. One observation describes one student and one recent individual assignment whose original deadline passed within 14 days. Google Forms is the collection instrument; the application presents aggregate data and a probability model.

Public branding remains **Deadline Dynamics** in RU / KK / EN. Keep `ru.deadline.lab`, `deadline_lab`, schema identifiers and existing JAR names.

There are four public navigation destinations: Overview `/`, Data `/data`, Probability Lab `/simulator`, Method `/method`. `/source`, `/connection`, `/frequencies` and `/results` remain compatible. `/survey`, `/survey/thanks`, `/login`, `/admin` and private CSV endpoints remain functional without public navigation entries.

Real is the default. Overview, Data and Method use the complete selected dataset and ignore legacy allotted/start/planning/extension filters. Filtering internals remain available for Lab, aggregate downloads and legacy services. There is no silent Demo fallback. Explicit Demo uses 144 synthetic observations, quiet utility labeling and a synthetic source designation; it must never be attributed to Google Forms.

The owner's workbook was imported through authenticated Google CSV import: 52 source responses, 29 eligible observations, 23 known outcomes, 17 on time, 6 late and 6 pending. The source dates span 30.09.2026-02.10.2026. Re-import produced 52 unchanged and no new/updated records. These are measured snapshot counts, never production constants.

Normal research states have no dataset banners, small-sample alerts, repeated methodological caveats or inline page-navigation CTAs. Important explanations use 14-16px type; chart and formula copy uses 13-14px. Method contains the single concise public limitations section. Error styling is reserved for invalid inputs and failed requests.

## Statistical Authority

```text
eligible N = all eligible observations in the selected dataset
known = ON_TIME + LATE
pending = NOT_SUBMITTED
observed p = ON_TIME / known
category p_i = category ON_TIME / category known
late percentage = 100 * category LATE / category known
percentage-point difference = 100 * (p_1 - p_2)
```

Pending and unknown outcomes remain separate counts. With known=0, proportions are unavailable, not zero. Frequency denominators include every eligible observation, including unknown predictor values. Modes use known categories. Never substitute fixed workbook/example counts for model values.

Java / `StatisticsService` calculates counts, percentages, observed probabilities, comparisons, binomial theory and random outcomes. Frontend code consumes the server values; no probability calculation belongs in `report.js`.

## Page and Model Contracts

### Overview

The page shows one research question; eligible, known, on-time and late counts; the explicit on-time/known fraction and percentage; every start-time category's outcome; planning outcomes; difficulty distribution; then the data source.

Dashboard model: `summary`, `allCount`, `totalDataset`, `sections`, `startSection`, `planningSection`, `difficultySection`, `workloadSection`, `collectionStart`, `collectionEnd`. `allCount` includes ineligible source responses. Collection dates use all stored source-response timestamps, formatted `dd.MM.yyyy` in `Asia/Qyzylorda`; omit the period if unavailable. Demo omits the collection period and names its synthetic source. Harmless legacy model attributes remain compatible.

Overview has three independent chart workspaces. Start time and planning default to Percentage Bar, with Multiple Bar and Table alternatives. Difficulty defaults to Line, with Simple Bar and Table alternatives. Exact values are server-rendered and remain accessible independently of chart color or hover.

### Data

All seven numbered chapters appear simultaneously, in `DATA_VARIABLES` order. Seven distributions and six predictor-outcome relationships make 13 local workspaces. There are no public filters, variable selectors or analysis dropdowns. The outcome itself has no relationship-to-itself chart.

`StatisticsService.reportSections(rows)` returns `List<DataExplorer>`. Each section has `analysis=distribution`, its frequency table, outcome rows, allowed views and recommended view. Legacy `explorer()` and filtering methods remain for compatibility. Data model includes `sections`, `summary`, `allCount`, `totalDataset`; harmless legacy attributes remain.

| Field | Default distribution | Local alternatives |
|---|---|---|
| `allottedBand` | Simple Bar | Line, Table |
| `startBand` | Simple Bar | Line, Table |
| `submissionStatus` | Pie | Simple Bar, Table |
| `extensionStatus` | Pie | Simple Bar, Table |
| `planning` | Simple Bar | Pie, Table |
| `difficulty` | Line | Simple Bar, Table |
| `otherDeadlines` | Simple Bar | Line, Table |

Every predictor relationship defaults to Percentage Bar; alternatives are Multiple Bar and Table. Relationship labels show category `n=known`. Unknown, not-started and after-deadline answers remain explicit; line diagrams isolate non-ordered categories. Survey ranges and open-ended classes are not continuous histogram observations.

Ordered line alternatives use horizontal framing for long day/count category labels. Difficulty retains a vertical line with compact 1-5 ticks. Tooltips and exact tables retain complete localized labels. Pie legends explicitly set readable text colors. Formula CSS must not override native MathML display with `display:block`.

`OutcomeRow(code, total, known, onTime, late, pending, unknown, onTimePercent)` retains its constructor and adds nullable, JSON-serialized `latePercent()`. Both percentages are null when known=0. `allCount` remains a long.

### Method

Method uses the complete selected dataset through `summary`, `allCount` and `totalDataset`. The quiz is removed. Its timeline has Collect, Select, Describe, Compare and Model stages, arranged horizontally on desktop and vertically on mobile.

Seventeen formula cards form five groups: four descriptive formulas; observed probability and percentage-point difference; six binomial-model formulas; two simulation formulas; range, Sturges and integer class width. Each card gives a name, formula, meaning and use. One worked example uses current on-time, late and known counts; undefined p is handled explicitly. Four plain limitations conclude the page.

Sturges applies only to numerical simulated results. For M runs, implementation uses `K=max(1, round(1+3.322*log10(M)))` and `h=max(1, ceil((max-min+1)/K))`. The extra 1 covers inclusive integer outcomes. These are not questionnaire day-band calculations.

### Probability Lab

The progressive experiment remains: probability source, one random group, repetition, distribution/theory and interpretation. Sources are overall, earlier, final-day and custom. `labMode=single|compare`, `stage=group|repeat`, existing numeric parameters and advanced seed/statistics remain.

Lab's earlier/final-day scenarios still use a selected allotted-time band and no extensions. This is a secondary model comparison, independent of Overview's all-category primary visual. Missing known outcomes make an observed probability unavailable. Metadata may retain insufficiency flags internally without displaying ordinary warning banners.

Success is on-time submission: `X ~ Binomial(n,p)`. URL p is a percentage; server p is 0..1. Group size is 1..100, repeats 1..50000, threshold 0..n. Threshold changes request server recalculation. Convergence need not improve monotonically. Distribution overlap is a model summary, not a significance test.

`GET /simulator/group` returns `GroupExperiment`; `GET /simulator/groups` returns paired earlier/final-day experiments. These endpoints never persist survey observations. Advanced exact tables, descriptive simulation statistics and integer grouping remain available.

## Import and Private State

Google CSV import retains recognized self-reports even when fields conflict. It preserves source answers instead of rewriting or dropping them. Required columns, valid answer codes, timestamps, limits, atomic persistence and idempotency remain enforced. Website submissions and canonical CSV still use strict cross-field consistency validation.

The protected importer accepts CSV. A source XLSX response sheet must first be converted without altering answers or timestamps. A supplied workbook is not evidence that a primary import has completed; only a confirmed import and aggregate readback establish the current dataset.

Google IDs use timestamps and normalized answers; identical same-second rows receive suffixes. Re-importing an unchanged complete export is idempotent. Edited old responses, partial exports and anonymous participant duplicates still need owner reconciliation. There is no live Google synchronization.

Preserve Spring Security, CSRF, CSP, schema/entities, answer codes, transactional import and private environment. Never distribute `local.env`, `.env`, `.runtime`, raw workbook/CSV responses or credentials.

## Frontend and Localization

Shared shell is `templates/fragments.html`. Overview uses `overview.css`; Data and Overview share `report.css` and `report.js`; Lab uses `lab.js` / `lab.css`; Method uses `study.css`. Legacy explorer assets remain compatible. Avoid double initialization of a canvas.

`window.DeadlineReport.mount(workspace, payload)` also initializes `script[data-report-payload]` through its `data-report-target`. A workspace contains local `data-report-view` buttons, a canvas and an exact details/table region. Payloads contain `field`, `kind`, localized `labels`, `rows`, `outcomes`, `views`, `defaultView` and localized `text`. Controls support selected state and keyboard navigation; exact scroll regions are focusable.

Message basenames are `messages`, `story`, `experiment`, `explore`, `lab`, `study`, `report`, `overview`, each with base / ru / kk / en variants. Locale defaults to Russian; `?lang=ru|kk|en` persists through the session. Language links preserve query state, never POST answers or CSRF. Formula notation and response codes stay universal.

## Verification and Packaging

Root owns Maven, runtime, authorized primary import, browser checks, archive creation and `docs/VERIFICATION.md`; avoid concurrent builds and any synthetic writes to the primary research database.

Acceptance must cover RU / KK / EN at 1440 / 1024 / 768 / 390px, all public destinations, compatible routes, 13 Data workspaces, three Overview visuals, local diagram switches, exact tables, Method formulas, locale persistence and preserved security/import behavior. No page-level overflow or console errors are acceptable. Record measured totals only after execution.

The final clean Maven build passed 95 tests with no failures, errors or skips, including seven PostgreSQL tests; JaCoCo line/branch coverage is 95.0%/82.3%. Final browser and archive evidence is recorded in VERIFICATION. The 145-entry archive includes the verified application JAR, current screenshots, npm launcher and code; SHA-256 matches the final Maven build. It excludes private state, raw responses and stale screenshots. Main runtime is localhost:8085; the disposable browser app was stopped. A deployed copy needs its own private response import. Docker and external hosting need separate validation.

See [`docs/PRODUCT_2026_10.md`](docs/PRODUCT_2026_10.md), [`docs/METHODOLOGY.md`](docs/METHODOLOGY.md), [`docs/GOOGLE_FORMS.md`](docs/GOOGLE_FORMS.md) and [`README.md`](README.md) for the product, academic, collection and launch documentation.
