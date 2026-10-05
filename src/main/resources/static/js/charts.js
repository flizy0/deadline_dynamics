"use strict";

(() => {
    const locale = document.documentElement.lang || "ru";
    const format = new Intl.NumberFormat(locale, { maximumFractionDigits: 1 });
    const colors = { empirical: "#49bfae", theoretical: "#5da6bd", tail: "#d0a03a", late: "#c8766f", grid: "rgba(255,255,255,.055)" };
    const charts = new Map();
    const percent = value => format.format(value) + "%";
    const wrap = label => {
        const words = String(label).split(" ");
        const lines = [];
        let line = "";
        for (const word of words) {
            if (line && (line + " " + word).length > 23) { lines.push(line); line = word; }
            else line += (line ? " " : "") + word;
        }
        if (line) lines.push(line);
        return lines;
    };

    if (window.Chart) {
        Chart.defaults.color = "#929a96";
        Chart.defaults.font.family = 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
        Chart.defaults.font.size = 11;
        Chart.defaults.animation = false;
        Chart.defaults.borderColor = colors.grid;
        Chart.defaults.plugins.legend.labels.usePointStyle = true;
        Chart.defaults.plugins.legend.labels.boxWidth = 8;
        Chart.defaults.plugins.legend.labels.padding = 14;
        Object.assign(Chart.defaults.plugins.tooltip, {
            backgroundColor: "#121615", borderColor: "rgba(255,255,255,.12)", borderWidth: 1,
            titleColor: "#e8ece9", bodyColor: "#aab1ad", cornerRadius: 6, padding: 10
        });
    }

    const options = (xTitle, yTitle, horizontal = false) => ({
        responsive: true, maintainAspectRatio: false, animation: false,
        interaction: { mode: "index", intersect: false },
        indexAxis: horizontal ? "y" : "x",
        plugins: { legend: { position: "bottom" } },
        scales: {
            x: { beginAtZero: true, title: { display: !!xTitle, text: xTitle }, grid: { display: !horizontal }, ticks: { maxRotation: 0 } },
            y: { beginAtZero: true, title: { display: !!yTitle, text: yTitle }, grid: { display: !horizontal }, ticks: horizontal ? { autoSkip: false } : { maxTicksLimit: 6 } }
        }
    });
    const create = (id, config) => {
        const canvas = document.getElementById(id);
        if (!canvas || !window.Chart) return null;
        if (charts.has(id)) charts.get(id).destroy();
        if (config.options.indexAxis === "y") {
            const labels = config.data.labels || [];
            const lineCount = Math.max(1, ...labels.map(label => Array.isArray(label) ? label.length : 1));
            // Leave room for every ordered category rather than silently skipping axis labels.
            const minimum = labels.length * Math.max(24, lineCount * 14 + 6) + 60;
            canvas.parentElement.style.minHeight = minimum + "px";
        }
        const chart = new Chart(canvas, config);
        charts.set(id, chart);
        return chart;
    };
    const resizeVisible = () => requestAnimationFrame(() => {
        charts.forEach(chart => { if (chart.canvas.getClientRects().length) chart.resize(); });
    });

    // Keyboard and pointer interactions share the same tab state.
    const wireTabs = (buttons, activate) => {
        buttons.forEach((button, index) => {
            button.setAttribute("role", "tab");
            button.tabIndex = button.getAttribute("aria-selected") === "true" || index === 0 ? 0 : -1;
            button.addEventListener("click", () => activate(button));
            button.addEventListener("keydown", event => {
                let target;
                if (event.key === "ArrowRight" || event.key === "ArrowDown") target = buttons[(index + 1) % buttons.length];
                else if (event.key === "ArrowLeft" || event.key === "ArrowUp") target = buttons[(index + buttons.length - 1) % buttons.length];
                else if (event.key === "Home") target = buttons[0];
                else if (event.key === "End") target = buttons[buttons.length - 1];
                if (target) { event.preventDefault(); activate(target); target.focus(); }
            });
        });
    };
    const selectTab = (buttons, current) => buttons.forEach(button => {
        const selected = button === current;
        button.setAttribute("aria-selected", String(selected));
        button.tabIndex = selected ? 0 : -1;
        button.classList.toggle("active", selected);
    });

    const readPayload = id => {
        const element = document.getElementById(id);
        return element ? JSON.parse(element.textContent) : null;
    };
    const study = readPayload("study-charts");
    if (study) {
        const text = study.text;
        const showStart = mode => {
            const percentage = mode === "percent";
            const chartOptions = options(percentage ? text.percent : text.count, "", true);
            chartOptions.plugins.legend.display = false;
            chartOptions.scales.x.grid.display = true;
            chartOptions.scales.x.max = percentage ? 100 : undefined;
            chartOptions.scales.x.ticks.callback = value => percentage ? percent(value) : format.format(value);
            chartOptions.plugins.tooltip = { callbacks: { label: context => percentage ? percent(context.parsed.x) : format.format(context.parsed.x) } };
            create("start-distribution", { type: "bar", data: {
                labels: study.start.labels.map(wrap),
                datasets: [{ label: percentage ? text.percent : text.count, data: percentage ? study.start.percentages : study.start.counts, backgroundColor: colors.empirical, maxBarThickness: 12 }]
            }, options: chartOptions });
        };
        if (study.start) {
            showStart("count");
            const buttons = Array.from(document.querySelectorAll('[data-chart-switch="start"]'));
            const panel = document.getElementById("start-distribution").parentElement;
            panel.id = "start-chart-panel";
            panel.setAttribute("role", "tabpanel");
            buttons.forEach((button, index) => {
                button.id = "start-tab-" + index;
                button.setAttribute("aria-controls", panel.id);
            });
            wireTabs(buttons, button => { selectTab(buttons, button); panel.setAttribute("aria-labelledby", button.id); showStart(button.dataset.mode); });
            if (buttons.length) selectTab(buttons, buttons[0]);
            if (buttons.length) panel.setAttribute("aria-labelledby", buttons[0].id);
        }
        if (study.comparison) {
            const chartOptions = options(text.percent, "", true);
            chartOptions.scales.x.max = 100;
            chartOptions.scales.x.stacked = true;
            chartOptions.scales.y.stacked = true;
            chartOptions.scales.x.grid.display = true;
            chartOptions.scales.x.ticks.callback = percent;
            chartOptions.plugins.tooltip = { callbacks: { label: context => context.dataset.label + ": " + percent(context.parsed.x) } };
            create("group-comparison", { type: "bar", data: { labels: study.comparison.labels.map(wrap), datasets: [
                { label: text.onTime, data: study.comparison.onTime, backgroundColor: colors.empirical, maxBarThickness: 26 },
                { label: text.late, data: study.comparison.late, backgroundColor: colors.late, maxBarThickness: 26 }
            ] }, options: chartOptions });
        }
        const frequencyData = Array.from(document.querySelectorAll(".frequency-chart-data"), element => JSON.parse(element.textContent));
        (study.frequencies || []).concat(frequencyData).forEach(item => {
            const field = item.field || item.id;
            const canvasId = item.field ? item.id : "frequency-" + item.id;
            const buttons = Array.from(document.querySelectorAll(`[data-view-target="${field}"]`));
            const panels = Array.from(document.querySelectorAll(`[data-view-panel="${field}"]`));
            if (!buttons.length) return;
            buttons.forEach(button => {
                button.id = item.id + "-tab-" + button.dataset.view;
                button.setAttribute("aria-controls", item.id + "-panel-" + button.dataset.view);
            });
            panels.forEach(panel => {
                panel.id = item.id + "-panel-" + panel.dataset.view;
                panel.setAttribute("role", "tabpanel");
                panel.setAttribute("aria-labelledby", item.id + "-tab-" + panel.dataset.view);
                panel.tabIndex = 0;
            });
            wireTabs(buttons, button => {
                selectTab(buttons, button);
                panels.forEach(panel => { panel.hidden = panel.dataset.view !== button.dataset.view; });
                if (button.dataset.view === "bars" && !charts.has(canvasId)) {
                    const chartOptions = options(text.percent, "", true);
                    chartOptions.scales.x.max = 100;
                    chartOptions.scales.x.grid.display = true;
                    chartOptions.scales.x.ticks.callback = percent;
                    chartOptions.plugins.legend.display = false;
                    chartOptions.plugins.tooltip = { callbacks: { label: context => `${percent(context.parsed.x)} (${format.format(item.counts[context.dataIndex])})` } };
                    create(canvasId, { type: "bar", data: { labels: item.labels.map(wrap), datasets: [{ label: text.percent, data: item.percentages, backgroundColor: colors.empirical, maxBarThickness: 13 }] }, options: chartOptions });
                }
                resizeVisible();
            });
            if (buttons.length) selectTab(buttons, buttons[0]);
        });
    }

    const experiment = readPayload("probability-experiment");
    if (experiment) {
        const labels = experiment.labels;
        const distribution = tail => {
            const chartOptions = options(labels.xAxis, labels.yAxis);
            chartOptions.scales.y.ticks.callback = percent;
            chartOptions.plugins.tooltip = { callbacks: { label: context => context.dataset.label + ": " + percent(context.parsed.y) } };
            return { type: "bar", data: { labels: experiment.histogram.map(bin => bin.value), datasets: [
                { label: labels.empirical, data: experiment.histogram.map(bin => 100 * bin.empiricalProbability), backgroundColor: experiment.histogram.map(bin => tail && bin.value >= experiment.threshold ? colors.tail : colors.empirical), borderWidth: 0, order: 2 },
                { type: "line", label: labels.theoretical, data: experiment.histogram.map(bin => 100 * bin.theoreticalProbability), borderColor: colors.theoretical, borderWidth: 2, pointRadius: 0, pointHoverRadius: 4, tension: 0, order: 1 }
            ] }, options: chartOptions };
        };
        const showExperiment = view => {
            if (view === "distribution" && !charts.has("experiment-distribution")) create("experiment-distribution", distribution(false));
            if (view === "tail" && !charts.has("experiment-tail")) create("experiment-tail", distribution(true));
            if (view === "convergence" && !charts.has("experiment-convergence")) {
                const chartOptions = options(labels.runsAxis, labels.tailAxis);
                chartOptions.scales.x.type = "linear";
                chartOptions.scales.x.ticks.callback = value => format.format(value);
                chartOptions.scales.y.min = 0;
                chartOptions.scales.y.max = 100;
                chartOptions.scales.y.ticks.callback = percent;
                chartOptions.plugins.tooltip = { callbacks: { title: items => labels.runsAxis + ": " + format.format(items[0].parsed.x), label: context => context.dataset.label + ": " + percent(context.parsed.y) } };
                create("experiment-convergence", { type: "line", data: { datasets: [
                    { label: labels.empirical, data: experiment.convergence.map(point => ({ x: point.runs, y: 100 * point.empiricalProbability })), borderColor: colors.empirical, borderWidth: 2, pointRadius: 0, pointHoverRadius: 4, tension: 0 },
                    { label: labels.theoretical, data: experiment.convergence.map(point => ({ x: point.runs, y: 100 * experiment.theoreticalTail })), borderColor: colors.theoretical, borderDash: [6, 4], borderWidth: 2, pointRadius: 0 }
                ] }, options: chartOptions });
            }
            resizeVisible();
        };
        const buttons = Array.from(document.querySelectorAll("[data-viz-tab]"));
        const panels = Array.from(document.querySelectorAll("[data-viz-panel]"));
        wireTabs(buttons, button => {
            selectTab(buttons, button);
            panels.forEach(panel => { panel.hidden = panel.dataset.vizPanel !== button.dataset.vizTab; });
            showExperiment(button.dataset.vizTab);
        });
        if (buttons.length) selectTab(buttons, buttons[0]);
        showExperiment("distribution");

        const dots = document.querySelector("[data-experiment-dots]");
        const result = document.querySelector("[data-group-result]");
        const renderGroup = (outcomes, successes) => {
            if (!dots || !result) return;
            dots.replaceChildren(...outcomes.map((success, index) => {
                const dot = document.createElement("span");
                dot.className = "student-dot" + (success ? " student-success" : "");
                dot.setAttribute("role", "img");
                dot.setAttribute("aria-label", (index + 1) + ": " + (success ? labels.success : labels.failure));
                dot.textContent = success ? "+" : "−";
                return dot;
            }));
            result.textContent = labels.groupResult.replace("{successes}", successes).replace("{size}", outcomes.length);
        };
        if (Array.isArray(experiment.firstGroup)) renderGroup(experiment.firstGroup, experiment.firstGroup.filter(Boolean).length);
        const runGroup = document.querySelector("[data-run-group]");
        const groupError = document.querySelector("[data-group-error]");
        if (runGroup) runGroup.addEventListener("click", async () => {
            runGroup.disabled = true;
            if (groupError) { groupError.hidden = true; groupError.textContent = ""; }
            try {
                const url = new URL("/simulator/group", location.origin);
                url.searchParams.set("p", experiment.probability * 100);
                url.searchParams.set("size", experiment.size);
                const response = await fetch(url, { headers: { Accept: "application/json" }, credentials: "same-origin" });
                if (!response.ok) throw new Error("Group request failed");
                const group = await response.json();
                if (!Array.isArray(group.outcomes) || !Number.isFinite(group.successes)) throw new Error("Invalid group response");
                renderGroup(group.outcomes, group.successes);
            } catch (error) {
                if (groupError) { groupError.textContent = labels.groupError; groupError.hidden = false; }
            } finally { runGroup.disabled = false; }
        });
    }

    const probabilityInput = document.querySelector("[data-probability-input]");
    const probabilitySlider = document.querySelector("[data-probability-slider]");
    const probabilityOutput = document.querySelector("[data-probability-output]");
    const syncProbability = source => {
        if (source.value.trim() === "") return;
        const value = Number(source.value);
        if (!Number.isFinite(value) || value < 0 || value > 100) return;
        if (probabilitySlider) probabilitySlider.value = value;
        if (probabilityInput) probabilityInput.value = value;
        if (probabilityOutput) probabilityOutput.textContent = percent(value);
        if (probabilitySlider) probabilitySlider.style.setProperty("--range-fill", value + "%");
    };
    if (probabilityInput) probabilityInput.addEventListener("input", () => syncProbability(probabilityInput));
    if (probabilitySlider) probabilitySlider.addEventListener("input", () => syncProbability(probabilitySlider));
    if (probabilityInput) syncProbability(probabilityInput);
    const sizeInput = document.querySelector("[data-size-input]");
    const thresholdInput = document.querySelector("[data-threshold-input]");
    const updateSize = () => {
        if (!sizeInput || !thresholdInput) return;
        thresholdInput.max = sizeInput.value;
        if (+thresholdInput.value > +sizeInput.value) thresholdInput.value = sizeInput.value;
    };
    if (sizeInput) sizeInput.addEventListener("input", updateSize);
    document.querySelectorAll("[data-size-step]").forEach(button => button.addEventListener("click", () => {
        if (!sizeInput) return;
        sizeInput.value = Math.max(1, Math.min(100, (+sizeInput.value || 1) + +button.dataset.sizeStep));
        updateSize();
    }));
    const runsInput = document.querySelector("[data-runs-input]");
    document.querySelectorAll("[data-runs-preset]").forEach(button => button.addEventListener("click", () => {
        if (runsInput) runsInput.value = button.dataset.runsPreset;
        document.querySelectorAll("[data-runs-preset]").forEach(item => item.setAttribute("aria-pressed", String(item === button)));
    }));
    const manualPanel = document.querySelector("[data-manual-probability]");
    const updateMode = () => {
        const selected = document.querySelector('input[name="mode"]:checked');
        if (manualPanel) manualPanel.hidden = !!selected && selected.value !== "manual";
    };
    document.querySelectorAll('input[name="mode"]').forEach(input => input.addEventListener("change", updateMode));
    updateMode();
})();
