# Static Vercel edition

The static version is served from `public/` and built to `dist/`. It uses plain HTML, CSS, vanilla JavaScript and local copies of Chart.js and Lucide. There is no runtime Java service, database, or API dependency in the Vercel build.

```text
npm run build    # create/update dist/
npm run preview  # serve dist/ at http://localhost:4173
npm run start:java  # optional: keep using the existing Spring/PostgreSQL edition locally
```

Vercel is configured to run `npm run build` and publish `dist/`. No `npm install` is needed for the static build. The original Spring Boot source and its database workflow remain in the project as an optional local edition; they are not included in the static Vercel output.

## Published data

`public/assets/js/study-data.js` contains aggregate counts from the owner's workbook only. It contains no response rows or timestamps. The current snapshot is 52 total form submissions, 29 eligible observations, 23 known outcomes (17 on time and 6 late), and 6 pending outcomes. The build checks that the totals for all seven variables reconcile before producing `dist/`.

The site is a snapshot, not a live Google Forms connection. To publish updated responses, update the aggregate categories in `study-data.js` from a fresh private export, keeping pending and unknown outcomes separate. Never copy a raw response workbook or CSV into `public/` or `dist/`.

The simulator runs in the visitor's browser. Its observed mode uses the snapshot's 17 / 23 known outcomes; pending outcomes are excluded. Manual mode accepts a chosen probability. The Method page keeps the course formulas separate from the survey calculations; one clearly labelled worked example displays the current aggregate on-time proportion.
