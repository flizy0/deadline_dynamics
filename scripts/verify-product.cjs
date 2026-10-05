"use strict";

// This regression writes survey/import fixtures only to an explicitly isolated local test app.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const base = new URL(process.env.TEST_BASE_URL || "http://localhost:8086");
assert.equal(process.env.TEST_DATABASE_ISOLATED, "true", "Set TEST_DATABASE_ISOLATED=true only for a dedicated disposable test database.");
assert(["localhost", "127.0.0.1", "[::1]"].includes(base.hostname), "Browser regression is restricted to local test servers.");
assert.notEqual(base.port, "8085", "Refusing to write fixtures to the main application on port 8085.");
assert(process.env.TEST_ADMIN_PASSWORD, "Provide the isolated test app's TEST_ADMIN_PASSWORD.");
const { chromium } = require("playwright");
const artifacts = path.resolve(__dirname, "../previews");
const distributionViews = {
    allottedBand: ["bars", "line", "table"],
    startBand: ["bars", "line", "table"],
    submissionStatus: ["pie", "bars", "table"],
    extensionStatus: ["pie", "bars", "table"],
    planning: ["bars", "pie", "table"],
    difficulty: ["line", "bars", "table"],
    otherDeadlines: ["bars", "line", "table"]
};
const relationshipViews = ["percentage", "multiple", "table"];
const specialLineCodes = ["UNKNOWN", "NOT_STARTED", "AFTER"];

