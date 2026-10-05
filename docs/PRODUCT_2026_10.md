# Deadline Dynamics: Collected-Data Report

**Student Work Timing & Submission Study**

2-3 October 2026. This iteration simplifies the existing Java / Thymeleaf product around the collected dataset, course diagram vocabulary and a stronger mathematical Method page. Implementation and authorized primary import are integrated; measured verification is recorded separately in [VERIFICATION.md](VERIFICATION.md).

## 1. Public Navigation

Four destinations: Overview `/`, Data `/data`, Probability Lab `/simulator`, Method `/method`. Source is removed from navigation and integrated at the bottom of Overview. Existing `/source`, `/connection`, `/frequencies` and `/results` routes remain compatible. Survey, authentication and import infrastructure remain available internally.

The English brand **Deadline Dynamics** and the existing dark design remain. Internal package, database, schema and JAR names are unchanged.

## 2. Overview

Overview starts with one research question and four dynamic counts: eligible observations, known outcomes, on time and late. The main outcome gives both the on-time/known fraction and its percentage. All values come from the selected dataset; no sample counts are hardcoded.

The main visual relates every start-time category to submission outcome. Percentage Bar is the default, with Multiple Bar counts and an exact Table available locally. Category labels show known N. Planning outcomes and difficulty distribution are the two secondary visuals. The previous earlier/final-day comparison no longer drives Overview.

Overview, Data and Method show the complete selected dataset even if an old URL contains filter parameters. Real is the default; there is no silent synthetic fallback.

The actual supplied workbook contains 52 responses, 29 eligible observations, 23 known outcomes, 17 on time, 6 late and 6 pending. The observed proportion is 17/23 = 73.9%. This replaces the request's older 51-response snapshot; numbers remain dynamically calculated.

## 3. Data Presentation

Data is a continuous report with seven numbered chapters: time allowed, when work started, submission outcome, deadline extension, planning, difficulty and other simultaneous deadlines.

Each chapter shows its distribution immediately. Six predictor chapters also show outcome by category, giving 13 independent workspaces in total. Generous spacing, separators and numbered headings establish section boundaries. Public filters and global variable/analysis selectors are removed.

Each workspace has a local view selector and server-rendered exact table. Known, pending and unknown outcomes retain their distinct counts. Undefined percentages remain unavailable rather than becoming zero.

## 4. Diagram Types

Course names replace decorative chart vocabulary. The implemented families are Simple Bar Diagram, Line Diagram, Pie Diagram, Percentage Bar Diagram, Multiple Bar Diagram and Table.

| Distribution | Recommended | Alternatives |
|---|---|---|
| Time allowed | Simple Bar | Line, Table |
| Start time | Simple Bar | Line, Table |
| Submission outcome | Pie | Simple Bar, Table |
| Deadline extension | Pie | Simple Bar, Table |
| Planning | Simple Bar | Pie, Table |
| Difficulty 1-5 | Line | Simple Bar, Table |
| Other deadlines | Simple Bar | Line, Table |

Predictor-outcome relationships use Percentage Bar, Multiple Bar or Table, with server-calculated on-time and late percentages among known outcomes. Each category's N is visible. Line diagrams connect only ordered substantive categories; unknown and other non-ordered answers appear separately. Long day/count labels use horizontal line framing; difficulty has compact 1-5 ticks. Tooltips and tables retain complete text.

Day bands, open-ended answers and ordinal survey categories are not converted into continuous equal-width histogram observations. Numerical simulation diagrams and grouping remain in Probability Lab.

## 5. Method Timeline

The five stages are Collect, Select, Describe, Compare and Model. Each has a short title and one concise line. The timeline is horizontal on desktop and vertical on mobile. The intuition quiz is removed.

## 6. Formula Library

Seventeen formula cards form five groups:

- Describing data: absolute frequency, relative frequency, percentage and mode.
- Comparing outcomes: observed on-time probability and percentage-point difference.
- Probability model: combinations, binomial PMF, expectation, variance, standard deviation and tail probability.
- Simulation: empirical event probability and absolute simulation error.
- Grouping numerical data: range, Sturges' rule and integer class width.

Each card gives its name, formula, meaning and use. Formula notation is large and universal. One worked example calculates the observed probability from the current on-time, late and known counts.

Percentage-point difference is `100*(p1-p2)` when p values are proportions. Sturges and class width match the integer implementation and apply only to simulated numerical observations.

## 7. Content Cleanup

Normal research pages no longer contain repeated small-sample alerts, causality footers, denominator caveats, dataset banners or inline page-to-page CTAs. Method has one final section of four plain limitations. Invalid input and failed requests retain error states.

Important explanations use approximately 14-16px; chart and formula copy uses 13-14px. Small N metadata remains secondary. The Lab's progressive experiment is preserved with reduced explanatory clutter and advanced technical detail kept in its existing sections.

## 8. Source Integration

The final Overview section identifies Google Forms, all responses, eligible observations and the collection period when source timestamps exist. Dates use `Asia/Qyzylorda` and `dd.MM.yyyy`, including timestamps from ineligible source responses. No collection period is invented when unavailable.

Explicit Demo names a synthetic source and does not claim Google Forms collection. No source CTA, SQL explanation or CSV maintenance instruction appears in the public block.

The supplied real workbook is handled through the private CSV import path. Google-imported recognized self-reports retain cross-field inconsistencies; malformed structure, invalid codes/timestamps and batch limits remain enforced. Website submissions and canonical CSV retain strict consistency validation. The source workbook and raw CSV stay outside the distribution.

## 9. Localization

One template set serves RU / KK / EN. New `report` and `overview` bundles join the existing localization architecture; `study` contains the revised timeline and formula library. Controls, diagram names, formula names and explanations are translated. Language selection persists in session and links preserve active URL state. Enum codes and database records do not change.

## 10. Verification Status

The clean Maven build passed 95 tests, with no failures, errors or skips. Primary import/readback and repeat-import identity passed. Isolated browser acceptance passed 379 layouts and 170 canvas checks; primary GET-only acceptance passed 48 layouts and 96 canvas checks. The 143-entry archive excludes private state/raw responses and includes a JAR whose hash matches the final build. Evidence is recorded in [VERIFICATION.md](VERIFICATION.md).

Required checks include four public navigation entries, compatible old routes, seven simultaneous Data chapters and 13 workspaces, correct local diagram mappings, exact category values and percentages, the Overview fraction/source, five Method stages and 17 formulas, RU / KK / EN, keyboard operation and no page-level overflow at 390px. Existing survey, CSRF, auth, atomic/idempotent import and mathematical tests remain required.

Technical ownership and payload/model contracts: [SITE_HANDOFF.md](../SITE_HANDOFF.md). Academic definitions: [METHODOLOGY.md](METHODOLOGY.md). Private collection/import workflow: [GOOGLE_FORMS.md](GOOGLE_FORMS.md).
