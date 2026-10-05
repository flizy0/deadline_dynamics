package ru.deadline.lab.service;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import ru.deadline.lab.model.HistogramBin;
import ru.deadline.lab.model.ConvergencePoint;
import ru.deadline.lab.model.GroupExperiment;
import ru.deadline.lab.model.SimulationResult;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.model.DataExplorer;
import ru.deadline.lab.model.OutcomeRow;
import ru.deadline.lab.model.SecondaryFinding;
import ru.deadline.lab.model.LabScenario;
import ru.deadline.lab.model.ComparisonSimulation;
import ru.deadline.lab.model.ScenarioGroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class StatisticsService {
    public static final List<String> DATA_VARIABLES = List.of("allottedBand", "startBand", "submissionStatus",
            "extensionStatus", "planning", "difficulty", "otherDeadlines");

    public DataExplorer explorer(List<SurveyResponse> rows, String requestedField, String requestedAnalysis,
            String requestedView) {
        String field = DATA_VARIABLES.contains(requestedField) ? requestedField : "startBand";
        String analysis = "relationship".equals(requestedAnalysis) && !"submissionStatus".equals(field)
                ? "relationship" : "distribution";
        List<String> views = "relationship".equals(analysis) ? List.of("bars", "lollipop", "table")
                : switch (field) {
                    case "submissionStatus", "extensionStatus" -> List.of("donut", "bars", "table");
                    case "planning" -> List.of("bars", "donut", "table");
                    case "difficulty" -> List.of("columns", "line", "lollipop", "table");
                    case "otherDeadlines" -> List.of("columns", "lollipop", "table");
                    default -> List.of("bars", "lollipop", "table");
                };
        String recommended = views.getFirst();
        String view = views.contains(requestedView) ? requestedView : recommended;
        List<OutcomeRow> outcomes = SurveyCatalog.labels(field).keySet().stream().map(code -> {
            Summary summary = summarize(rows.stream().filter(row -> code.equals(SurveyCatalog.value(row, field))).toList());
            return new OutcomeRow(code, summary.total(), summary.known(), summary.onTime(), summary.late(),
                    summary.pending(), summary.unknown(), summary.known() == 0 ? null : summary.onTimePercent());
        }).toList();
        List<OutcomeRow> candidates = outcomes.stream().filter(row -> row.known() > 0 && !"UNKNOWN".equals(row.code())).toList();
        Double highest = candidates.stream().mapToDouble(OutcomeRow::onTimePercent).max().stream().boxed().findFirst().orElse(null);
        List<String> highestCodes = highest == null ? List.of() : candidates.stream()
                .filter(row -> Double.compare(row.onTimePercent(), highest) == 0).map(OutcomeRow::code).toList();
        return new DataExplorer(field, analysis, view, recommended, views, frequency(rows, field), outcomes,
                highestCodes, highest);
    }

    public List<DataExplorer> reportSections(List<SurveyResponse> rows) {
        return DATA_VARIABLES.stream().map(field -> {
            DataExplorer explorer = explorer(rows, field, "distribution", "");
            List<String> views = switch (field) {
                case "submissionStatus", "extensionStatus" -> List.of("pie", "bars", "table");
                case "planning" -> List.of("bars", "pie", "table");
                case "difficulty" -> List.of("line", "bars", "table");
                default -> List.of("bars", "line", "table");
            };
            String recommended = views.getFirst();
            return new DataExplorer(field, "distribution", recommended, recommended, views, explorer.frequency(),
                    explorer.outcomes(), explorer.highestCodes(), explorer.highestPercent());
        }).toList();
    }

    public List<SecondaryFinding> secondaryFindings(List<SurveyResponse> rows) {
        List<SecondaryFinding> findings = new ArrayList<>();
        for (String field : List.of("planning", "otherDeadlines", "difficulty")) {
            DataExplorer explorer = explorer(rows, field, "relationship", "bars");
            if (explorer.highestPercent() != null) {
                int known = explorer.outcomes().stream().filter(row -> explorer.highestCodes().contains(row.code()))
                        .mapToInt(OutcomeRow::known).sum();
                findings.add(new SecondaryFinding(field, explorer.highestCodes(), explorer.highestPercent(), known));
            }
        }
        return List.copyOf(findings);
    }

    public List<LabScenario> labScenarios(List<SurveyResponse> rows, String allotted) {
        Comparison comparison = compare(rows, allotted);
        return List.of(scenario("overall", summarize(rows)), scenario("earlier", comparison.earlier()),
                scenario("final", comparison.sameDay()));
    }

    private LabScenario scenario(String code, Summary summary) {
        return new LabScenario(code, summary, summary.known() == 0 ? null : (double) summary.onTime() / summary.known(),
                summary.known() < 20);
    }

    public ComparisonSimulation compareSimulation(List<SurveyResponse> rows, String allotted, int size, int runs,
            int threshold, long seed) {
        List<LabScenario> scenarios = labScenarios(rows, allotted);
        LabScenario earlierScenario = scenarios.get(1);
        LabScenario finalScenario = scenarios.get(2);
        if (earlierScenario.probability() == null || finalScenario.probability() == null) return null;
        SimulationResult earlier = simulate(size, earlierScenario.probability(), runs, threshold, seed);
        SimulationResult finalDay = simulate(size, finalScenario.probability(), runs, threshold, seed ^ 0x9E3779B97F4A7C15L);
        double overlap = 0;
        for (int value = 0; value <= size; value++) {
            overlap += Math.min(earlier.histogram().get(value).theoreticalProbability(),
                    finalDay.histogram().get(value).theoreticalProbability());
        }
        return new ComparisonSimulation(earlierScenario, finalScenario, earlier, finalDay,
                Math.max(0, Math.min(1, overlap)), earlier.expected() - finalDay.expected());
    }

    public ScenarioGroups simulateGroups(List<SurveyResponse> rows, String allotted, int size, long seed) {
        List<LabScenario> scenarios = labScenarios(rows, allotted);
        if (scenarios.get(1).probability() == null || scenarios.get(2).probability() == null) {
            throw new IllegalArgumentException("experiment.error.comparisonUnavailable");
        }
        return new ScenarioGroups(simulateGroup(size, scenarios.get(1).probability(), seed),
                simulateGroup(size, scenarios.get(2).probability(), seed ^ 0x9E3779B97F4A7C15L));
    }

    public FrequencyTable frequency(List<SurveyResponse> rows, String field) {
        var labels = SurveyCatalog.labels(field);
        List<FrequencyRow> frequencies = new ArrayList<>();
        for (var entry : labels.entrySet()) {
            int count = (int) rows.stream().filter(r -> entry.getKey().equals(SurveyCatalog.value(r, field))).count();
            frequencies.add(new FrequencyRow(entry.getKey(), entry.getValue(), count,
                    rows.isEmpty() ? 0 : 100.0 * count / rows.size()));
        }
        int maximum = frequencies.stream().filter(r -> !r.code().equals("UNKNOWN"))
                .mapToInt(FrequencyRow::count).max().orElse(0);
        List<String> modes = maximum == 0 ? List.of() : frequencies.stream()
                .filter(r -> !r.code().equals("UNKNOWN") && r.count() == maximum).map(FrequencyRow::label).toList();
        List<String> modeCodes = maximum == 0 ? List.of() : frequencies.stream()
                .filter(r -> !r.code().equals("UNKNOWN") && r.count() == maximum).map(FrequencyRow::code).toList();
        int unknown = frequencies.stream().filter(r -> r.code().equals("UNKNOWN")).mapToInt(FrequencyRow::count).sum();
        String interpretation = modes.isEmpty() ? "Недостаточно известных ответов для вывода."
                : "Наиболее частый ответ" + (modes.size() > 1 ? " (несколько мод)" : "") + ": "
                + String.join("; ", modes) + ". Частота каждого: " + maximum + " из " + rows.size() + ".";
        return new FrequencyTable(field, SurveyCatalog.question(field).shortTitle(), rows.size(), unknown,
                List.copyOf(frequencies), modes, interpretation, modeCodes, maximum);
    }

    public Comparison compare(List<SurveyResponse> rows, String allotted) {
        if (!List.of("ONE", "TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS").contains(allotted)) {
            throw new IllegalArgumentException("Для сравнения выберите один срок от 1 дня.");
        }
        List<SurveyResponse> comparable = rows.stream().filter(r -> allotted.equals(r.allottedBand())
                && "NO".equals(r.extensionStatus())).toList();
        var early = List.of("ONE", "TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS");
        Summary earlier = summarize(comparable.stream().filter(r -> early.contains(r.startBand())).toList());
        Summary sameDay = summarize(comparable.stream().filter(r -> "SAME_DAY".equals(r.startBand())).toList());
        Double difference = earlier.known() == 0 || sameDay.known() == 0 ? null
                : earlier.onTimePercent() - sameDay.onTimePercent();
        String conclusion;
        if (difference == null) {
            conclusion = "Для сравнения нужны известные результаты в обеих группах.";
        } else {
            conclusion = difference == 0 ? "Доли сдачи вовремя в двух группах равны."
                    : String.format(Locale.forLanguageTag("ru"), "Доля сдачи вовремя %s у начавших раньше на %.1f п.п. Это %s исходной гипотезе в этой выборке.",
                    difference > 0 ? "выше" : "ниже", Math.abs(difference), difference > 0 ? "соответствует" : "не соответствует");
            if (earlier.known() < 20 || sameDay.known() < 20) conclusion = "Мало наблюдений: хотя бы в одной группе меньше 20. " + conclusion;
            conclusion += " Это описательная связь, не доказательство причин или статистической значимости.";
        }
        return new Comparison(allotted, earlier, sameDay, difference, conclusion,
                rows.size() - earlier.known() - sameDay.known());
    }

    public List<GroupRow> groups(List<SurveyResponse> rows, String field) {
        return SurveyCatalog.labels(field).entrySet().stream()
                .filter(e -> !List.of("UNKNOWN", "NOT_STARTED").contains(e.getKey()))
                .map(e -> new GroupRow(field, e.getKey(), e.getValue(), summarize(rows.stream()
                        .filter(r -> e.getKey().equals(SurveyCatalog.value(r, field))).toList()))).toList();
    }

    public GroupedSimulation groupSimulation(SimulationResult result) {
        DescriptiveStatistics numbers = new DescriptiveStatistics();
        result.histogram().forEach(bin -> {
            for (int i = 0; i < bin.observed(); i++) numbers.addValue(bin.value());
        });
        int min = (int) numbers.getMin();
        int max = (int) numbers.getMax();
        int classes = Math.max(1, (int) Math.round(1 + 3.322 * Math.log10(numbers.getN())));
        int width = Math.max(1, (int) Math.ceil((max - min + 1.0) / classes));
        List<GroupedBin> bins = new ArrayList<>();
        int cumulative = 0;
        for (int lower = min; lower <= max; lower += width) {
            int low = lower;
            int high = lower + width - 1;
            int count = result.histogram().stream().filter(b -> b.value() >= low && b.value() <= high)
                    .mapToInt(HistogramBin::observed).sum();
            cumulative += count;
            bins.add(new GroupedBin(low, high, (low + high) / 2.0, count,
                    100.0 * count / numbers.getN(), cumulative));
        }
        return new GroupedSimulation((int) numbers.getN(), min, max, max - min, numbers.getMean(),
                numbers.getPercentile(50), numbers.getN() < 2 ? null : numbers.getStandardDeviation(), classes, width, List.copyOf(bins));
    }

    public record FrequencyRow(String code, String label, int count, double percent) { }
    public record FrequencyTable(String field, String title, int total, int unknown,
                                 List<FrequencyRow> rows, List<String> modes, String interpretation,
                                 List<String> modeCodes, int modeCount) { }
    public record Comparison(String band, Summary earlier, Summary sameDay, Double difference,
                             String conclusion, int excluded) {
        public boolean sufficient() {
            return earlier.known() >= 20 && sameDay.known() >= 20;
        }
    }
    public record GroupRow(String field, String code, String label, Summary summary) { }
    public record GroupedBin(int lower, int upper, double midpoint, int count, double percent, int cumulative) { }
    public record GroupedSimulation(int count, int minimum, int maximum, int range, double mean,
                                    double median, Double deviation, int suggestedClasses, int width,
                                    List<GroupedBin> bins) { }

    public Summary summarize(List<SurveyResponse> rows) {
        int onTime = 0;
        int late = 0;
        int pending = 0;
        int unknown = 0;
        for (SurveyResponse row : rows) {
            switch (row.submissionStatus()) {
                case "ON_TIME" -> onTime++;
                case "LATE" -> late++;
                case "NOT_SUBMITTED" -> pending++;
                default -> unknown++;
            }
        }
        // Pending and unknown submissions stay visible but are not completed, known outcomes.
        int known = onTime + late;
        double onTimePercent = known == 0 ? 0 : 100.0 * onTime / known;
        double notOnTimePercent = known == 0 ? 0 : 100.0 * late / known;
        return new Summary(rows.size(), known, onTime, late, pending, unknown, onTimePercent, notOnTimePercent);
    }

    public SimulationResult simulate(int size, double probability, int runs, int threshold, long seed) {
        validateModel(size, probability);
        if (runs < 1 || runs > 50_000) {
            throw new IllegalArgumentException("experiment.error.runs");
        }
        if (threshold < 0 || threshold > size) {
            throw new IllegalArgumentException("experiment.error.threshold");
        }

        BinomialDistribution distribution = new BinomialDistribution(new Well19937c(seed), size, probability);
        int[] counts = new int[size + 1];
        int firstResult = 0;
        int thresholdReached = 0;
        long totalSuccesses = 0;
        int checkpointInterval = Math.max(1, runs / 200);
        List<ConvergencePoint> convergence = new ArrayList<>();
        for (int run = 0; run < runs; run++) {
            int value = distribution.sample();
            if (run == 0) {
                firstResult = value;
            }
            counts[value]++;
            totalSuccesses += value;
            if (value >= threshold) {
                thresholdReached++;
            }
            int completed = run + 1;
            if (completed <= 20 || completed % checkpointInterval == 0 || completed == runs) {
                convergence.add(new ConvergencePoint(completed, (double) thresholdReached / completed));
            }
        }

        List<HistogramBin> histogram = new ArrayList<>();
        double theoreticalTail = 0;
        for (int value = 0; value <= size; value++) {
            double mass = distribution.probability(value);
            histogram.add(new HistogramBin(value, counts[value], runs * mass, (double) counts[value] / runs, mass));
            if (value >= threshold) {
                theoreticalTail += mass;
            }
        }
        // Summing the upper tail avoids subtracting two nearly equal numbers for a rare event.
        theoreticalTail = Math.max(0, Math.min(1, theoreticalTail));
        double empiricalTail = (double) thresholdReached / runs;
        List<Boolean> firstGroup = new ArrayList<>();
        for (int student = 0; student < size; student++) firstGroup.add(student < firstResult);
        // Conditional on its success count, a binomial group is exchangeable across students.
        Collections.shuffle(firstGroup, new Random(seed ^ 0x9E3779B97F4A7C15L));
        return new SimulationResult(size, probability, runs, threshold, seed, size * probability,
                theoreticalTail, empiricalTail, firstResult, histogram, (double) totalSuccesses / runs,
                Math.abs(theoreticalTail - empiricalTail), convergence, firstGroup);
    }

    public GroupExperiment simulateGroup(int size, double probability, long seed) {
        validateModel(size, probability);
        BinomialDistribution trial = new BinomialDistribution(new Well19937c(seed), 1, probability);
        List<Boolean> outcomes = new ArrayList<>();
        int successes = 0;
        for (int student = 0; student < size; student++) {
            boolean success = trial.sample() == 1;
            outcomes.add(success);
            if (success) successes++;
        }
        return new GroupExperiment(size, probability, seed, successes, outcomes);
    }

    private void validateModel(int size, double probability) {
        if (size < 1 || size > 100) throw new IllegalArgumentException("experiment.error.size");
        if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
            throw new IllegalArgumentException("experiment.error.probability");
        }
    }
}
