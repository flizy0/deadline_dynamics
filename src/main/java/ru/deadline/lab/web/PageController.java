package ru.deadline.lab.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.apache.commons.csv.CSVFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.model.LabScenario;
import ru.deadline.lab.model.DataExplorer;
import ru.deadline.lab.service.StatisticsService;
import ru.deadline.lab.service.SurveyService;
import ru.deadline.lab.service.SurveyCatalog;

@Controller
public class PageController {
    private static final Map<String, String> BANDS = orderedMap(
            "SAME_DAY", "В тот же день", "ONE", "За 1 день", "TWO", "За 2 дня",
            "THREE_FOUR", "За 3–4 дня", "FIVE_SEVEN", "За 5–7 дней",
            "EIGHT_PLUS", "За 8+ дней", "UNKNOWN", "Не помню");
    private static final Map<String, String> STARTS = starts();
    private static final Map<String, String> PLANNING = orderedMap(
            "WRITTEN", "Записанный план", "MENTAL", "План в голове", "NONE", "Без плана", "NOT_STARTED", "Ещё не начал", "UNKNOWN", "Не помню");
    private static final List<String> COMPARABLE_BANDS = List.of("TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS");
    private final SurveyService survey;
    private final StatisticsService statistics;
    private final SiteText text;

    public PageController(SurveyService survey, StatisticsService statistics, SiteText text) {
        this.survey = survey;
        this.statistics = statistics;
        this.text = text;
    }