(async () => {
    const browser = await chromium.launch({ headless: true, channel: "chrome" });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(15_000);
    const errors = [];
    const failures = [];
    let layoutChecks = 0;
    let visualizationChecks = 0;
    page.on("pageerror", error => errors.push(error.message));
    page.on("console", message => {
        if (message.type() === "error" && !message.location().url.includes("/missing-page")) {
            errors.push(message.text() + " " + message.location().url);
        }
    });
    const url = route => new URL(route, base).href;
    const open = async route => {
        const response = await page.goto(url(route), { waitUntil: "load" });
        assert(response && response.status() < 500, "Server error: " + route);
        const text = await page.locator("body").innerText();
        assert(!/NaN|Infinity|undefined|\?\?[^\n]*\?\?/.test(text), "Invalid public text: " + route);
    };
    const payload = async (id, selector = "#" + id) => {
        const result = await page.locator(selector).textContent();
        assert(result, "Missing server payload: " + id);
        const data = JSON.parse(result);
        const validate = value => {
            if (typeof value === "number") assert(Number.isFinite(value), "Non-finite server data");
            else if (Array.isArray(value)) value.forEach(validate);
            else if (value && typeof value === "object") Object.values(value).forEach(validate);
        };
        validate(data);
        return data;
    };
    const reportPayload = id => payload(id, `script[data-report-target="${id}"]`);
    const percentage = text => {
        const match = String(text).match(/(-?\d+(?:[.,]\d+)?)\s*%/);
        return match ? Number(match[1].replace(",", ".")) : null;
    };
    const assertReportData = data => {
        assert.equal(data.labels.length, data.rows.length, "Every response category needs a localized label");
        assert.deepEqual(data.views, data.kind === "relationship" ? relationshipViews : distributionViews[data.field]);
        const total = data.rows.reduce((sum, row) => sum + row.count, 0);
        for (const row of data.rows) {
            assert(Number.isInteger(row.count) && row.count >= 0);
            assert(Math.abs(row.percent - (total ? row.count / total * 100 : 0)) < 1e-9, "Server frequency denominator");
        }
        assert.equal(data.outcomes.reduce((sum, row) => sum + row.total, 0), total);
        for (const row of data.outcomes) {
            assert.equal(row.known, row.onTime + row.late);
            assert.equal(row.total, row.known + row.pending + row.unknown);
            if (row.known === 0) {
                assert.equal(row.onTimePercent, null);
                assert.equal(row.latePercent, null);
            } else {
                assert(Math.abs(row.onTimePercent - row.onTime / row.known * 100) < 1e-9);
                assert(Math.abs(row.latePercent - row.late / row.known * 100) < 1e-9);
                assert(Math.abs(row.onTimePercent + row.latePercent - 100) < 1e-9);
            }
        }
        return total;
    };
    const assertExactTable = async (id, data) => {
        const cells = await page.locator(`#${id} .report-exact tbody tr`).evaluateAll(rows =>
            rows.map(row => Array.from(row.children, cell => cell.textContent.trim())));
        const source = data.kind === "relationship" ? data.outcomes : data.rows;
        assert.equal(cells.length, source.length, id + " exact rows");
        const labels = new Map(data.rows.map((row, index) => [row.code, data.labels[index]]));
        const assertPercent = (actual, expected) => expected == null
            ? assert.equal(actual, data.text.noData)
            : assert(Math.abs(percentage(actual) - expected) <= 0.051, id + " rounded server percentage");
        source.forEach((row, index) => {
            assert.equal(cells[index][0], labels.get(row.code));
            if (data.kind === "relationship") {
                assert.equal(cells[index].length, 9);
                assert.deepEqual(cells[index].slice(1, 7).map(Number), [row.total, row.known, row.onTime, row.late, row.pending, row.unknown]);
                assertPercent(cells[index][7], row.onTimePercent);
                assertPercent(cells[index][8], row.latePercent);
            } else {
                assert.equal(cells[index].length, 3);
                assert.equal(Number(cells[index][1]), row.count);
                assertPercent(cells[index][2], row.percent);
            }
        });
    };
    const assertMethod = async (route, lang, width) => {
        assert.equal(await page.locator("[data-method-timeline] > li").count(), 5);
        assert.equal(await page.locator("[data-formula-group]").count(), 5);
        assert.equal(await page.locator(".formula-card[data-formula]").count(), 17);
        const expressions = await page.locator(".formula-card[data-formula]").evaluateAll(cards => cards.map(card => card.querySelectorAll("math").length));
        assert(expressions.every(count => count > 0), "Each formula must use MathML");
        assert.equal(await page.locator("[data-worked-example]").count(), 1);
        const overflow = await page.locator(".formula-expression math, .method-example-calculation math").evaluateAll(formulas => formulas.flatMap(formula => {
            const bounds = formula.getBoundingClientRect();
            const parent = formula.parentElement.getBoundingClientRect();
            return bounds.left < parent.left - 1 || bounds.right > parent.right + 1
                ? [{ formula: formula.textContent, left: bounds.left, right: bounds.right, parentLeft: parent.left, parentRight: parent.right }]
                : [];
        }));
        assert.deepEqual(overflow, [], `MathML overflow: ${route} / ${lang} / ${width}px`);
        const expected = await page.locator('[data-formula="expected"] math').evaluate(math => {
            const box = math.getBoundingClientRect();
            const centers = Array.from(math.querySelectorAll(":scope > mi"), token => {
                const bounds = token.getBoundingClientRect();
                return bounds.top + bounds.height / 2;
            });
            return { height: box.height, width: box.width, spread: Math.max(...centers) - Math.min(...centers) };
        });
        assert(expected.height < 60 && expected.width > expected.height * 2 && expected.spread < 10, "MathML tokens must remain horizontal");
    };
    const assertLayout = async (route, lang, width) => {
        const layout = await page.evaluate(() => ({ viewport: innerWidth, content: document.documentElement.scrollWidth }));
        if (layout.content > layout.viewport + 1) failures.push({ route, lang, width, ...layout });
        layoutChecks++;
    };
    const chart = async (selector, expectedSeries) => {
        await page.locator(selector).waitFor({ state: "visible" });
        await page.waitForFunction(selector => {
            const canvas = document.querySelector(selector);
            return canvas && window.Chart && Chart.getChart(canvas);
        }, selector);
        const rendered = await page.locator(selector).evaluate(canvas => {
            const instance = Chart.getChart(canvas);
            const pixels = canvas.getContext("2d").getImageData(0, 0, canvas.width, canvas.height).data;
            let painted = 0;
            for (let index = 3; index < pixels.length; index += 16) if (pixels[index]) painted++;
            return {
                series: instance.data.datasets.length,
                points: Math.max(instance.data.labels?.length || 0, ...instance.data.datasets.map(dataset => dataset.data.length)),
                painted,
                width: canvas.clientWidth,
                height: canvas.clientHeight,
                invalid: instance.data.datasets.some(series => series.data.some(value => typeof value === "number" && !Number.isFinite(value)))
            };
        });
        if (expectedSeries !== undefined) assert.equal(rendered.series, expectedSeries, selector + " series");
        assert(!rendered.invalid && rendered.points > 0 && rendered.painted > 100 && rendered.width > 0 && rendered.height > 0, "Blank/invalid chart: " + selector);
        visualizationChecks++;
    };
    const assertReportChart = async (id, data, view = data.defaultView) => {
        await chart(`#${id} canvas`);
        const rendered = await page.locator(`#${id} canvas`).evaluate(canvas => {
            const instance = Chart.getChart(canvas);
            return {
                type: instance.config.type,
                labels: instance.data.labels,
                series: instance.data.datasets.map(series => ({ values: series.data, showLine: series.showLine })),
                stacked: instance.options.scales?.x?.stacked,
                maximum: instance.options.scales?.x?.max,
                indexAxis: instance.options.indexAxis,
                categoryOffset: instance.options.scales?.y?.offset,
                legendColors: instance.legend?.legendItems?.map(item => item.fontColor)
            };
        });
        if (data.kind === "relationship") {
            assert.equal(rendered.type, "bar");
            assert.equal(rendered.series.length, 2);
            const proportional = view === "percentage";
            assert.equal(rendered.stacked, proportional);
            if (proportional) assert.equal(rendered.maximum, 100);
            assert.deepEqual(rendered.series[0].values, data.outcomes.map(row => proportional ? row.onTimePercent : row.onTime));
            assert.deepEqual(rendered.series[1].values, data.outcomes.map(row => proportional ? row.latePercent : row.late));
            data.outcomes.forEach((row, index) => assert(rendered.labels[index].includes("n = " + row.known), "Visible category denominator"));
        } else if (view === "line") {
            assert.equal(rendered.type, "line");
            assert.equal(rendered.indexAxis, data.field === "difficulty" ? "x" : "y", "Long ordered labels need horizontal framing");
            if (data.field !== "difficulty") assert.equal(rendered.categoryOffset, true, "First/last multiline labels need space inside the canvas");
            assert.deepEqual(rendered.series[0].values, data.rows.map(row => specialLineCodes.includes(row.code) ? null : row.count));
            if (data.rows.some(row => specialLineCodes.includes(row.code))) {
                assert.equal(rendered.series[1].showLine, false, "Special answers must remain unconnected");
                assert.deepEqual(rendered.series[1].values, data.rows.map(row => specialLineCodes.includes(row.code) ? row.count : null));
            }
        } else {
            assert.equal(rendered.type, view === "pie" ? "pie" : "bar");
            if (view === "pie") assert(rendered.legendColors.every(color => color === "#aab1ad"), "Pie legends must be readable");
            assert.equal(rendered.series.length, 1);
            assert.deepEqual(rendered.series[0].values, data.rows.map(row => row.count));
        }
    };
    const localReportView = async (id, data, view) => {
        const workspace = page.locator("#" + id);
        const before = page.url();
        let navigations = 0;
        const recordNavigation = frame => { if (frame === page.mainFrame()) navigations++; };
        page.on("framenavigated", recordNavigation);
        try {
            await workspace.locator(`[data-report-view="${view}"]`).click();
            await page.waitForFunction(({ id, view }) => document.getElementById(id)?.dataset.reportCurrentView === view, { id, view });
            assert.equal(page.url(), before, "Local report view must preserve the URL");
            assert.equal(navigations, 0, "Local report view must not navigate");
            assert.equal(await workspace.locator('[data-report-view][aria-pressed="true"]').count(), 1);
            assert.equal(await workspace.locator(`[data-report-view="${view}"]`).getAttribute("aria-pressed"), "true");
            if (view === "table") {
                assert(await workspace.locator(".report-exact table").isVisible());
                assert(await workspace.locator(".report-chart").isHidden());
                const scroll = workspace.locator(".table-scroll");
                assert.equal(await scroll.getAttribute("tabindex"), "0");
                await scroll.focus();
                assert(await scroll.evaluate(element => document.activeElement === element));
            } else await assertReportChart(id, data, view);
            await assertExactTable(id, data);
        } finally { page.off("framenavigated", recordNavigation); }
    };
    const checkDistribution = result => {
        assert(result && Array.isArray(result.histogram));
        assert.equal(result.histogram.reduce((sum, bin) => sum + bin.observed, 0), result.runs);
        assert(Math.abs(result.histogram.reduce((sum, bin) => sum + bin.theoreticalProbability, 0) - 1) < 1e-9);
        assert.equal(result.firstGroup.length, result.size);
        assert.equal(result.convergence.at(-1).runs, result.runs);
        assert.equal(result.convergence.at(-1).empiricalProbability, result.empiricalTail);
    };
    const groupCount = async (name, expected, successes) => {
        const selector = `[data-lab-group="${name}"]`;
        await page.waitForFunction(({ selector, expected }) => document.querySelector(selector)?.querySelector("[data-lab-dots]")?.children.length === expected,
            { selector, expected });
        assert.equal(await page.locator(selector + " [data-lab-dots] > *").count(), expected);
        if (successes !== undefined) {
            await page.waitForFunction(({ selector, successes }) => document.querySelector(selector)?.querySelector("[data-lab-success]")?.textContent.includes(String(successes)),
                { selector, successes });
            assert((await page.locator(selector + " [data-lab-success]").innerText()).includes(String(successes)));
            assert((await page.locator(selector + " [data-lab-late]").innerText()).includes(String(expected - successes)));
        }
    };
    const screenshots = async (name, selector) => {
        if (selector) await page.locator(selector).scrollIntoViewIfNeeded();
        await page.screenshot({ path: path.join(artifacts, name + ".png") });
    };
    try {
        fs.mkdirSync(artifacts, { recursive: true });
        const routes = [
            "/?dataset=demo", "/?dataset=real", "/data?dataset=demo", "/data?dataset=real",
            "/data?dataset=demo&variable=planning&analysis=relationship&view=bars",
            "/simulator?dataset=demo", "/simulator?dataset=real",
            "/simulator?dataset=demo&source=custom&p=62.4&size=20&stage=repeat&runs=1000&threshold=12",
            "/simulator?dataset=demo&labMode=compare&stage=group&size=20",
            "/simulator?dataset=demo&labMode=compare&stage=repeat&size=20&runs=1000",
            "/method?dataset=demo", "/source?dataset=real"
        ];
        for (const lang of ["ru", "kk", "en"]) {
            for (const width of [1440, 1024, 768, 390]) {
                await page.setViewportSize({ width, height: 950 });
                for (const route of routes) {
                    await open(route + "&lang=" + lang);
                    assert.equal(await page.locator("html").getAttribute("lang"), lang);
                    assert((await page.title()).includes("Deadline Dynamics"));
                    const nav = await page.locator(".main-nav a").evaluateAll(links => links.map(link => new URL(link.href).pathname));
                    assert.deepEqual(nav, ["/", "/data", "/simulator", "/method"]);
                    const internalLinks = await page.locator("a[href]").evaluateAll(links => links.map(link => new URL(link.href).pathname)
                        .filter(path => ["/survey", "/admin", "/login", "/csv-template.csv"].includes(path)));
                    assert.deepEqual(internalLinks, [], "Internal workflows promoted publicly");
                    if (route.startsWith("/method")) await assertMethod(route, lang, width);
                    if (!route.startsWith("/source")) assert.equal(await page.locator(".dataset-notice, .lab-small-warning, .notice:not(.notice-error)").count(), 0);
                    await assertLayout(route, lang, width);
                }
                console.log("Layouts checked: " + lang + " / " + width + "px");
            }
        }
        assert.deepEqual(failures, [], "Horizontal overflow");
        console.log("PASS: " + layoutChecks + " localized responsive product layouts");

        await page.setViewportSize({ width: 1440, height: 950 });
        await open("/?dataset=demo&lang=en");
        assert.equal(await page.locator("canvas").count(), 3);
        for (const id of ["overview-startBand", "overview-planning", "overview-difficulty"]) {
            const data = await reportPayload(id);
            assert.equal(assertReportData(data), 144);
            await assertExactTable(id, data);
            for (const view of data.views) await localReportView(id, data, view);
        }
        assert.equal(await page.locator(".overview-source-facts dd").first().innerText(), "144");
        await screenshots("product-overview-desktop");

        for (const lang of ["ru", "kk", "en"]) {
            for (const width of [1440, 390]) {
                await page.setViewportSize({ width, height: 950 });
                await open(`/data?dataset=demo&lang=${lang}`);
                assert.equal(await page.locator(".report-chapter").count(), 7);
                assert.equal(await page.locator("[data-report-workspace]").count(), 13);
                assert.equal(await page.locator("main form").count(), 0);
                for (const [field, views] of Object.entries(distributionViews)) {
                    const id = "distribution-" + field;
                    const data = await reportPayload(id);
                    assert.equal(data.defaultView, views[0]);
                    assert.equal(assertReportData(data), 144);
                    assert.deepEqual(await page.locator(`#${id} [data-report-view]`).evaluateAll(buttons => buttons.map(button => button.dataset.reportView)), views);
                    for (const view of views) {
                        await localReportView(id, data, view);
                        await assertLayout(id + " " + view, lang, width);
                    }
                    if (field === "submissionStatus") continue;
                    const relationshipId = "relationship-" + field;
                    const relationship = await reportPayload(relationshipId);
                    assert.equal(assertReportData(relationship), 144);
                    for (const view of relationshipViews) {
                        await localReportView(relationshipId, relationship, view);
                        await assertLayout(relationshipId + " " + view, lang, width);
                    }
                }
            }
        }
        await page.setViewportSize({ width: 1440, height: 950 });
        await open("/data?dataset=demo&start=UNKNOWN&lang=ru");
        const dataLanguageHref = await page.locator('.language-switcher a[lang="en"]').getAttribute("href");
        const dataLanguageUrl = new URL(dataLanguageHref, base);
        for (const [key, value] of Object.entries({ dataset: "demo", start: "UNKNOWN", lang: "en" })) {
            assert.equal(dataLanguageUrl.searchParams.get(key), value);
        }
        await page.goto(dataLanguageUrl.href, { waitUntil: "networkidle" });
        assert.equal(assertReportData(await reportPayload("distribution-startBand")), 144, "Hidden legacy filters cannot hide the report");
        await page.locator('.main-nav a[href^="/method"]').click();
        assert.equal(await page.locator("html").getAttribute("lang"), "en");
        await open("/data?dataset=demo&lang=en");
        const keyboardView = page.locator('#distribution-startBand [data-report-view="line"]');
        await keyboardView.focus();
        await keyboardView.press("Enter");
        assert.equal(await keyboardView.getAttribute("aria-pressed"), "true");
        await keyboardView.press("ArrowRight");
        assert.equal(await page.locator('#distribution-startBand [data-report-view="table"]').getAttribute("aria-pressed"), "true");

        for (const source of ["overall", "earlier", "final", "custom"]) {
            await open(`/simulator?dataset=demo&source=${source}&p=62.4&size=20&stage=group&seed=42&lang=en`);
            const data = await payload("lab-experiment");
            assert.equal(data.stage, "group");
            assert.equal(await page.locator("#lab-distribution").count(), 0, "One group must precede a bulk report");
            assert.equal(data.result.firstGroup.length, 20);
            if (source === "custom") assert.equal(data.result.probability, 0.624);
            else {
                assert.equal(data.result.probability, data.selectedScenario.summary.onTime / data.selectedScenario.summary.known);
            }
            await groupCount("single", 20, data.result.firstGroup.filter(Boolean).length);
            const responsePromise = page.waitForResponse(response => new URL(response.url()).pathname === "/simulator/group" && response.status() === 200);
            await page.locator("[data-lab-another]").click();
            const another = await (await responsePromise).json();
            assert.equal(another.size, 20);
            assert(Math.abs(another.probability - data.result.probability) < 1e-12);
            await groupCount("single", 20, another.successes);
        }

        await open("/simulator?dataset=demo&lang=en");
        assert.equal(await page.locator('input[name="seed"]').inputValue(), "", "Ordinary group runs should use a random seed");
        await page.locator("[data-lab-run-group]").click();
        await page.waitForLoadState("networkidle");
        assert.equal(new URL(page.url()).searchParams.get("seed"), "");
        assert.notEqual((await payload("lab-experiment")).result.seed, 42);
        await page.locator('[data-lab-source][value="custom"]').check();
        await page.locator("[data-lab-p]").fill("150");
        await page.locator('[data-lab-source][value="overall"]').check();
        await page.waitForLoadState("networkidle");
        assert.equal((await payload("lab-experiment")).source, "overall", "Unused invalid custom input must not block observed mode");
        await page.locator('[data-lab-source][value="custom"]').check();
        await page.locator("[data-lab-p]").fill("150");
        await page.locator('[data-lab-mode][value="compare"]').check();
        await page.waitForLoadState("networkidle");
        assert.equal((await payload("lab-experiment")).mode, "compare", "Comparison uses observed probabilities, not the unused custom input");

        for (const size of [1, 100]) {
            for (const p of [0, 100]) {
                await open(`/simulator?dataset=demo&source=custom&p=${p}&size=${size}&stage=repeat&runs=100&threshold=${size}&seed=17&lang=en`);
                const data = await payload("lab-experiment");
                checkDistribution(data.result);
                assert.equal(data.result.theoreticalTail, p / 100);
                assert.equal(data.result.empiricalTail, p / 100);
                await groupCount("single", size, p === 100 ? size : 0);
                await chart("#lab-distribution", 2);
            }
        }
        await open("/simulator?dataset=demo&source=custom&p=62.4&size=20&stage=repeat&runs=10000&threshold=12&seed=123&lang=ru");
        const bulk = await payload("lab-experiment");
        checkDistribution(bulk.result);
        await chart("#lab-distribution", 2);
        for (const series of ["simulation", "theory"]) {
            const toggle = page.locator(`[data-lab-series="${series}"]`);
            await toggle.uncheck();
            assert.equal(await toggle.isChecked(), false);
            const visibleSeries = await page.locator("#lab-distribution").evaluate(canvas => {
                const chart = Chart.getChart(canvas);
                return chart.data.datasets.filter((_, index) => chart.isDatasetVisible(index)).length;
            });
            assert.equal(visibleSeries, 1);
            await toggle.check();
        }
        assert.equal(await page.locator("details[data-lab-convergence]").getAttribute("open"), null);
        await page.locator("details[data-lab-convergence] > summary").click();
        await chart("#lab-convergence", 2);
        await page.locator("#lab-distribution").scrollIntoViewIfNeeded();
        const canvasPosition = await page.locator("#lab-distribution").boundingBox();
        const thresholdPoint = await page.locator("#lab-distribution").evaluate(canvas => {
            const chart = Chart.getChart(canvas);
            return { x: chart.scales.x.getPixelForValue(8), y: chart.chartArea.bottom - 8 };
        });
        await page.mouse.click(canvasPosition.x + thresholdPoint.x, canvasPosition.y + thresholdPoint.y);
        await page.waitForLoadState("networkidle");
        assert.equal((await payload("lab-experiment")).result.threshold, 8, "Chart threshold click must request a server recalculation");
        await page.locator("[data-lab-threshold]").fill("10");
        await page.locator("[data-lab-apply-threshold]").click();
        await page.waitForLoadState("networkidle");
        const changed = await payload("lab-experiment");
        assert.equal(changed.result.threshold, 10);
        assert.equal(changed.result.runs, 10000);
        assert.equal(changed.result.probability, 0.624);
        assert.equal(changed.result.seed, 123);
        const labLanguageHref = await page.locator('.language-switcher a[lang="kk"]').getAttribute("href");
        const labLanguageUrl = new URL(labLanguageHref, base);
        for (const [key, value] of Object.entries({ source: "custom", p: "62.4", size: "20", stage: "repeat", runs: "10000", threshold: "10", seed: "123", lang: "kk" })) {
            assert.equal(labLanguageUrl.searchParams.get(key), value);
        }

        await open("/simulator?dataset=demo&labMode=compare&stage=group&size=20&seed=42&lang=en");
        let pair = await payload("lab-experiment");
        await groupCount("earlier", 20, pair.comparison.earlier.firstGroup.filter(Boolean).length);
        await groupCount("final", 20, pair.comparison.finalDay.firstGroup.filter(Boolean).length);
        const pairResponse = page.waitForResponse(response => new URL(response.url()).pathname === "/simulator/groups" && response.status() === 200);
        await page.locator("[data-lab-another]").click();
        const anotherPair = await (await pairResponse).json();
        await groupCount("earlier", 20, anotherPair.earlier.successes);
        await groupCount("final", 20, anotherPair.finalDay.successes);
        await page.locator('[data-lab-runs="10000"]').click();
        await page.locator("[data-lab-repeat]").click();
        await page.waitForLoadState("networkidle");
        pair = await payload("lab-experiment");
        assert.equal(pair.stage, "repeat");
        checkDistribution(pair.comparison.earlier);
        checkDistribution(pair.comparison.finalDay);
        assert.equal(pair.comparison.earlier.runs, 10000);
        assert.equal(pair.comparison.finalDay.runs, 10000);
        assert(pair.comparison.overlap >= 0 && pair.comparison.overlap <= 1);
        await chart("#lab-distribution", 4);
        await screenshots("product-lab-comparison-desktop", "#lab-distribution");

        for (const invalid of ["NaN", "Infinity", "-Infinity", "oops", "101", "-1"]) {
            await open("/simulator?source=custom&p=" + encodeURIComponent(invalid) + "&lang=en");
            assert.equal(await page.locator(".notice-error").count(), 1);
            assert.equal(await page.locator("#lab-distribution").count(), 0);
        }
        await open("/simulator?dataset=real&labMode=compare&stage=repeat&lang=en");
        assert.equal(await page.locator("#lab-distribution").count(), 0);
        await open("/data?dataset=real&variable=planning&analysis=relationship&lang=en");
        assert.equal(await page.locator("[data-report-workspace]").count(), 13);
        assert.equal(await page.locator(".report-empty:visible").count(), 13);
        for (const data of await page.locator("script[data-report-payload]").evaluateAll(nodes => nodes.map(node => JSON.parse(node.textContent)))) {
            assert.equal(assertReportData(data), 0);
        }

        await page.setViewportSize({ width: 390, height: 844 });
        await open("/?dataset=demo&lang=kk");
        await page.locator("[data-nav-toggle]").click();
        assert.equal(await page.locator("[data-sidebar]").getAttribute("aria-modal"), "true");
        await page.keyboard.press("Escape");
        assert.equal(await page.locator("[data-nav-toggle]").getAttribute("aria-expanded"), "false");
        await screenshots("product-overview-mobile-kk");
        await open("/data?dataset=demo&lang=kk");
        await chart("#distribution-difficulty canvas");
        await screenshots("product-data-mobile-kk", "#distribution-difficulty");
        await open("/method?dataset=demo&lang=kk");
        await assertMethod("/method", "kk", 390);
        await screenshots("product-formulas-mobile-kk", '[data-formula="expected"]');
        await open("/simulator?dataset=demo&labMode=compare&stage=group&size=100&lang=kk");
        await groupCount("earlier", 100);
        await groupCount("final", 100);
        await screenshots("product-pair-mobile-kk", "[data-lab-group='earlier']");
        await assertLayout("pair size100", "kk", 390);

        // Retained private functionality is exercised only after the isolation guards above.
        await open("/survey?lang=kk");
        await page.locator('input[name="eligible"][value="NO"]').check();
        assert(await page.locator('fieldset[data-field="startBand"]').isHidden());
        await page.locator('button[type="submit"]').click();
        await page.waitForURL("**/survey/thanks");
        assert.equal(await page.locator("html").getAttribute("lang"), "kk");
        await page.reload();
        assert(page.url().endsWith("/survey/thanks"));
        const studentContext = await browser.newContext();
        const student = await studentContext.newPage();
        try {
            await student.goto(url("/survey?lang=en"));
            for (const [name, value] of Object.entries({ eligible: "YES", allottedBand: "THREE_FOUR", startBand: "ONE", submissionStatus: "ON_TIME", extensionStatus: "NO", planning: "MENTAL", difficulty: "3", otherDeadlines: "TWO" })) {
                await student.locator(`input[name="${name}"][value="${value}"]`).check();
            }
            await student.locator('button[type="submit"]').click();
            await student.waitForURL("**/survey/thanks");
        } finally { await studentContext.close(); }
        const noCsrf = await context.request.post(url("/survey"), { form: { eligible: "NO" }, maxRedirects: 0 });
        assert.equal(noCsrf.status(), 403, "Survey writes must require CSRF");

        await page.setViewportSize({ width: 1440, height: 950 });
        await open("/login?lang=en");
        await page.locator('input[name="username"]').fill("admin");
        await page.locator('input[name="password"]').fill(process.env.TEST_ADMIN_PASSWORD);
        await page.locator('button[type="submit"]').click();
        await page.waitForURL("**/admin");
        const noAdminCsrf = await context.request.post(url("/admin/import"), { form: { format: "canonical" }, maxRedirects: 0 });
        assert.equal(noAdminCsrf.status(), 403, "Import writes must require CSRF");
        const fixture = fs.readFileSync(path.resolve(__dirname, "../src/test/resources/ui-import.csv"));
        let importedCounts;
        for (let index = 0; index < 2; index++) {
            await page.locator('select[name="format"]').selectOption("canonical");
            await page.locator('input[type="file"]').setInputFiles({ name: "ui-regression.csv", mimeType: "text/csv", buffer: fixture });
            await page.locator('form[action="/admin/import"] button[type="submit"]').click();
            await page.waitForLoadState("networkidle");
            assert.equal(await page.locator(".notice-error").count(), 0, "CSV import should succeed");
            const counts = await page.locator(".page-subtitle").innerText();
            if (index === 0) importedCounts = counts;
            else assert.equal(counts, importedCounts, "Repeated CSV must not duplicate responses");
        }
        assert.deepEqual(failures, [], "Horizontal overflow");
        assert.deepEqual(errors, [], "Browser console/page errors");
        console.log(JSON.stringify({ status: "PASS", layoutChecks, visualizationChecks, navigation: true, report: true, formulas: true,
            progressiveLab: true, pairComparison: true, localePersistence: true, survey: true, csrf: true, login: true,
            csvIdempotence: true, errors }, null, 2));
    } catch (error) {
        await page.screenshot({ path: path.join(artifacts, "product-regression-failure.png"), fullPage: true }).catch(() => {});
        fs.writeFileSync(path.join(artifacts, "product-regression-failure.json"), JSON.stringify({ url: page.url(), layoutChecks, failures, errors, error: String(error) }, null, 2));
        throw error;
    } finally { await context.close(); await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
