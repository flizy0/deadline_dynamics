"use strict";

(() => {
    const number = new Intl.NumberFormat(document.documentElement.lang || "ru", { maximumFractionDigits: 1 });
    const percent = value => number.format(value) + "%";
    const palette = { teal: "#49bfae", blue: "#5da6bd", coral: "#c8766f", amber: "#d0a03a", purple: "#8e7eb0", sage: "#7f9d86", gray: "#98a09c" };
    const read = id => {
        const element = document.getElementById(id);
        return element ? JSON.parse(element.textContent) : null;
    };
    const wrap = (label, length = 27) => {
        const lines = [];
        let line = "";
        String(label).split(" ").forEach(word => {
            if (line && line.length + word.length + 1 > length) { lines.push(line); line = word; }
            else line += (line ? " " : "") + word;
        });
        if (line) lines.push(line);
        return lines;
    };
    const axes = (title, horizontal) => ({
        responsive: true, maintainAspectRatio: false, animation: false,
        indexAxis: horizontal ? "y" : "x",
        interaction: { mode: "nearest", intersect: false },
        plugins: { legend: { display: false } },
        scales: {
            x: { beginAtZero: true, title: { display: horizontal, text: title }, grid: { display: horizontal, color: "rgba(255,255,255,.055)" }, ticks: { maxRotation: 0 } },
            y: { beginAtZero: true, title: { display: !horizontal, text: title }, grid: { display: !horizontal, color: "rgba(255,255,255,.055)" }, ticks: horizontal ? { autoSkip: false } : { maxTicksLimit: 6 } }
        }
    });
    const draw = (id, configuration) => {
        const canvas = document.getElementById(id);
        if (!canvas || !window.Chart) return null;
        if (configuration.options.indexAxis === "y") {
            const labels = configuration.data.labels || [];
            const lineCount = labels.reduce((sum, label) => sum + (Array.isArray(label) ? label.length : 1), 0);
            canvas.parentElement.style.minHeight = Math.max(190, lineCount * 15 + labels.length * 14 + 70) + "px";
        }
        return new Chart(canvas, configuration);
    };

    const overview = read("overview-data");
    if (overview) {
        const options = axes(overview.text.percent, true);
        options.scales.x.max = 100;
        options.scales.x.stacked = true;
        options.scales.y.stacked = true;
        options.scales.x.ticks.callback = percent;
        options.plugins.legend = { display: true, position: "bottom" };
        options.plugins.tooltip = { callbacks: { label: item => item.dataset.label + ": " + percent(item.parsed.x) } };
        draw("overview-comparison", { type: "bar", data: { labels: overview.labels.map(label => wrap(label)), datasets: [
            { label: overview.text.onTime, data: overview.onTime, backgroundColor: palette.teal, maxBarThickness: 22 },
            { label: overview.text.late, data: overview.late, backgroundColor: palette.coral, maxBarThickness: 22 }
        ] }, options });
    }

    const form = document.querySelector("[data-explorer-form]");
    if (form) {
        const analysis = form.querySelector("[data-explorer-analysis]");
        const view = form.querySelector("[data-explorer-current-view]");
        form.querySelector("[data-explorer-variable]")?.addEventListener("change", () => { if (view) view.disabled = true; });
        form.addEventListener("submit", event => {
            const name = event.submitter?.name;
            if (name === "analysis") { if (analysis) analysis.disabled = true; if (view) view.disabled = true; }
            if (name === "view" && view) view.disabled = true;
        });
        const buttons = Array.from(document.querySelectorAll("[data-explorer-view-button]"));
        buttons.forEach((button, index) => button.addEventListener("keydown", event => {
            let next;
            if (event.key === "ArrowRight") next = buttons[(index + 1) % buttons.length];
            else if (event.key === "ArrowLeft") next = buttons[(index + buttons.length - 1) % buttons.length];
            else if (event.key === "Home") next = buttons[0];
            else if (event.key === "End") next = buttons[buttons.length - 1];
            if (next) { event.preventDefault(); next.focus(); }
        }));
    }

    const explorer = read("explorer-data");
    if (!explorer || explorer.view === "table" || !window.Chart) return;
    const relationship = explorer.analysis === "relationship";
    const horizontal = explorer.view === "bars" || explorer.view === "lollipop";
    const labelByCode = new Map(explorer.rows.map((row, index) => [row.code, explorer.labels[index]]));
    // Unknown difficulty is visible in exact values, not connected as a sixth numerical score.
    const rows = (relationship ? explorer.outcomes : explorer.rows).filter(row => explorer.view !== "line" || row.code !== "UNKNOWN");
    const categoryColor = code => {
        if (code === "UNKNOWN") return palette.gray;
        if (explorer.field === "submissionStatus") return code === "ON_TIME" ? palette.teal : code === "LATE" ? palette.coral : palette.amber;
        if (explorer.field === "extensionStatus") return code === "NO" ? palette.teal : palette.amber;
        if (explorer.field === "planning") return ({ WRITTEN: palette.teal, MENTAL: palette.blue, NONE: palette.sage, NOT_STARTED: palette.amber })[code] || palette.gray;
        return palette.teal;
    };
    const label = row => {
        const text = labelByCode.get(row.code) || row.code;
        return explorer.field === "difficulty" && row.code !== "UNKNOWN" ? row.code + ". " + text : text;
    };
    const values = rows.map(row => relationship ? row.onTimePercent : row.count);
    const options = axes(relationship ? explorer.text.percent : explorer.text.count, horizontal);
    const valueAxis = horizontal ? options.scales.x : options.scales.y;
    if (relationship) { valueAxis.max = 100; valueAxis.ticks.callback = percent; }
    else valueAxis.ticks.precision = 0;
    options.plugins.tooltip = { callbacks: {
        title: items => items.length ? label(rows[items[0].dataIndex]) : "",
        label: item => {
            const row = rows[item.dataIndex];
            if (!relationship) return explorer.text.count + ": " + number.format(row.count) + " · " + percent(row.percent);
            return explorer.text.onTime + ": " + (row.onTimePercent == null ? explorer.text.noKnown : percent(row.onTimePercent)) + " · " + row.onTime + " / " + row.known;
        },
        afterLabel: item => {
            const row = rows[item.dataIndex];
            if (!relationship) return "";
            const lines = [explorer.text.pending + ": " + row.pending + " · " + explorer.text.unknown + ": " + row.unknown];
            if (row.known < 20) lines.push(row.known === 0 ? explorer.text.noKnown : explorer.text.small);
            return lines;
        }
    } };
    const labels = rows.map(row => horizontal ? wrap(label(row), 26) : wrap(label(row), 19));
    const colors = rows.map(row => categoryColor(row.code));

    if (explorer.view === "donut") {
        delete options.scales;
        delete options.indexAxis;
        options.cutout = "67%";
        options.plugins.legend = { display: true, position: "bottom", labels: { boxWidth: 8, padding: 12, font: { size: 10 } } };
        draw("explorer-chart", { type: "doughnut", data: { labels: rows.map(label), datasets: [{ label: explorer.text.count, data: values, backgroundColor: colors, borderColor: "#080a09", borderWidth: 3, hoverOffset: 0 }] }, options });
        return;
    }
    if (explorer.view === "line") {
        draw("explorer-chart", { type: "line", data: { labels, datasets: [{ label: relationship ? explorer.text.onTime : explorer.text.count, data: values, borderColor: palette.teal, borderWidth: 2, pointRadius: 3, pointHoverRadius: 4, tension: 0, fill: false }] }, options });
        return;
    }
    const lollipop = explorer.view === "lollipop";
    const stems = {
        id: "explorerLollipop",
        afterDatasetsDraw(chart) {
            const scale = chart.scales.x;
            const context = chart.ctx;
            const zero = scale.getPixelForValue(0);
            const meta = chart.getDatasetMeta(0);
            context.save();
            meta.data.forEach((bar, index) => {
                if (values[index] == null) return;
                const point = bar.getProps(["x", "y"], true);
                context.strokeStyle = colors[index];
                context.fillStyle = colors[index];
                context.lineWidth = 1.5;
                context.beginPath(); context.moveTo(zero, point.y); context.lineTo(point.x, point.y); context.stroke();
                context.beginPath(); context.arc(point.x, point.y, 3.5, 0, Math.PI * 2); context.fill();
            });
            context.restore();
        }
    };
    draw("explorer-chart", { type: "bar", data: { labels, datasets: [{ label: relationship ? explorer.text.onTime : explorer.text.count, data: values, backgroundColor: lollipop ? "transparent" : colors, hoverBackgroundColor: lollipop ? "transparent" : colors, borderWidth: 0, maxBarThickness: horizontal ? 15 : 40 }] }, options, plugins: lollipop ? [stems] : [] });
})();
