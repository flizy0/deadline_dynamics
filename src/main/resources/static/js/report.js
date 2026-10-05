"use strict";

(() => {
    const mounted = new WeakMap();
    const tablePayloads = new WeakMap();
    const number = new Intl.NumberFormat(document.documentElement.lang || "ru", { maximumFractionDigits: 1 });
    const colors = { teal: "#49bfae", blue: "#5da6bd", coral: "#c8766f", amber: "#d0a03a", sage: "#7f9d86", gray: "#98a09c", purple: "#8e7eb0" };
    const specialCodes = new Set(["UNKNOWN", "NOT_STARTED", "AFTER"]);
    let nextId = 0;
    const wrap = (label, length) => {
        const lines = [];
        let line = "";
        String(label).split(/\s+/).forEach(word => {
            if (line && line.length + word.length + 1 > length) { lines.push(line); line = ""; }
            while (word.length > length) {
                if (line) { lines.push(line); line = ""; }
                lines.push(word.slice(0, length));
                word = word.slice(length);
            }
            line += (line ? " " : "") + word;
        });
        if (line) lines.push(line);
        return lines;
    };
    const categoryColor = (field, code, index, distinct = false) => {
        if (code === "UNKNOWN" || code === "NOT_STARTED") return colors.gray;
        if (code === "AFTER") return colors.coral;
        if (field === "submissionStatus") return ({ ON_TIME: colors.teal, LATE: colors.coral, NOT_SUBMITTED: colors.amber })[code] || colors.gray;
        if (field === "extensionStatus") return code === "NO" ? colors.teal : colors.amber;
        if (distinct && field === "planning") return ({ WRITTEN: colors.teal, MENTAL: colors.blue, NONE: colors.sage })[code] || colors.gray;
        return distinct ? [colors.teal, colors.blue, colors.sage, colors.amber, colors.purple, colors.coral][index % 6] : colors.teal;
    };
    const cell = (tag, value, scope) => {
        const element = document.createElement(tag);
        element.textContent = String(value);
        if (scope) element.scope = scope;
        return element;
    };
    const ensureTable = (workspace, payload, label, percent) => {
        let exact = workspace.querySelector(".report-exact");
        if (!exact) {
            exact = document.createElement("details");
            exact.className = "report-exact";
            const summary = document.createElement("summary");
            summary.textContent = payload.text.exact || payload.text.category;
            exact.append(summary);
            workspace.append(exact);
        }
        if (!exact.id) exact.id = "report-exact-" + (++nextId);
        const signature = JSON.stringify([payload.kind, payload.labels, payload.rows, payload.outcomes, payload.text]);
        const previous = tablePayloads.get(exact);
        if (exact.querySelector("table") && (previous == null || previous === signature)) {
            tablePayloads.set(exact, signature);
            return exact;
        }
        exact.querySelectorAll(".table-scroll").forEach(scroll => scroll.remove());
        const relationship = payload.kind === "relationship";
        const text = payload.text;
        const headers = relationship
            ? [text.category, text.count, text.known, text.onTime, text.late, ...(text.pending ? [text.pending] : []), ...(text.unknown ? [text.unknown] : []), text.onTime + ", %", text.late + ", %"]
            : [text.category, text.count, text.percent];
        const table = document.createElement("table");
        const caption = document.createElement("caption");
        caption.className = "sr-only";
        caption.textContent = workspace.querySelector("canvas")?.getAttribute("aria-label") || text.category;
        const head = document.createElement("thead");
        const heading = document.createElement("tr");
        headers.forEach(title => heading.append(cell("th", title, "col")));
        head.append(heading);
        const body = document.createElement("tbody");
        (relationship ? payload.outcomes : payload.rows).forEach(row => {
            const values = relationship
                ? [row.total, row.known, row.onTime, row.late, ...(text.pending ? [row.pending] : []), ...(text.unknown ? [row.unknown] : []), percent(row.onTimePercent), percent(row.latePercent)]
                : [row.count, percent(row.percent)];
            const tr = document.createElement("tr");
            tr.append(cell("th", label(row), "row"));
            values.forEach(value => tr.append(cell("td", value)));
            body.append(tr);
        });
        table.append(caption, head, body);
        const scroll = document.createElement("div");
        scroll.className = "table-scroll";
        scroll.append(table);
        exact.append(scroll);
        tablePayloads.set(exact, signature);
        return exact;
    };
    const mount = (workspace, input) => {
        if (!workspace || !input) return null;
        mounted.get(workspace)?.destroy();
        const payload = { rows: [], outcomes: [], labels: [], text: {}, ...input };
        const relationship = payload.kind === "relationship";
        const text = payload.text;
        const percent = value => value == null ? text.noData : number.format(value) + "%";
        const labelByCode = new Map(payload.rows.map((row, index) => [row.code, payload.labels[index] || row.code]));
        const label = row => {
            const value = labelByCode.get(row.code) || row.code;
            return payload.field === "difficulty" && row.code !== "UNKNOWN" ? row.code + ". " + value : value;
        };
        const available = relationship ? ["percentage", "multiple", "table"] : ["bars", "line", "pie", "table"];
        const views = (payload.views || available).filter(view => available.includes(view));
        if (!views.length) views.push("table");
        const allButtons = Array.from(workspace.querySelectorAll("[data-report-view]"));
        allButtons.forEach(button => {
            const supported = views.includes(button.dataset.reportView);
            button.hidden = !supported;
            button.disabled = !supported;
            if (!supported) { button.setAttribute("aria-pressed", "false"); button.tabIndex = -1; }
        });
        const buttons = allButtons.filter(button => views.includes(button.dataset.reportView));
        let frame = workspace.querySelector(".report-chart");
        if (!frame) {
            frame = document.createElement("div");
            frame.className = "report-chart";
            workspace.append(frame);
        }
        let canvas = frame.querySelector("canvas");
        if (!canvas) {
            canvas = document.createElement("canvas");
            canvas.setAttribute("role", "img");
            canvas.setAttribute("aria-label", workspace.querySelector("h2, h3")?.textContent || text.category);
            frame.append(canvas);
        }
        const exact = ensureTable(workspace, payload, label, percent);
        canvas.setAttribute("aria-describedby", exact.id);
        if (!frame.id) frame.id = "report-chart-" + (++nextId);
        buttons.forEach(button => button.setAttribute("aria-controls", frame.id + " " + exact.id));
        exact.querySelectorAll(".table-scroll").forEach(scroll => {
            scroll.tabIndex = 0;
            scroll.setAttribute("role", "region");
            scroll.setAttribute("aria-label", scroll.querySelector("caption")?.textContent || text.category);
        });
        const empty = document.createElement("p");
        empty.className = "report-empty";
        empty.textContent = text.noData;
        empty.hidden = true;
        frame.after(empty);
        let chart = null;
        let currentView = payload.defaultView || views[0];
        let resizeFrame = 0;
        let lastWidth = workspace.clientWidth;
        let observer = null;
        const listeners = [];
        const on = (element, name, handler) => { element.addEventListener(name, handler); listeners.push(() => element.removeEventListener(name, handler)); };
        const baseOptions = horizontal => ({
            responsive: true, maintainAspectRatio: false, animation: false,
            indexAxis: horizontal ? "y" : "x",
            color: "#aab1ad",
            interaction: { mode: "index", intersect: false },
            plugins: {
                legend: { display: relationship, position: "bottom", labels: { color: "#aab1ad", font: { size: 13 }, padding: 16, boxWidth: 12, boxHeight: 12 } },
                tooltip: { titleFont: { size: 14 }, bodyFont: { size: 13 }, callbacks: {} }
            },
            scales: {
                x: { beginAtZero: true, title: { display: true, text: horizontal ? text.count : text.category, color: "#aab1ad", font: { size: 13 } }, grid: { display: horizontal, color: "rgba(255,255,255,.075)" }, ticks: { color: "#aab1ad", maxRotation: 0, autoSkip: false, font: { size: 13 } } },
                y: { beginAtZero: true, title: { display: true, text: horizontal ? text.category : text.count, color: "#aab1ad", font: { size: 13 } }, grid: { display: !horizontal, color: "rgba(255,255,255,.075)" }, ticks: { color: "#aab1ad", autoSkip: false, precision: 0, font: { size: 13 } } }
            }
        });
        const show = requested => {
            const view = views.includes(requested) ? requested : views[0];
            currentView = view;
            workspace.dataset.reportCurrentView = view;
            buttons.forEach(button => {
                const selected = button.dataset.reportView === view;
                button.setAttribute("aria-pressed", String(selected));
                button.tabIndex = selected ? 0 : -1;
            });
            chart?.destroy();
            chart = null;
            const rows = relationship ? payload.outcomes : payload.rows;
            const hasValues = relationship ? rows.some(row => row.known > 0) : rows.some(row => row.count > 0);
            frame.hidden = view === "table" || !hasValues || !window.Chart;
            empty.hidden = view === "table" || hasValues;
            exact.open = view === "table" || !window.Chart;
            if (frame.hidden) return;
            const horizontal = view !== "pie" && (view !== "line" || payload.field !== "difficulty");
            const narrow = workspace.clientWidth < 520;
            const labels = rows.map(row => {
                const tickLabel = view === "line" && payload.field === "difficulty" && row.code !== "UNKNOWN" ? row.code : label(row);
                const lines = wrap(tickLabel, horizontal ? (narrow ? 17 : 28) : (narrow ? 8 : 21));
                if (relationship) lines.push("n = " + number.format(row.known));
                return lines;
            });
            const chartOptions = baseOptions(horizontal);
            const valueAxis = horizontal ? chartOptions.scales.x : chartOptions.scales.y;
            const percentage = view === "percentage";
            valueAxis.title.text = percentage ? text.percent : text.count;
            valueAxis.ticks.precision = percentage ? undefined : 0;
            if (percentage) { valueAxis.max = 100; valueAxis.ticks.callback = percent; }
            if (horizontal) {
                const labelLines = labels.reduce((sum, lines) => sum + lines.length, 0);
                frame.style.height = Math.max(240, labelLines * 16 + rows.length * (view === "multiple" ? 25 : 13) + (relationship ? 100 : 75)) + "px";
            } else {
                frame.style.height = view === "pie" ? (narrow ? "420px" : "340px") : (narrow ? "390px" : "330px");
            }
            chartOptions.plugins.tooltip.callbacks = {
                title: items => items.length ? label(rows[items[0].dataIndex]) : "",
                label: item => {
                    const row = rows[item.dataIndex];
                    if (!relationship) return text.count + ": " + number.format(row.count) + " | " + percent(row.percent);
                    const onTime = item.datasetIndex === 0;
                    const count = onTime ? row.onTime : row.late;
                    const rate = onTime ? row.onTimePercent : row.latePercent;
                    return (onTime ? text.onTime : text.late) + ": " + number.format(count) + " / " + number.format(row.known) + " | " + percent(rate);
                },
                afterBody: items => {
                    if (!relationship || !items.length) return [];
                    const row = rows[items[0].dataIndex];
                    return [text.known + ": " + number.format(row.known)];
                }
            };
            let configuration;
            if (relationship) {
                chartOptions.scales.x.stacked = percentage;
                chartOptions.scales.y.stacked = percentage;
                configuration = { type: "bar", data: { labels, datasets: [
                    { label: text.onTime, data: rows.map(row => percentage ? row.onTimePercent : row.onTime), backgroundColor: colors.teal, maxBarThickness: percentage ? 24 : 18 },
                    { label: text.late, data: rows.map(row => percentage ? row.latePercent : row.late), backgroundColor: colors.coral, maxBarThickness: percentage ? 24 : 18 }
                ] }, options: chartOptions };
            } else if (view === "pie") {
                delete chartOptions.scales;
                delete chartOptions.indexAxis;
                chartOptions.plugins.legend = { display: true, position: "bottom", labels: { color: "#aab1ad", font: { size: 13 }, padding: 14, boxWidth: 12, boxHeight: 12, generateLabels: instance => instance.data.labels.map((lines, index) => ({ text: Array.isArray(lines) ? lines.join(" ") : lines, fontColor: "#aab1ad", fillStyle: instance.data.datasets[0].backgroundColor[index], strokeStyle: "transparent", hidden: !instance.getDataVisibility(index), index })) } };
                configuration = { type: "pie", data: { labels, datasets: [{ label: text.count, data: rows.map(row => row.count), backgroundColor: rows.map((row, index) => categoryColor(payload.field, row.code, index, true)), borderColor: "#080a09", borderWidth: 2 }] }, options: chartOptions };
            } else if (view === "line") {
                // Special answers remain visible as isolated points, outside the ordered series.
                if (horizontal) chartOptions.scales.y.offset = true;
                const special = rows.some(row => specialCodes.has(row.code));
                chartOptions.plugins.legend.display = special;
                configuration = { type: "line", data: { labels, datasets: [
                    { label: text.count, data: rows.map(row => specialCodes.has(row.code) ? null : row.count), borderColor: colors.teal, backgroundColor: colors.teal, borderWidth: 2, pointRadius: 4, pointHoverRadius: 6, tension: 0, spanGaps: false },
                    ...(special ? [{ label: text.special || text.category, data: rows.map(row => specialCodes.has(row.code) ? row.count : null), borderColor: colors.gray, backgroundColor: colors.gray, showLine: false, pointStyle: "rect", pointRadius: 5, pointHoverRadius: 7 }] : [])
                ] }, options: chartOptions };
            } else {
                configuration = { type: "bar", data: { labels, datasets: [{ label: text.count, data: rows.map(row => row.count), backgroundColor: rows.map((row, index) => categoryColor(payload.field, row.code, index)), maxBarThickness: 24 }] }, options: chartOptions };
            }
            chart = new Chart(canvas, configuration);
        };
        buttons.forEach((button, index) => {
            on(button, "click", () => show(button.dataset.reportView));
            on(button, "keydown", event => {
                let target;
                if (event.key === "ArrowRight" || event.key === "ArrowDown") target = buttons[(index + 1) % buttons.length];
                else if (event.key === "ArrowLeft" || event.key === "ArrowUp") target = buttons[(index + buttons.length - 1) % buttons.length];
                else if (event.key === "Home") target = buttons[0];
                else if (event.key === "End") target = buttons[buttons.length - 1];
                if (target) { event.preventDefault(); show(target.dataset.reportView); target.focus(); }
            });
        });
        const resize = () => {
            const width = workspace.clientWidth;
            if (Math.abs(width - lastWidth) < 1) return;
            lastWidth = width;
            cancelAnimationFrame(resizeFrame);
            resizeFrame = requestAnimationFrame(() => show(currentView));
        };
        if (window.ResizeObserver) { observer = new ResizeObserver(resize); observer.observe(workspace); }
        else on(window, "resize", resize);
        const api = { show, destroy: () => { observer?.disconnect(); cancelAnimationFrame(resizeFrame); chart?.destroy(); listeners.forEach(remove => remove()); empty.remove(); mounted.delete(workspace); } };
        mounted.set(workspace, api);
        show(currentView);
        return api;
    };
    window.DeadlineReport = { mount };
    const initialize = () => document.querySelectorAll("script[data-report-payload]").forEach(script => {
        const workspace = document.getElementById(script.dataset.reportTarget);
        if (workspace) mount(workspace, JSON.parse(script.textContent));
    });
    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initialize, { once: true });
    else initialize();
})();
