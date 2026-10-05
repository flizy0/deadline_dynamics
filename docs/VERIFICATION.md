# Deadline Dynamics: Verification

Current simplification and real-data iteration: 2-3 October 2026. Final verification runs on 3 October 2026. See [PRODUCT_2026_10.md](PRODUCT_2026_10.md) for the current file map.

## Build and Automated Tests

Java 21 / Maven `clean package` passed against a disposable PostgreSQL verification database with integration opt-in enabled:

**95 tests, 0 failures, 0 errors, 0 skipped.**

| Suite | Tests | Main coverage |
|---|---:|---|
| StatisticsServiceTest | 12 | Binomial PMF/tails, p=0/1, n=1, threshold=0/n, small/large runs, fixed seed, checkpoints and groups |
| ResearchProductTest | 10 | Seven report mappings, ordered/unknown categories, known-only proportions, observed scenarios and comparisons |
| ProductPageControllerTest | 11 | Full report datasets, legacy filters, unavailable estimates, validation and pair API |
| ProductUiTest | 9 | Four destinations, aliases, seven chapters, 13 workspaces, 17 formulas, Real default, locale/state and empty/invalid pages |
| LocaleFlowTest | 5 | Eight message basenames, three locales, persistence, URL state and private POST exclusions |
| WebFlowTest | 13 | Routes/locales, survey, authentication, CSRF, import and aggregate reports |
| SurveyServiceTest | 13 | Strict survey/canonical validation, atomic/idempotent storage and answer codes |
| GoogleCsvReaderTest | 5 | Multilingual Google CSV columns, values and timestamps |
| GoogleSurveyImportTest | 4 | Google-only self-report preservation, structural validation, atomicity and strict other paths |
| ProjectRequirementsTest | 6 | Frequencies, descriptive statistics and numerical simulation grouping |
| PostgresIntegrationTest | 7 | Real JPA persistence/transactions, collection dates and idempotent Google imports |

JaCoCo: **95.0% line coverage, 82.3% branch coverage**. Reports are in `target/surefire-reports/` and `target/site/jacoco/`.

## Real Workbook and Primary Data

The owner supplied the Google Forms XLSX export. The original workbook was read without alteration. A private UTF-8 CSV intermediate outside the project preserved question/answer values and Asia/Qyzylorda timestamps. The existing authenticated, CSRF-protected Google import inserted **52 new records** into the primary database. Repeating it produced **0 new, 0 updated, 52 unchanged**.

The actual file supersedes the older prompt's 51-response snapshot:

| Measure | Current count |
|---|---:|
| All collected responses | 52 |
| Eligible observations | 29 |
| Known outcomes | 23 |
| On time | 17 |
| Late | 6 |
| Pending | 6 |

Collection dates: **30.09.2026 - 02.10.2026**. Observed probability is `17/23`, about **73.9%**. Pending outcomes are not counted as late or included in this denominator.

Some Google answers contain internally inconsistent self-reported timing. They are preserved, not corrected or silently dropped. Only Google imports relax cross-field timing consistency; structural validation, atomicity and strict on-site/canonical validation remain intact. Method explains self-report limitations once.

## Browser Verification

The final primary GET-only matrix passed **48 layouts and 96 painted/initialized canvas checks**, covering four public pages, RU/KK/EN and 1440/1024/768/390px. Counts, known-only rates, seven chapters, 13 workspaces, source dates, formula structure and horizontal MathML layout, observed lab probability and compatibility routes were checked. The imported research records survived application restarts. No synthetic/test-fixture writes used the research database.

Visual review caught and fixed native MathML overridden by `display:block`, unreadable custom pie legends, and overlapping long ordinal labels at 390px. Formula token alignment, readable pie legend colors, horizontal ordered-label framing and first/last tick spacing now have explicit checks. Updated screenshots were visually reviewed after the final rebuild.

The final isolated `scripts/verify-product.cjs` acceptance passed **379 layout checks and 170 canvas checks**, with `errors: []`. This includes 144 localized responsive route/states, 234 local distribution/relationship view layouts and an n=100 paired-group mobile layout. Local switching does not navigate; exact tables, category N, denominator arithmetic, chart data, keyboard controls, locale persistence, drawer behavior, progressive single/paired groups, p=0/1, theory/empirical distributions, thresholds and convergence passed. Survey submission, CSRF rejection, login and twice-imported canonical CSV passed only on the disposable application on port 8086.

Primary screenshots: [Overview desktop](../previews/real-overview-1440-ru.png), [Data mobile](../previews/real-data-390-kk.png), [formulas mobile](../previews/real-formulas-390-kk.png), [ordered line mobile](../previews/real-distribution-startBand-line-390-kk.png), [pie mobile](../previews/real-distribution-submissionStatus-pie-390-kk.png). Synthetic fixtures are not research evidence. Browser logs are kept outside the distributable project in `work/product-browser.log` and `work/real-report-check.log`.

## Repeat the Checks

With JDK21 and Maven:

```text
mvn clean package
```

Without integration opt-in, 88 tests execute and 7 PostgreSQL tests skip. For all 95, use a separate empty database named `deadline_lab_verify_<hex>` on local PostgreSQL port 55432 and set `DATABASE_URL`, `DB_USERNAME` (or `DB_USER`), `DB_PASSWORD`, `DEADLINE_PG_TEST=true`. Never use the research database for tests.

For browser acceptance, start a separate disposable database/application and set:

```text
TEST_BASE_URL=http://localhost:8086
TEST_DATABASE_ISOLATED=true
TEST_ADMIN_PASSWORD=<isolated application's password>
node scripts/verify-product.cjs
```

Node must resolve `playwright` through `node_modules` or `NODE_PATH`. Installed Chrome is used. Start from an empty verification database because empty-state checks precede synthetic survey/import fixtures.

## Distribution and Scope

Archive verification passed: **145 entries**, including current source, bundles, documentation, browser harness, npm launcher and current screenshots. Archived `app.jar` matches the final Maven JAR by SHA-256: `77ab84226c76ba7daf406474ea7a3164b7f2dba2d46cfbf6623758eae841283f`. The main application on `http://localhost:8085` uses this build. The isolated application was stopped after acceptance; the main Java/PostgreSQL runtime remains available.

Raw responses, workbook, private intermediate CSV, `local.env`, `.env`, `.runtime`, `target`, outdated screenshots and failed-run diagnostics are excluded. The archive does not contain the primary database: deployment needs a private import of the owner's responses.

No external Google Form was submitted by tests. There is no live Google synchronization. Voluntary/non-random sampling, self-report accuracy, anonymous deduplication and identical-p/independence assumptions remain real limitations. Collection of 120+ eligible observations is not yet complete. External hosting/HTTPS/Docker/backups require deployment-specific checks.

Earlier 83-test/145-layout and 58-test/120-layout results are historical. Four previously approved synthetic records were removed in the earlier iteration; this iteration does not delete primary observations.
