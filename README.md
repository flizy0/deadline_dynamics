# Deadline Dynamics

**Student Work Timing & Submission Study**

A university Probability and Statistics project investigating whether assignment start time is associated with on-time submission. One observation is one student and one recent individual assignment. The public report uses the collected Google Forms responses.

This is an observational, voluntary, non-random sample. Association does not establish causation, and the sample is not presented as representative of all students.

## Public Experience

The Vercel edition is now static: it publishes aggregate survey data and performs probability experiments in the browser. It does not require PostgreSQL or a running Java backend. The existing Spring Boot application remains available for local use and has not been removed.

| Page | Purpose | Route |
|---|---|---|
| Overview | Sample counts, the on-time fraction and start-time outcomes | `/` |
| Data | Select one of seven variable distributions and compare six predictors with outcome | `/data/` |
| Probability Lab | Model random groups and compare theory, simulation, convergence and one group | `/simulator/` |
| Method | Independent course formula reference; survey values are not inserted into formulas | `/method/` |

The public navigation has four entries. Data source information is the final section of Overview: Google Forms, all responses, eligible observations and collection dates when available. `/source`, `/connection`, `/frequencies` and `/results` remain compatibility routes. Questionnaire and database maintenance remain private technical workflows. The configured collection instrument is the [research Google Form](https://docs.google.com/forms/d/e/1FAIpQLSdqIg5NleKUHx8wAzEpLAQ-f8V7Taui6vn56bHmvWKAeX6hYg/viewform).

The static edition uses the eligible Google Forms snapshot shown in `public/assets/js/study-data.js`; it does not bundle individual answers. The Spring edition retains its separate real/demo behavior and database rules. The static pages do not expose a Demo dataset.

## Data and Probability

The static Data page selects among all seven variables (allotted time, start time, submission outcome, extension, planning, difficulty and other deadlines) and six predictor-outcome comparisons. Each workspace switches between count, percentage and exact-table views. Distribution percentages use all 29 eligible observations; outcome comparisons use known outcomes within each group and keep pending counts separate. There are no public filters and no individual response rows are published.

```text
known outcomes = ON_TIME + LATE
observed on-time probability = ON_TIME / known outcomes
```

Pending (`NOT_SUBMITTED`) and unknown outcomes remain separate counts. With no known outcomes, the observed probability is unavailable. The Method page is independent: it explains course formulas without inserting or connecting survey values.

The static Lab begins with a concrete binomial experiment, offers the observed share (on-time / known outcomes) or a manually chosen probability, and visualizes distribution, tail probability, convergence and one random group. The older Spring Lab remains in the codebase for its richer server-backed scenarios.

Success means on-time submission. For a model group of size n, `X ~ Binomial(n,p)`. Distribution compares simulation with theory; selecting a threshold asks how likely at least that many on-time submissions are. Convergence illustrates why many repetitions help, without claiming monotonically decreasing error. Compare scenarios shows two random groups and overlapping distributions, not a guarantee that every earlier-start group outperforms every final-day group.

Seed, exact distribution tables, mean, median, standard deviation and Sturges grouping remain under advanced statistics. More simulations do not create more survey observations or prove a causal effect.

## Stack and Localization

The existing edition uses Java 21, Spring Boot 3.5.16, Maven, Thymeleaf, PostgreSQL, Spring Security and Apache Commons Math. The static Vercel edition uses HTML/CSS and vanilla JavaScript with local Chart.js and Lucide assets. It needs Node.js to build and preview, but no npm packages or CDN.

The existing dark analytical design system, responsive sidebar/drawer and reduced-motion behavior remain. In the optional Spring edition, Java remains authoritative for statistics and the original server-backed simulator. In the Vercel edition, the browser reads only aggregate counts and calculates the binomial experiment locally. Charts have exact text/table equivalents.

Spring `MessageSource`, `SessionLocaleResolver` and `LocaleChangeInterceptor` provide RU / KK / EN in the optional backend edition. The static site supports the same three languages through `?lang=ru|kk|en` and browser local storage, with language carried across navigation. The product name stays Deadline Dynamics.

```text
src/main/java/ru/deadline/lab/
  web/          page, survey and private import controllers
  service/      validation, CSV, statistics and simulation
  model/        JPA entity and immutable result records
  repository/   persistence and transactional import
  config/       security and locale configuration
src/main/resources/
  templates/    shared server-rendered pages
  static/       CSS, JavaScript and local libraries with licenses
  messages*.properties, story*.properties, experiment*.properties
  explore*.properties, lab*.properties, study*.properties
  report*.properties, overview*.properties
  schema.sql    unchanged PostgreSQL schema
scripts/        Windows start/stop, packaging and browser verification
docs/           methodology, defense, collection and technical handoff
```

## Quick Start on Windows

The distribution includes `app.jar`. Running it needs JDK 21+ and installed PostgreSQL binaries; Maven 3.6.3+ is needed only to rebuild. `start-local.cmd` or the PowerShell script locates Maven in IntelliJ and PostgreSQL in Program Files. It creates a dedicated local cluster on port 55432 under `.runtime`.

From the project directory, create the static Vercel site and preview it locally:

```powershell
npm run build
npm run preview
npm test
```

Open `http://localhost:4173`. The build validates the aggregate counts, writes the static site to `dist/` and includes local chart/icon libraries. Vercel is configured to publish that directory. No `npm install` step is needed.

To start the existing Spring/PostgreSQL edition locally instead, use `npm run start:java` or the PowerShell command below. This optional edition keeps its previous `localhost:8085` workflow.

The equivalent PowerShell command is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start.ps1 -Background
```

Default URL: `http://localhost:8085`. If that port is occupied, the script stops without killing another process; change `SERVER_PORT` in `local.env`.

First launch creates `local.env` with random passwords. Researcher username is `admin`; its password is `ADMIN_PASSWORD` in that local file. Do not publish `local.env`, `.runtime`, raw responses or credentials with source code.

Stop owned processes without deleting data:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

Use `-SkipBuild` for an existing build. Explicit paths can be supplied through `-JavaHome`, `-MavenHome`, `-PostgresBin`.

After source changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop.ps1 -AppOnly
mvn clean package
Copy-Item .\target\deadline-lab-1.0.0.jar .\app.jar -Force
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start.ps1 -SkipBuild -Background
```

`-AppOnly` leaves PostgreSQL running. Background launch uses a runtime JAR copy to avoid locking the build output on Windows.

## Existing PostgreSQL and Maven

Create a dedicated role and database using an administrator's `psql`:

```sql
CREATE ROLE deadline LOGIN;
\password deadline
CREATE DATABASE deadline_lab OWNER deadline;
```

Set process environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` (at least 12 characters). `.env.example` documents their names but Spring does not load that file automatically. `DATABASE_URL=jdbc:postgresql://host:port/database` and legacy `DB_USERNAME` are supported.

```text
mvn clean package
java -jar target/deadline-lab-1.0.0.jar
```

The public rename does not change `ru.deadline.lab`, `deadline_lab`, the JAR name or SQL identifiers. `schema.sql` creates the table; Hibernate validates the mapping (`ddl-auto=validate`). Use a dedicated project database; this is not an automatic migration of an unrelated old schema. H2 is not used.

## Collection and Private Maintenance

Google Forms is the primary collection channel. Its public URL does not grant access to responses. The owner exports the complete CSV and imports it through protected `/admin`; publishing the Google Sheet is unnecessary. New records become visible on the next page request. There is no live synchronization or timer.

The existing `/survey` and `/survey/thanks` routes remain for compatibility, not as promoted collection features. Their POST validation, CSRF and answer codes are unchanged. Avoid collecting the same participant through both channels: anonymous cross-channel duplicates cannot be identified reliably.

Private researcher flow: `/login` -> `/admin` -> Google or canonical CSV import. Spring Security and CSRF remain enabled. Import validates the entire batch before transactional persistence. The Google Forms path preserves recognized self-reported answers even when they conflict across fields; it still rejects malformed structure, unrecognized answers and invalid timestamps. Website submissions and canonical CSV retain strict cross-field validation. Re-importing the same complete unchanged export is idempotent. Google CSV IDs use timestamps and normalized answers; equal answers in one second receive suffixes. Edited historical answers or partial files without stable Google IDs require manual reconciliation.

`survey_responses` retains `external_id`, `submitted_at`, `eligible`, categorical fields and nullable difficulty 1-5. Legacy `assignment_type` stays for compatibility and receives UNKNOWN from the current questionnaire. No schema, entity, package or database rename is required.

Collection and import instructions: [`docs/GOOGLE_FORMS.md`](docs/GOOGLE_FORMS.md). Exact questionnaire reference: [`docs/SURVEY.md`](docs/SURVEY.md).

## Deployment and Verification

For Docker, set unique `DB_PASSWORD` and `ADMIN_PASSWORD` in `.env`, then run `docker compose up --build -d`. The database port is not published; the site binds to `127.0.0.1:8085`. The named database volume persists through `docker compose down`; do not add `-v` when data must be retained.

For external hosting, use Java 21, PostgreSQL and `SPRING_PROFILES_ACTIVE=prod`, configure an HTTPS reverse proxy and environment credentials, restrict direct application access, overwrite forwarded headers at the proxy, rate-limit sensitive endpoints and back up PostgreSQL. Do not expose PostgreSQL publicly.

The collected-data report iteration is implemented. The supplied workbook was privately imported: 52 responses, 29 eligible observations, 23 known outcomes (17 on time, 6 late), and 6 pending. Real is the public default; PostgreSQL data and raw responses are not bundled in the archive. The clean Maven build passed 95 tests without failures, errors or skips. Current browser/runtime/archive evidence belongs in [`docs/VERIFICATION.md`](docs/VERIFICATION.md). Docker and external hosting require their own validation and a private data import on the destination database.

The distribution must exclude `local.env`, `.env`, `.runtime`, `target`, credentials, the source workbook and raw response CSV files. The final archive's JAR must match the verified Maven output.

## Presentation

Use [`docs/METHODOLOGY.md`](docs/METHODOLOGY.md) for denominators and formulas, [`docs/PRODUCT_2026_10.md`](docs/PRODUCT_2026_10.md) for the current product report, and [`SITE_HANDOFF.md`](SITE_HANDOFF.md) for technical contracts. Present Overview, the seven Data chapters, the probability experiment and Method's formula library. Show the real Google Form or its responses separately when discussing collection; never present synthetic records as collected responses.
