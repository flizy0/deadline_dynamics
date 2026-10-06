import { cp, copyFile, mkdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const publicDir = path.join(root, "public");
const distDir = path.join(root, "dist");
const backendAssets = path.join(root, "src", "main", "resources", "static");
const requiredPages = ["index.html", "data/index.html", "simulator/index.html", "method/index.html"];

for (const relative of requiredPages) {
  await readFile(path.join(publicDir, relative));
}

const studySource = await readFile(path.join(publicDir, "assets", "js", "study-data.js"), "utf8");
const sandbox = { window: {} };
vm.runInNewContext(studySource, sandbox, { timeout: 1000 });
const data = sandbox.window.STUDY_DATA;
if (!data || data.sourceResponses !== data.eligible + data.ineligible || data.known !== data.onTime + data.late || data.eligible !== data.known + data.pending) {
  throw new Error("The aggregate dataset totals do not reconcile.");
}
for (const variable of data.variables) {
  const total = variable.categories.reduce((sum, category) => sum + category.count, 0);
  if (total !== data.eligible) throw new Error(`${variable.key} totals ${total}, expected ${data.eligible}.`);
  if (variable.key !== "outcome" && variable.categories.some(category => category.onTime + category.late + category.pending !== category.count)) {
    throw new Error(`${variable.key} outcome counts do not match its category totals.`);
  }
}

const siteSource = await readFile(path.join(publicDir, "assets", "js", "site.js"), "utf8");
const siteCopySource = await readFile(path.join(publicDir, "assets", "js", "site-copy.js"), "utf8");
const dictionaryBoundary = siteSource.indexOf("  const langParam =");
if (dictionaryBoundary < 0) throw new Error("Could not locate the localization dictionaries.");
const dictionarySandbox = { window: {} };
vm.runInNewContext(siteCopySource, dictionarySandbox, { timeout: 1000 });
vm.runInNewContext(`${siteSource.slice(0, dictionaryBoundary)}\nwindow.__translations = copy;\n})();`, dictionarySandbox, { timeout: 1000 });
const translations = dictionarySandbox.window.__translations;
const publicHtml = await Promise.all(requiredPages.map(relative => readFile(path.join(publicDir, relative), "utf8")));
const htmlKeys = publicHtml.flatMap(html => [...html.matchAll(/data-i18n(?:-aria)?="([^"]+)"/g)].map(match => match[1]));
const literalScriptKeys = [...siteSource.matchAll(/\bt\(["']([^"']+)["']\)/g)].map(match => match[1]);
const categoryKeys = data.variables.flatMap(variable => variable.categories.map(category => `cat.${category.code}`));
const variableKeys = data.variables.map(variable => `data.variable.${variable.key}`);
const dynamicKeys = ["view.simpleBar", "view.line", "view.pie", "view.percentageBar", "view.multipleBar", "view.table", "data.recommended"];
const methodHtml = publicHtml[3];
if ((methodHtml.match(/class="formula-card"/g) || []).length !== 2) throw new Error("The method page must contain the two week 1-5 formulas.");
if ((methodHtml.match(/data-i18n="method\.example\.limitations\.(?:one|two|three)"/g) || []).length !== 3) throw new Error("The method page must show exactly three limitations.");
if (/<select\b/.test(publicHtml[1]) || !publicHtml[1].includes('id="dataChapters"')) throw new Error("The data page must use independent, local chart controls.");
for (const locale of ["ru", "kk", "en"]) {
  if (translations[locale]["brand.title"] !== "Deadline Dynamics" || translations[locale]["brand.course"] !== "Student Work Timing & Submission Study") {
    throw new Error(`The public brand strings must remain fixed in ${locale}.`);
  }
}
const expectedOrder = ["allotted", "start", "outcome", "extension", "planning", "difficulty", "otherDeadlines"];
if (data.variables.map(variable => variable.key).filter(key => expectedOrder.includes(key)).sort((a, b) => expectedOrder.indexOf(a) - expectedOrder.indexOf(b)).join(",") !== expectedOrder.join(",")) {
  throw new Error("The aggregate data is missing a required variable for the data page.");
}
for (const locale of ["ru", "kk", "en"]) {
  const missing = [...new Set([...htmlKeys, ...literalScriptKeys, ...categoryKeys, ...variableKeys, ...dynamicKeys])].filter(key => !translations[locale][key]);
  if (missing.length) throw new Error(`Missing ${locale} translations: ${missing.join(", ")}`);
}

await mkdir(distDir, { recursive: true });
await cp(publicDir, distDir, { recursive: true, force: true });
await mkdir(path.join(distDir, "assets", "css"), { recursive: true });
await mkdir(path.join(distDir, "assets", "js"), { recursive: true });
await copyFile(path.join(backendAssets, "css", "app.css"), path.join(distDir, "assets", "css", "app.css"));
await copyFile(path.join(backendAssets, "js", "app.js"), path.join(distDir, "assets", "js", "app.js"));
await copyFile(path.join(backendAssets, "js", "lucide.min.js"), path.join(distDir, "assets", "js", "lucide.min.js"));
await copyFile(path.join(backendAssets, "js", "chart.umd.min.js"), path.join(distDir, "assets", "js", "chart.umd.min.js"));
await copyFile(path.join(backendAssets, "js", "chart.LICENSE.md"), path.join(distDir, "assets", "js", "chart.LICENSE.md"));
await copyFile(path.join(backendAssets, "js", "lucide.LICENSE"), path.join(distDir, "assets", "js", "lucide.LICENSE"));
await copyFile(path.join(backendAssets, "favicon.svg"), path.join(distDir, "favicon.svg"));

console.log(`Static site built in dist/ (${requiredPages.length} routes).`);