    @GetMapping({"/", "/results"})
    public String dashboard(@RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions,
            @RequestParam(defaultValue = "THREE_FOUR") String compareBand, Model model, HttpServletRequest request) {
        String selected = common(model, "dashboard", dataset);
        List<SurveyResponse> rows = survey.responses(selected);
        reportModel(model, selected, rows);
        List<DataExplorer> sections = statistics.reportSections(rows);
        model.addAttribute("sections", sections);
        model.addAttribute("startSection", reportSection(sections, "startBand"));
        model.addAttribute("planningSection", reportSection(sections, "planning"));
        model.addAttribute("difficultySection", reportSection(sections, "difficulty"));
        model.addAttribute("workloadSection", reportSection(sections, "otherDeadlines"));
        model.addAttribute("collectionStart", collectionDate(survey.collectionStart(selected)));
        model.addAttribute("collectionEnd", collectionDate(survey.collectionEnd(selected)));
        model.addAttribute("startChart", chart(rows, SurveyResponse::startBand, STARTS));
        String comparisonBand = COMPARABLE_BANDS.contains(compareBand) || "ONE".equals(compareBand) ? compareBand : "THREE_FOUR";
        var comparison = statistics.compare(rows, comparisonBand);
        model.addAttribute("comparison", comparison);
        model.addAttribute("compareBand", comparisonBand);
        model.addAttribute("compareBandLabel", BANDS.get(comparisonBand));
        model.addAttribute("sameDay", comparison.sameDay());
        model.addAttribute("earlier", comparison.earlier());
        model.addAttribute("workloadChart", statistics.groups(rows, "otherDeadlines"));
        model.addAttribute("planningChart", statistics.groups(rows, "planning"));
        model.addAttribute("difficultyChart", statistics.groups(rows, "difficulty"));
        model.addAttribute("secondaryFindings", statistics.secondaryFindings(rows));
        model.addAttribute("latestResponse", rows.stream().map(SurveyResponse::submittedAt).filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.naturalOrder()).map(i -> DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                        .withZone(ZoneId.of("UTC")).format(i) + " UTC").orElse(null));
        return "dashboard";
    }

    @GetMapping("/simulator")
    public String simulator(@RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions,
            @RequestParam(defaultValue = "observed") String mode, @RequestParam(defaultValue = "20") String size,
            @RequestParam(defaultValue = "1000") String runs, @RequestParam(defaultValue = "12") String threshold,
            @RequestParam(required = false) String seed, @RequestParam(defaultValue = "62") String p,
            @RequestParam(defaultValue = "") String source, @RequestParam(defaultValue = "single") String labMode,
            @RequestParam(defaultValue = "group") String stage, @RequestParam(defaultValue = "THREE_FOUR") String compareBand,
            Model model) {
        seed = seed == null ? "42" : seed;
        String selected = common(model, "simulator", dataset);
        Filter filter = filter(allotted, start, planning, excludeExtensions);
        List<SurveyResponse> rows = apply(survey.responses(selected), filter);
        Summary summary = statistics.summarize(rows);
        String selectedSource = List.of("overall", "earlier", "final", "custom").contains(source) ? source
                : "manual".equals(mode) ? "custom" : "overall";
        String selectedLabMode = "compare".equals(labMode) ? "compare" : "single";
        String selectedMode = "custom".equals(selectedSource) && "single".equals(selectedLabMode) ? "manual" : "observed";
        String comparisonBand = comparisonBand(compareBand);
        List<LabScenario> scenarios = statistics.labScenarios(rows, comparisonBand);
        LabScenario selectedScenario = "compare".equals(selectedLabMode) ? scenarios.getFirst()
                : scenarios.stream().filter(scenario -> scenario.code().equals(selectedSource)).findFirst().orElse(null);
        boolean compareAvailable = scenarios.get(1).probability() != null && scenarios.get(2).probability() != null;
        model.addAttribute("filter", filter);
        model.addAttribute("summary", summary);
        model.addAttribute("mode", selectedMode);
        model.addAttribute("source", selectedSource);
        model.addAttribute("labMode", selectedLabMode);
        model.addAttribute("stage", "repeat".equals(stage) ? "repeat" : "group");
        model.addAttribute("compareBand", comparisonBand);
        model.addAttribute("labScenarios", scenarios);
        model.addAttribute("selectedScenario", selectedScenario);
        model.addAttribute("compareAvailable", compareAvailable);
        model.addAttribute("comparisonMissing", "compare".equals(selectedLabMode) && !compareAvailable);
        model.addAttribute("size", safeIntegerInput(size, 1, 100, "20"));
        model.addAttribute("runs", safeIntegerInput(runs, 1, 50_000, "1000"));
        model.addAttribute("threshold", safeIntegerInput(threshold, 0, 100, "12"));
        model.addAttribute("seed", safeLongInput(seed, "42"));
        model.addAttribute("p", safeProbabilityInput(p));
        try {
            int n = Integer.parseInt(size);
            int repetitions = Integer.parseInt(runs);
            int atLeast = Integer.parseInt(threshold);
            long randomSeed = seed.isBlank() ? ThreadLocalRandom.current().nextLong() : Long.parseLong(seed);
            Double probability = "manual".equals(selectedMode) ? Double.valueOf(Double.parseDouble(p.replace(',', '.')) / 100.0)
                    : selectedScenario.probability();
            if (probability != null && (!Double.isFinite(probability) || probability < 0 || probability > 1)) {
                throw new IllegalArgumentException("experiment.error.probability");
            }
            if (n < 1 || n > 100) throw new IllegalArgumentException("experiment.error.size");
            if (repetitions < 1 || repetitions > 50000) throw new IllegalArgumentException("experiment.error.runs");
            if (atLeast < 0 || atLeast > n) throw new IllegalArgumentException("experiment.error.threshold");
            if (probability == null) {
                model.addAttribute("emptySimulation", true);
            } else {
                var result = statistics.simulate(n, probability, repetitions, atLeast, randomSeed);
                model.addAttribute("result", result);
                model.addAttribute("grouped", statistics.groupSimulation(result));
                model.addAttribute("tailDifference", 100.0 * result.absoluteTailDifference());
                model.addAttribute("simulationChart", result.histogram());
                model.addAttribute("convergence", result.convergence());
                model.addAttribute("histogramMax", Math.max(1.0, result.histogram().stream()
                        .mapToDouble(b -> Math.max(b.observed(), b.expectedCount())).max().orElse(1)));
                model.addAttribute("tickStep", Math.max(1, n / 10));
            }
            if ("compare".equals(selectedLabMode) && compareAvailable) {
                var comparison = statistics.compareSimulation(rows, comparisonBand, n, repetitions, atLeast, randomSeed);
                model.addAttribute("labComparison", comparison);
                model.addAttribute("earlierGrouped", statistics.groupSimulation(comparison.earlier()));
                model.addAttribute("finalGrouped", statistics.groupSimulation(comparison.finalDay()));
            }
        } catch (NumberFormatException exception) {
            model.addAttribute("errorKey", "experiment.error.numbers");
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorKey", exception.getMessage());
        }
        return "simulator";
    }

    @GetMapping("/simulator/groups")
    @ResponseBody
    public ResponseEntity<?> simulatorGroups(@RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions,
            @RequestParam(defaultValue = "THREE_FOUR") String compareBand,
            @RequestParam(defaultValue = "20") String size, @RequestParam(defaultValue = "") String seed) {
        try {
            List<SurveyResponse> rows = apply(survey.responses(dataset(dataset)), filter(allotted, start, planning, excludeExtensions));
            int n = Integer.parseInt(size);
            long randomSeed = seed.isBlank() ? ThreadLocalRandom.current().nextLong() : Long.parseLong(seed);
            return ResponseEntity.ok(statistics.simulateGroups(rows, comparisonBand(compareBand), n, randomSeed));
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().body(Map.of("errorKey", "experiment.error.numbers"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("errorKey", exception.getMessage()));
        }
    }

    @GetMapping("/simulator/group")
    @ResponseBody
    public ResponseEntity<?> simulatorGroup(@RequestParam(defaultValue = "62") String p,
            @RequestParam(defaultValue = "20") String size, @RequestParam(defaultValue = "") String seed) {
        try {
            double probability = Double.parseDouble(p.replace(',', '.')) / 100.0;
            int n = Integer.parseInt(size);
            long randomSeed = seed.isBlank() ? ThreadLocalRandom.current().nextLong() : Long.parseLong(seed);
            return ResponseEntity.ok(statistics.simulateGroup(n, probability, randomSeed));
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().body(Map.of("errorKey", "experiment.error.numbers"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("errorKey", exception.getMessage()));
        }
    }

    @GetMapping("/method")
    public String method(@RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "") String answer,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions,
            Model model) {
        String selected = common(model, "method", dataset);
        List<SurveyResponse> rows = survey.responses(selected);
        reportModel(model, selected, rows);
        model.addAttribute("answer", "");
        return "method";
    }

    @GetMapping({"/data", "/frequencies"})
    public String frequencies(@RequestParam(defaultValue = "real") String dataset,
            @RequestParam(defaultValue = "startBand") String variable,
            @RequestParam(defaultValue = "distribution") String analysis, @RequestParam(defaultValue = "") String view,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions,
            Model model) {
        String selected = common(model, "frequencies", dataset);
        List<SurveyResponse> rows = survey.responses(selected);
        var explorer = statistics.explorer(rows, variable, analysis, view);
        reportModel(model, selected, rows);
        model.addAttribute("sections", statistics.reportSections(rows));
        model.addAttribute("dataVariables", StatisticsService.DATA_VARIABLES);
        model.addAttribute("explorer", explorer);
        model.addAttribute("relationshipUnavailable", "submissionStatus".equals(explorer.field()) && "relationship".equals(analysis));
        model.addAttribute("tables", SurveyCatalog.questions().stream().filter(q -> !q.field().equals("eligible"))
                .map(q -> statistics.frequency(rows, q.field())).toList());
        return "frequencies";
    }

    @GetMapping({"/source", "/connection"})
    public String connection(@RequestParam(defaultValue = "") String dataset, Model model) {
        common(model, "connection", dataset);
        return "connection";
    }

    @GetMapping("/login")
    public String login(Model model) {
        common(model, "admin", "real");
        return "login";
    }

    @GetMapping("/report.csv")
    public ResponseEntity<byte[]> report(@RequestParam(defaultValue = "") String dataset,
            @RequestParam(defaultValue = "") String allotted, @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String planning, @RequestParam(defaultValue = "false") boolean excludeExtensions) {
        String selected = dataset(dataset);
        Filter filter = filter(allotted, start, planning, excludeExtensions);
        List<SurveyResponse> rows = apply(survey.responses(selected), filter);
        Summary summary = statistics.summarize(rows);
        StringWriter csv = new StringWriter();
        try (var printer = CSVFormat.DEFAULT.builder().setRecordSeparator("\r\n").get().print(csv)) {
            printer.printRecord(text.text("report.metric"), text.text("report.value"));
            printer.printRecord(text.text("report.dataset"), text.text("dataset." + selected));
            printer.printRecord(text.text("short.allottedBand"), filter.allotted().isEmpty()
                    ? text.text("filter.allDeadlines") : text.option("allottedBand", filter.allotted()));
            printer.printRecord(text.text("short.startBand"), filter.start().isEmpty()
                    ? text.text("filter.allOptions") : text.option("startBand", filter.start()));
            printer.printRecord(text.text("short.planning"), filter.planning().isEmpty()
                    ? text.text("filter.allOptions") : text.option("planning", filter.planning()));
            printer.printRecord(text.text("report.noExtensions"), text.option("eligible", filter.excludeExtensions() ? "YES" : "NO"));
            printer.printRecord(text.text("report.total"), summary.total());
            printer.printRecord(text.text("report.known"), summary.known());
            printer.printRecord(text.text("report.onTime"), summary.onTime());
            printer.printRecord(text.text("report.late"), summary.late());
            printer.printRecord(text.text("report.pending"), summary.pending());
            printer.printRecord(text.text("report.unknown"), summary.unknown());
            printer.printRecord(text.text("report.onTimePercent"), String.format(java.util.Locale.ROOT, "%.2f", summary.onTimePercent()));
            printer.println();
            printer.printRecord(text.text("short.startBand"), text.text("report.total"));
            for (var row : chart(rows, SurveyResponse::startBand, STARTS)) {
                printer.printRecord(text.option("startBand", row.code()), row.count());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write the in-memory CSV report.", exception);
        }
        return download("deadline-lab-" + selected + "-summary.csv", "\uFEFF" + csv);
    }

    @GetMapping("/csv-template.csv")
    public ResponseEntity<byte[]> csvTemplate() {
        return download("deadline-lab-EXAMPLE-template.csv", survey.csvTemplate());
    }

    private ResponseEntity<byte[]> download(String filename, String csv) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String common(Model model, String page, String requested) {
        String selected = dataset(requested);
        model.addAttribute("page", page);
        model.addAttribute("dataset", selected);
        model.addAttribute("realCount", survey.realCount());
        model.addAttribute("bands", BANDS);
        model.addAttribute("starts", STARTS);
        model.addAttribute("plans", PLANNING);
        model.addAttribute("googleForm", SurveyCatalog.GOOGLE_FORM);
        return selected;
    }

    private String dataset(String requested) {
        if ("real".equals(requested) || "demo".equals(requested)) return requested;
        return "real";
    }

    private void reportModel(Model model, String dataset, List<SurveyResponse> rows) {
        model.addAttribute("filter", new Filter("", "", "", false));
        model.addAttribute("summary", statistics.summarize(rows));
        model.addAttribute("totalDataset", rows.size());
        model.addAttribute("allCount", "demo".equals(dataset) ? (long) rows.size() : survey.allCount());
    }

    private DataExplorer reportSection(List<DataExplorer> sections, String field) {
        return sections.stream().filter(section -> field.equals(section.field())).findFirst().orElseThrow();
    }

    private String collectionDate(Instant submittedAt) {
        return submittedAt == null ? null : DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.of("Asia/Qyzylorda")).format(submittedAt);
    }

    private String comparisonBand(String requested) {
        return COMPARABLE_BANDS.contains(requested) || "ONE".equals(requested) ? requested : "THREE_FOUR";
    }

    private Filter filter(String allotted, String start, String planning, boolean excludeExtensions) {
        return new Filter(BANDS.containsKey(allotted) ? allotted : "", STARTS.containsKey(start) ? start : "",
                PLANNING.containsKey(planning) ? planning : "", excludeExtensions);
    }

    private List<SurveyResponse> apply(List<SurveyResponse> rows, Filter filter) {
        return rows.stream().filter(r -> filter.allotted().isEmpty() || filter.allotted().equals(r.allottedBand()))
                .filter(r -> filter.start().isEmpty() || filter.start().equals(r.startBand()))
                .filter(r -> filter.planning().isEmpty() || filter.planning().equals(r.planning()))
                .filter(r -> !filter.excludeExtensions() || "NO".equals(r.extensionStatus())).toList();
    }

    private List<ChartRow> chart(List<SurveyResponse> rows, Function<SurveyResponse, String> key, Map<String, String> labels) {
        int maximum = Math.max(1, labels.keySet().stream().mapToInt(code -> (int) rows.stream().filter(r -> code.equals(key.apply(r))).count()).max().orElse(1));
        return labels.entrySet().stream().map(entry -> {
            int count = (int) rows.stream().filter(r -> entry.getKey().equals(key.apply(r))).count();
            return new ChartRow(entry.getKey(), entry.getValue(), count,
                    rows.isEmpty() ? 0 : 100.0 * count / rows.size(), 100.0 * count / maximum);
        }).toList();
    }

    private String safeProbabilityInput(String value) {
        try {
            double parsed = Double.parseDouble(value.replace(',', '.'));
            if (!Double.isFinite(parsed) || parsed < 0 || parsed > 100) return "62";
            return parsed == Math.rint(parsed) ? Long.toString((long) parsed) : Double.toString(parsed);
        } catch (RuntimeException exception) {
            return "62";
        }
    }

    private String safeIntegerInput(String value, int minimum, int maximum, String fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= minimum && parsed <= maximum ? Integer.toString(parsed) : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String safeLongInput(String value, String fallback) {
        if (value == null || value.isBlank()) return "";
        try {
            return Long.toString(Long.parseLong(value));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static Map<String, String> orderedMap(String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i], pairs[i + 1]);
        return java.util.Collections.unmodifiableMap(result);
    }

    private static Map<String, String> starts() {
        Map<String, String> result = new LinkedHashMap<>(BANDS);
        result.put("AFTER", "После дедлайна");
        result.put("NOT_STARTED", "Ещё не начал");
        return java.util.Collections.unmodifiableMap(result);
    }

    public record Filter(String allotted, String start, String planning, boolean excludeExtensions) { }
    public record ChartRow(String code, String label, int count, double percent, double relativeWidth) { }
}
