package ru.deadline.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.deadline.lab.model.SurveyResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ResearchProductTest {
    private final StatisticsService statistics = new StatisticsService();

    @Test
    void explorerKeepsOrderedCategoriesAndDistributionDenominator() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "SAME_DAY", "LATE", "MENTAL", "NO"),
                row("c", "UNKNOWN", "NOT_SUBMITTED", "UNKNOWN", "NO"));
        var explorer = statistics.explorer(rows, "startBand", "distribution", "lollipop");
        assertThat(explorer.field()).isEqualTo("startBand");
        assertThat(explorer.views()).containsExactly("bars", "lollipop", "table");
        assertThat(explorer.view()).isEqualTo("lollipop");
        assertThat(explorer.frequency().total()).isEqualTo(3);
        assertThat(explorer.frequency().unknown()).isEqualTo(1);
        assertThat(explorer.frequency().rows().stream().mapToDouble(r -> r.percent()).sum())
                .isCloseTo(100, within(1e-10));
        assertThat(explorer.outcomes().stream().map(r -> r.code()).toList())
                .containsExactlyElementsOf(SurveyCatalog.labels("startBand").keySet());
    }

    @Test
    void relationshipUsesOnlyKnownOutcomesWithinEachPredictorCategory() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "WRITTEN", "NO"),
                row("b", "ONE", "LATE", "WRITTEN", "NO"),
                row("c", "ONE", "NOT_SUBMITTED", "WRITTEN", "NO"),
                row("d", "ONE", "UNKNOWN", "WRITTEN", "NO"),
                row("e", "ONE", "UNKNOWN", "NONE", "NO"),
                row("f", "ONE", "ON_TIME", "UNKNOWN", "NO"));
        var explorer = statistics.explorer(rows, "planning", "relationship", "donut");
        var written = explorer.outcomes().stream().filter(r -> r.code().equals("WRITTEN")).findFirst().orElseThrow();
        assertThat(written.total()).isEqualTo(4);
        assertThat(written.known()).isEqualTo(2);
        assertThat(written.pending()).isEqualTo(1);
        assertThat(written.unknown()).isEqualTo(1);
        assertThat(written.onTimePercent()).isEqualTo(50.0);
        assertThat(written.latePercent()).isEqualTo(50.0);
        assertThat(explorer.outcomes().stream().filter(r -> r.code().equals("NONE")).findFirst().orElseThrow().onTimePercent())
                .isNull();
        assertThat(explorer.outcomes().stream().filter(r -> r.code().equals("NONE")).findFirst().orElseThrow().latePercent())
                .isNull();
        assertThat(explorer.highestCodes()).containsExactly("WRITTEN");
        assertThat(explorer.view()).isEqualTo("bars");
        assertThat(explorer.views()).doesNotContain("donut");
    }

    @Test
    void completeReportUsesSevenCourseSupportedDistributionMappings() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "WRITTEN", "NO"),
                row("b", "ONE", "LATE", "WRITTEN", "NO"),
                row("c", "ONE", "NOT_SUBMITTED", "WRITTEN", "NO"),
                row("d", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN"));
        Map<String, List<String>> expectedViews = Map.of(
                "allottedBand", List.of("bars", "line", "table"),
                "startBand", List.of("bars", "line", "table"),
                "submissionStatus", List.of("pie", "bars", "table"),
                "extensionStatus", List.of("pie", "bars", "table"),
                "planning", List.of("bars", "pie", "table"),
                "difficulty", List.of("line", "bars", "table"),
                "otherDeadlines", List.of("bars", "line", "table"));
        var sections = statistics.reportSections(rows);
        assertThat(sections.stream().map(section -> section.field()).toList())
                .containsExactlyElementsOf(StatisticsService.DATA_VARIABLES);
        assertThat(sections).allSatisfy(section -> {
            assertThat(section.analysis()).isEqualTo("distribution");
            assertThat(section.views()).containsExactlyElementsOf(expectedViews.get(section.field()));
            assertThat(section.recommendedView()).isEqualTo(expectedViews.get(section.field()).getFirst());
            assertThat(section.view()).isEqualTo(section.recommendedView());
            assertThat(section.frequency().total()).isEqualTo(4);
            assertThat(section.frequency().rows().stream().mapToInt(frequency -> frequency.count()).sum()).isEqualTo(4);
            assertThat(section.frequency().rows().stream().mapToDouble(frequency -> frequency.percent()).sum())
                    .isCloseTo(100, within(1e-10));
        });
        var start = sections.get(1);
        var oneDay = start.outcomes().stream().filter(outcome -> outcome.code().equals("ONE")).findFirst().orElseThrow();
        assertThat(oneDay.total()).isEqualTo(3);
        assertThat(oneDay.known()).isEqualTo(2);
        assertThat(oneDay.onTimePercent()).isEqualTo(50.0);
        assertThat(oneDay.latePercent()).isEqualTo(50.0);
        assertThat(oneDay.pending()).isEqualTo(1);
        assertThat(start.outcomes().stream().filter(outcome -> outcome.code().equals("UNKNOWN")).findFirst().orElseThrow().latePercent())
                .isNull();
    }

    @Test
    void derivedOutcomePercentageIsIncludedInChartJsonAndUnavailableRemainsNull() throws Exception {
        var sections = statistics.reportSections(List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "ONE", "LATE", "MENTAL", "NO"), row("c", "SAME_DAY", "NOT_SUBMITTED", "NONE", "NO")));
        var start = sections.get(1);
        ObjectMapper mapper = new ObjectMapper();
        var oneDay = start.outcomes().stream().filter(outcome -> outcome.code().equals("ONE")).findFirst().orElseThrow();
        var pending = start.outcomes().stream().filter(outcome -> outcome.code().equals("SAME_DAY")).findFirst().orElseThrow();
        assertThat(mapper.readTree(mapper.writeValueAsString(oneDay)).get("latePercent").doubleValue()).isEqualTo(50);
        assertThat(mapper.readTree(mapper.writeValueAsString(pending)).get("latePercent").isNull()).isTrue();
    }

    @Test
    void distributionsAllowOnlyStatisticallyAppropriateVisualizations() {
        assertThat(statistics.explorer(List.of(), "submissionStatus", "distribution", "line").view()).isEqualTo("donut");
        assertThat(statistics.explorer(List.of(), "extensionStatus", "distribution", "").recommendedView()).isEqualTo("donut");
        assertThat(statistics.explorer(List.of(), "difficulty", "distribution", "line").view()).isEqualTo("line");
        assertThat(statistics.explorer(List.of(), "otherDeadlines", "distribution", "line").view()).isEqualTo("columns");
        assertThat(statistics.explorer(List.of(), "notAField", "nonsense", "donut").field()).isEqualTo("startBand");
        assertThat(statistics.explorer(List.of(), "submissionStatus", "relationship", "").analysis()).isEqualTo("distribution");
    }

    @Test
    void secondaryFindingsRemainDynamicAndDoNotPreferUnknownPredictors() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "ONE", "LATE", "MENTAL", "NO"),
                row("c", "ONE", "ON_TIME", "UNKNOWN", "NO"));
        var findings = statistics.secondaryFindings(rows);
        var planning = findings.stream().filter(f -> f.field().equals("planning")).findFirst().orElseThrow();
        assertThat(planning.codes()).containsExactly("MENTAL");
        assertThat(planning.onTimePercent()).isEqualTo(50.0);
        assertThat(planning.known()).isEqualTo(2);
        assertThat(statistics.secondaryFindings(List.of())).isEmpty();
    }

    @Test
    void scenariosShareComparablePopulationButUseSeparateKnownDenominators() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "ONE", "LATE", "MENTAL", "NO"),
                row("c", "SAME_DAY", "LATE", "MENTAL", "NO"),
                row("d", "SAME_DAY", "NOT_SUBMITTED", "MENTAL", "NO"),
                row("e", "ONE", "ON_TIME", "MENTAL", "YES"));
        var scenarios = statistics.labScenarios(rows, "THREE_FOUR");
        assertThat(scenarios.stream().map(s -> s.code()).toList()).containsExactly("overall", "earlier", "final");
        assertThat(scenarios.get(0).probability()).isEqualTo(0.5);
        assertThat(scenarios.get(1).summary().known()).isEqualTo(2);
        assertThat(scenarios.get(1).probability()).isEqualTo(0.5);
        assertThat(scenarios.get(2).summary().known()).isEqualTo(1);
        assertThat(scenarios.get(2).probability()).isEqualTo(0.0);
        assertThat(scenarios).allSatisfy(s -> assertThat(s.insufficient()).isTrue());
    }

    @Test
    void missingKnownOutcomesAreUnavailableRatherThanEstimatedAsZero() {
        var rows = List.of(row("a", "ONE", "NOT_SUBMITTED", "MENTAL", "NO"),
                row("b", "SAME_DAY", "UNKNOWN", "MENTAL", "NO"));
        assertThat(statistics.labScenarios(rows, "THREE_FOUR")).allSatisfy(s -> assertThat(s.probability()).isNull());
        assertThat(statistics.compareSimulation(rows, "THREE_FOUR", 20, 100, 12, 42)).isNull();
    }

    @Test
    void pairedSimulationIsReproducibleAndComputesTheoreticalOverlap() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "ONE", "LATE", "MENTAL", "NO"),
                row("c", "SAME_DAY", "ON_TIME", "MENTAL", "NO"),
                row("d", "SAME_DAY", "LATE", "MENTAL", "NO"));
        var result = statistics.compareSimulation(rows, "THREE_FOUR", 20, 1000, 12, 42);
        assertThat(result).isEqualTo(statistics.compareSimulation(rows, "THREE_FOUR", 20, 1000, 12, 42));
        assertThat(result.overlap()).isCloseTo(1, within(1e-12));
        assertThat(result.expectedDifference()).isZero();
        assertThat(result.earlier().histogram().stream().mapToInt(b -> b.observed()).sum()).isEqualTo(1000);
        assertThat(result.finalDay().histogram().stream().mapToInt(b -> b.observed()).sum()).isEqualTo(1000);
    }

    @Test
    void oppositeBoundaryScenariosHaveZeroOverlapAndCorrectExpectations() {
        var rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL", "NO"),
                row("b", "SAME_DAY", "LATE", "MENTAL", "NO"));
        var result = statistics.compareSimulation(rows, "THREE_FOUR", 100, 100, 100, 42);
        assertThat(result.overlap()).isZero();
        assertThat(result.expectedDifference()).isEqualTo(100);
        assertThat(result.earlier().probability()).isEqualTo(1);
        assertThat(result.finalDay().probability()).isZero();
        assertThat(result.earlier().empiricalTail()).isEqualTo(1);
        assertThat(result.finalDay().empiricalTail()).isZero();
    }

    private static SurveyResponse row(String id, String start, String status, String planning, String extension) {
        return new SurveyResponse(id, Instant.parse("2026-09-01T08:00:00Z"), true, "CODE", "THREE_FOUR",
                start, status, extension, planning, 3, "TWO");
    }
}
