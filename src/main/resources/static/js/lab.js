"use strict";

(() => {
    const form = document.getElementById("lab-form");
    if (!form) return;
    const payloadElement = document.getElementById("lab-experiment");
    const payload = payloadElement ? JSON.parse(payloadElement.textContent) : null;
    const locale = document.documentElement.lang || "ru";
    const number = new Intl.NumberFormat(locale, { maximumFractionDigits: 2 });
    const percentage = value => number.format(value) + "%";
    const sizeInput = form.querySelector("[data-lab-size]");
    const pInput = form.querySelector("[data-lab-p]");
    const pRange = form.querySelector("[data-lab-p-range]");
    const thresholdInput = document.querySelector("[data-lab-threshold]");
    const thresholdRange = document.querySelector("[data-lab-threshold-range]");
    const hiddenThreshold = form.querySelector("[data-lab-hidden-threshold]");
    const runGroupButton = form.querySelector("[data-lab-run-group]");
    const thresholdButton = document.querySelector("[data-lab-apply-threshold]");

    const synchronizeProbability = input => {
        if (!input || input.value.trim() === "") return;
        const value = Number(input.value);
        if (!Number.isFinite(value) || value < 0 || value > 100) return;
        pInput.value = value;
        pRange.value = value;
        pRange.setAttribute("aria-valuetext", percentage(value));
    };
    pInput?.addEventListener("input", () => synchronizeProbability(pInput));
    pRange?.addEventListener("input", () => synchronizeProbability(pRange));
    synchronizeProbability(pInput);

    const updateSize = () => {
        if (!sizeInput) return;
        const size = Number(sizeInput.value);
        if (!Number.isInteger(size) || size < 1 || size > 100) return;
        [thresholdInput, thresholdRange, hiddenThreshold].filter(Boolean).forEach(input => {
            input.max = size;
            if (Number(input.value) > size) input.value = size;
        });
    };
    sizeInput?.addEventListener("input", updateSize);
    form.querySelectorAll("[data-lab-step]").forEach(button => button.addEventListener("click", () => {
        sizeInput.value = Math.max(1, Math.min(100, (Number(sizeInput.value) || 1) + Number(button.dataset.labStep)));
        updateSize();
    }));
    updateSize();

    form.querySelectorAll("[data-lab-source]").forEach(input => input.addEventListener("change", () => {
        const custom = input.value === "custom";
        const manual = form.querySelector("[data-lab-manual]");
        if (manual) manual.hidden = !custom;
        if (pInput) pInput.disabled = !custom;
        const legacyMode = form.querySelector("[data-lab-legacy-mode]");
        if (legacyMode) legacyMode.value = custom ? "manual" : "observed";
        form.querySelectorAll(".lab-source-options label").forEach(label => label.classList.toggle("selected", label.contains(input)));
        if (custom) pInput?.focus();
        else form.requestSubmit(runGroupButton);
    }));
    form.querySelectorAll("[data-lab-mode]").forEach(input => input.addEventListener("change", () => {
        if (pInput && input.value === "compare") pInput.disabled = true;
        form.requestSubmit(runGroupButton);
    }));

    const runsInput = form.querySelector("[data-lab-runs-input]");
    const presets = Array.from(document.querySelectorAll("[data-lab-runs]"));
    presets.forEach(button => button.addEventListener("click", () => {
        if (runsInput) runsInput.value = button.dataset.labRuns;
        presets.forEach(item => item.setAttribute("aria-pressed", String(item === button)));
    }));
    runsInput?.addEventListener("input", () => presets.forEach(button => button.setAttribute("aria-pressed", String(button.dataset.labRuns === runsInput.value))));
    const synchronizeThreshold = input => {
        if (!input || input.value.trim() === "") return;
        const threshold = Number(input.value);
        if (!Number.isInteger(threshold) || threshold < 0 || threshold > Number(sizeInput.value)) return;
        if (thresholdInput) thresholdInput.value = threshold;
        if (thresholdRange) thresholdRange.value = threshold;
    };
    thresholdInput?.addEventListener("input", () => synchronizeThreshold(thresholdInput));
    thresholdRange?.addEventListener("input", () => synchronizeThreshold(thresholdRange));
    thresholdInput?.addEventListener("keydown", event => {
        if (event.key === "Enter") { event.preventDefault(); form.requestSubmit(thresholdButton); }
    });

    if (!payload) return;
    const labels = payload.labels;
    const pair = payload.mode === "compare";
    const results = pair ? [payload.comparison.earlier, payload.comparison.finalDay] : [payload.result];
    const palettes = [{ empirical: "#49bfae", theoretical: "#5da6bd" }, { empirical: "#d0a03a", theoretical: "#8e7eb0" }];
    const groupButton = document.querySelector("[data-lab-another]");
    const groupError = document.querySelector("[data-lab-group-error]");
    const renderGroup = (code, group) => {
        const container = document.querySelector(`[data-lab-group="${code}"]`);
        if (!container) return;
        container.querySelector("[data-lab-dots]").replaceChildren(...group.outcomes.map(success => {
            const mark = document.createElement("span");
            mark.className = "lab-mark " + (success ? "is-success" : "is-late");
            mark.textContent = success ? "+" : "-";
            return mark;
        }));
        container.querySelector("[data-lab-success]").textContent = `${group.successes} / ${group.size}`;
        container.querySelector("[data-lab-late]").textContent = `${group.size - group.successes} / ${group.size}`;
    };
    const validGroup = group => group && Array.isArray(group.outcomes) && group.outcomes.length === payload.size &&
        group.size === payload.size && group.outcomes.every(value => typeof value === "boolean") &&
        Number.isInteger(group.successes) && group.successes >= 0 && group.successes <= group.size;
    groupButton?.addEventListener("click", async () => {
        const actionText = groupButton.querySelector("span");
        const previousText = actionText?.textContent;
        groupButton.disabled = true;
        groupButton.setAttribute("aria-busy", "true");
        if (actionText) actionText.textContent = labels.groupLoading;
        groupError.hidden = true;
        try {
            const url = new URL(pair ? "/simulator/groups" : "/simulator/group", location.origin);
            url.searchParams.set("size", payload.size);
            if (pair) Object.entries(payload.requestParameters).forEach(([key, value]) => {
                if (value != null && value !== "") url.searchParams.set(key, String(value));
            });
            else url.searchParams.set("p", payload.result.probability * 100);
            const response = await fetch(url, { headers: { Accept: "application/json" }, credentials: "same-origin" });
            if (!response.ok) throw new Error("Group unavailable");
            const data = await response.json();
            if (pair) {
                if (!validGroup(data.earlier) || !validGroup(data.finalDay)) throw new Error("Invalid group response");
                renderGroup("earlier", data.earlier);
                renderGroup("final", data.finalDay);
            } else {
                if (!validGroup(data)) throw new Error("Invalid group response");
                renderGroup("single", data);
            }
        } catch (error) {
            groupError.textContent = labels.groupError;
            groupError.hidden = false;
        } finally {
            groupButton.disabled = false;
            groupButton.removeAttribute("aria-busy");
            if (actionText) actionText.textContent = previousText;
        }
    });

    if (payload.stage !== "repeat" || !window.Chart) return;
    const baseOptions = (x, y) => ({
        responsive: true, maintainAspectRatio: false, animation: false,
        interaction: { mode: "index", intersect: false },
        plugins: { legend: { position: "bottom", labels: { boxWidth: 10, padding: 14, font: { size: 13 } } },
            tooltip: { callbacks: { label: context => context.dataset.label + ": " + percentage(context.parsed.y) } } },
        scales: {
            x: { title: { display: true, text: x, font: { size: 13 } }, grid: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 16, font: { size: 12 } } },
            y: { beginAtZero: true, title: { display: true, text: y, font: { size: 13 } }, ticks: { maxTicksLimit: 6, callback: percentage, font: { size: 12 } }, grid: { color: "rgba(255,255,255,.055)" } }
        }
    });
    const seriesName = (index, theoretical) => (pair ? (index === 0 ? labels.earlier : labels.final) + " · " : "") + (theoretical ? labels.theory : labels.simulation);
    const distributionDatasets = results.flatMap((result, index) => [
        { labSeries: "simulation", label: seriesName(index, false), data: result.histogram.map(bin => bin.empiricalProbability * 100),
            backgroundColor: palettes[index].empirical, borderWidth: 0, maxBarThickness: 28, order: 2 },
        { labSeries: "theory", type: "line", label: seriesName(index, true), data: result.histogram.map(bin => bin.theoreticalProbability * 100),
            borderColor: palettes[index].theoretical, borderDash: index === 1 ? [5, 3] : [], borderWidth: 2, pointRadius: 0, pointHoverRadius: 4, tension: 0, order: 1 }
    ]);
    // The shaded tail and reference means use only values calculated by the server.
    const researchMarkers = {
        id: "researchMarkers",
        beforeDatasetsDraw(chart) {
            const { ctx, chartArea, scales } = chart;
            const threshold = results[0].threshold;
            const unit = chartArea.width / (results[0].size + 1);
            const marker = scales.x.getPixelForValue(threshold) - unit / 2;
            ctx.save();
            ctx.fillStyle = "rgba(208,160,58,.055)";
            ctx.fillRect(Math.max(chartArea.left, marker), chartArea.top, chartArea.right - Math.max(chartArea.left, marker), chartArea.height);
            ctx.strokeStyle = "rgba(208,160,58,.55)";
            ctx.lineWidth = 1;
            ctx.setLineDash([3, 3]);
            ctx.beginPath(); ctx.moveTo(marker, chartArea.top); ctx.lineTo(marker, chartArea.bottom); ctx.stroke();
            if (pair) results.forEach((result, index) => {
                const x = scales.x.getPixelForValue(result.expected);
                ctx.strokeStyle = palettes[index].empirical;
                ctx.setLineDash([2, 4]);
                ctx.beginPath(); ctx.moveTo(x, chartArea.top); ctx.lineTo(x, chartArea.bottom); ctx.stroke();
            });
            ctx.restore();
        }
    };
    const distributionOptions = baseOptions(labels.x, labels.y);
    distributionOptions.onClick = (event, elements, chart) => {
        const selected = elements.length ? elements[0] : chart.getElementsAtEventForMode(event, "nearest", { intersect: false }, true)[0];
        if (!selected || !thresholdInput || !thresholdButton) return;
        thresholdInput.value = results[0].histogram[selected.index].value;
        synchronizeThreshold(thresholdInput);
        form.requestSubmit(thresholdButton);
    };
    const distribution = new Chart(document.getElementById("lab-distribution"), {
        type: "bar", data: { labels: results[0].histogram.map(bin => bin.value), datasets: distributionDatasets },
        options: distributionOptions, plugins: [researchMarkers]
    });
    document.querySelectorAll("[data-lab-series]").forEach(input => input.addEventListener("change", () => {
        distribution.data.datasets.forEach((dataset, index) => {
            if (dataset.labSeries === input.dataset.labSeries) distribution.setDatasetVisibility(index, input.checked);
        });
        distribution.update("none");
    }));

    let convergence = null;
    const disclosure = document.querySelector("[data-lab-convergence]");
    disclosure?.addEventListener("toggle", () => {
        if (!disclosure.open) return;
        if (!convergence) {
            const convergenceOptions = baseOptions(labels.runs, labels.tail);
            convergenceOptions.scales.x.type = "linear";
            convergenceOptions.scales.x.ticks.callback = value => number.format(value);
            convergenceOptions.scales.x.ticks.maxTicksLimit = 6;
            convergenceOptions.scales.y.min = 0;
            convergenceOptions.scales.y.max = 100;
            const datasets = results.flatMap((result, index) => [
                { labSeries: "simulation", label: seriesName(index, false), data: result.convergence.map(point => ({ x: point.runs, y: point.empiricalProbability * 100 })),
                    borderColor: palettes[index].empirical, borderWidth: 2, pointRadius: 0, pointHoverRadius: 4, tension: 0 },
                { labSeries: "theory", label: seriesName(index, true), data: result.convergence.map(point => ({ x: point.runs, y: result.theoreticalTail * 100 })),
                    borderColor: palettes[index].theoretical, borderDash: [5, 3], borderWidth: 2, pointRadius: 0 }
            ]);
            convergence = new Chart(document.getElementById("lab-convergence"), { type: "line", data: { datasets }, options: convergenceOptions });
        }
        requestAnimationFrame(() => convergence.resize());
    });
})();
