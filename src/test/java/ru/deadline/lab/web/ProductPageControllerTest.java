package ru.deadline.lab.web;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import ru.deadline.lab.model.ComparisonSimulation;
import ru.deadline.lab.model.DataExplorer;
import ru.deadline.lab.model.LabScenario;
import ru.deadline.lab.model.ScenarioGroups;
import ru.deadline.lab.model.SimulationResult;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.service.StatisticsService;
import ru.deadline.lab.service.SurveyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductPageControllerTest {
    private final SurveyService survey = mock(SurveyService.class);
    private final StatisticsService statistics = new StatisticsService();
    private final PageController controller = new PageController(survey, statistics, mock(SiteText.class));
    private final List<SurveyResponse> rows = List.of(row("a", "ONE", "ON_TIME", "MENTAL"),
            row("b", "ONE", "LATE", "MENTAL"), row("c", "SAME_DAY", "LATE", "NONE"),
            row("d", "SAME_DAY", "NOT_SUBMITTED", "NONE"), row("e", "UNKNOWN", "UNKNOWN", "UNKNOWN"));

    @BeforeEach
    void setup() {
        when(survey.responses("real")).thenReturn(rows);
        when(survey.realCount()).thenReturn(5L);
        when(survey.allCount()).thenReturn(7L);
        when(survey.collectionStart("real")).thenReturn(Instant.parse("2026-08-31T20:05:00Z"));
        when(survey.collectionEnd("real")).thenReturn(Instant.parse("2026-09-02T19:30:00Z"));
    }

    @Test
    void completeDataReportIgnoresLegacyFiltersAndKeepsSourceCounts() {
        var model = new ExtendedModelMap();
        assertThat(controller.frequencies("real", "planning", "relationship", "lollipop", "ONE", "ONE", "MENTAL", true, model))
                .isEqualTo("frequencies");
        var explorer = (DataExplorer) model.get("explorer");
        assertThat(explorer.field()).isEqualTo("planning");
        assertThat(explorer.analysis()).isEqualTo("relationship");
        assertThat(explorer.view()).isEqualTo("lollipop");
        assertThat(explorer.frequency().total()).isEqualTo(5);
        assertThat(model.get("totalDataset")).isEqualTo(5);
        assertThat(model.get("allCount")).isEqualTo(7L);
        assertThat(((Summary) model.get("summary")).known()).isEqualTo(3);
        assertThat((List<?>) model.get("tables")).hasSize(7);
        assertThat((List<?>) model.get("sections")).hasSize(7);
        assertThat(model.get("filter")).isEqualTo(new PageController.Filter("", "", "", false));
    }

    @Test
    void outcomeCannotBeItsOwnRelationshipPredictor() {
        var model = new ExtendedModelMap();
        controller.frequencies("real", "submissionStatus", "relationship", "donut", "", "", "", false, model);
        assertThat(model.get("relationshipUnavailable")).isEqualTo(true);
        assertThat(((DataExplorer) model.get("explorer")).analysis()).isEqualTo("distribution");
    }

    @Test
    void methodShowsActualAllEligibleAndKnownDenominators() {
        var model = new ExtendedModelMap();
        controller.method("real", "yes", "ONE", "ONE", "MENTAL", true, model);
        assertThat(model.get("allCount")).isEqualTo(7L);
        assertThat(model.get("totalDataset")).isEqualTo(5);
        assertThat(((Summary) model.get("summary")).known()).isEqualTo(3);
        assertThat(model.get("answer")).isEqualTo("");
        assertThat(model.get("filter")).isEqualTo(new PageController.Filter("", "", "", false));
    }

    @Test
    void overviewUsesCompleteRealDatasetAndAllSourceCollectionDates() {
        var model = new ExtendedModelMap();
        assertThat(controller.dashboard("", "ONE", "ONE", "MENTAL", true, "THREE_FOUR", model, null))
                .isEqualTo("dashboard");
        assertThat(model.get("dataset")).isEqualTo("real");
        assertThat(model.get("allCount")).isEqualTo(7L);
        assertThat(model.get("totalDataset")).isEqualTo(5);
        assertThat(((Summary) model.get("summary")).total()).isEqualTo(5);
        assertThat(((Summary) model.get("summary")).known()).isEqualTo(3);
        assertThat(model.get("collectionStart")).isEqualTo("01.09.2026");
        assertThat(model.get("collectionEnd")).isEqualTo("03.09.2026");
        assertThat((List<?>) model.get("sections")).hasSize(7);
        for (String attribute : List.of("startSection", "planningSection", "difficultySection", "workloadSection")) {
            assertThat(((DataExplorer) model.get(attribute)).frequency().total()).isEqualTo(5);
        }
        assertThat(((DataExplorer) model.get("startSection")).field()).isEqualTo("startBand");
        assertThat(((DataExplorer) model.get("planningSection")).field()).isEqualTo("planning");
        assertThat(((DataExplorer) model.get("difficultySection")).field()).isEqualTo("difficulty");
        assertThat(((DataExplorer) model.get("workloadSection")).field()).isEqualTo("otherDeadlines");
        verify(survey).collectionStart("real");
        verify(survey).collectionEnd("real");
        verify(survey, never()).responses("demo");
    }

    @Test
    void emptyRealDatasetHasNoInventedCollectionDatesOrDemoFallback() {
        when(survey.responses("real")).thenReturn(List.of());
        when(survey.collectionStart("real")).thenReturn(null);
        when(survey.collectionEnd("real")).thenReturn(null);
        var model = new ExtendedModelMap();
        controller.dashboard("", "", "", "", false, "THREE_FOUR", model, null);
        assertThat(((Summary) model.get("summary")).total()).isZero();
        assertThat(model.get("collectionStart")).isNull();
        assertThat(model.get("collectionEnd")).isNull();
        assertThat((List<?>) model.get("sections")).hasSize(7);
        verify(survey, never()).responses("demo");
    }

    @Test
    void demoReportRemainsExplicitAndUsesItsOwnTotal() {
        when(survey.responses("demo")).thenReturn(rows.subList(0, 2));
        var model = new ExtendedModelMap();
        controller.frequencies("demo", "startBand", "distribution", "", "", "", "", false, model);
        assertThat(model.get("dataset")).isEqualTo("demo");
        assertThat(model.get("allCount")).isEqualTo(2L);
        assertThat(model.get("totalDataset")).isEqualTo(2);
        assertThat(((Summary) model.get("summary")).known()).isEqualTo(2);
    }

    @Test
    void labObservedScenariosAndCustomModeUseAuthoritativeProbabilities() {
        for (String source : List.of("overall", "earlier", "final", "custom")) {
            var model = lab(source, "single", "repeat", "observed", "37.5");
            assertThat(model.get("source")).isEqualTo(source);
            assertThat(model.get("stage")).isEqualTo("repeat");
            double expected = switch (source) {
                case "overall" -> 1.0 / 3;
                case "earlier" -> 0.5;
                case "final" -> 0.0;
                default -> 0.375;
            };
            assertThat(((SimulationResult) model.get("result")).probability()).isEqualTo(expected);
            if (!source.equals("custom")) {
                assertThat(((LabScenario) model.get("selectedScenario")).code()).isEqualTo(source);
            }
        }
        assertThat(lab("", "single", "group", "manual", "37.5").get("source")).isEqualTo("custom");
    }

    @Test
    void comparisonUsesMatchingGroupSizeAndKnownOutcomesAndRetainsAdvancedData() {
        var model = lab("overall", "compare", "repeat", "observed", "62");
        var comparison = (ComparisonSimulation) model.get("labComparison");
        assertThat(model.get("compareAvailable")).isEqualTo(true);
        assertThat(model.get("comparisonMissing")).isEqualTo(false);
        assertThat(comparison.earlier().size()).isEqualTo(20);
        assertThat(comparison.finalDay().size()).isEqualTo(20);
        assertThat(comparison.earlierScenario().summary().known()).isEqualTo(2);
        assertThat(comparison.finalScenario().summary().known()).isEqualTo(1);
        assertThat(model).containsKeys("earlierGrouped", "finalGrouped", "grouped");
    }

    @Test
    void comparisonIgnoresAnUnusedCustomProbability() {
        for (String probability : List.of("NaN", "101", "oops")) {
            var model = lab("custom", "compare", "repeat", "manual", probability);
            assertThat(model).doesNotContainKey("errorKey");
            assertThat(model.get("labComparison")).isInstanceOf(ComparisonSimulation.class);
            var comparison = (ComparisonSimulation) model.get("labComparison");
            assertThat(comparison.earlier().probability()).isEqualTo(0.5);
            assertThat(comparison.finalDay().probability()).isZero();
        }
    }

    @Test
    void absentOutcomesNeverProduceASpuriousObservedZeroProbability() {
        when(survey.responses("real")).thenReturn(List.of(row("p", "ONE", "NOT_SUBMITTED", "MENTAL")));
        var model = lab("earlier", "compare", "repeat", "observed", "62");
        assertThat(model.get("emptySimulation")).isEqualTo(true);
        assertThat(model.get("compareAvailable")).isEqualTo(false);
        assertThat(model.get("comparisonMissing")).isEqualTo(true);
        assertThat(model).doesNotContainKeys("result", "labComparison");
        assertThat(((LabScenario) model.get("selectedScenario")).probability()).isNull();
    }

    @Test
    void pairedGroupEndpointRespectsDatasetAndFiltersAndRejectsUnavailableGroups() {
        var response = controller.simulatorGroups("real", "", "", "", false, "THREE_FOUR", "20", "42");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var groups = (ScenarioGroups) response.getBody();
        assertThat(groups.earlier().probability()).isEqualTo(0.5);
        assertThat(groups.finalDay().probability()).isZero();
        assertThat(groups.earlier().outcomes()).hasSize(20);
        assertThat(groups.finalDay().outcomes()).hasSize(20);
        assertThat(response.getBody()).isEqualTo(controller.simulatorGroups("real", "", "", "", false, "THREE_FOUR", "20", "42").getBody());
        assertThat(controller.simulatorGroups("real", "", "", "MENTAL", false, "THREE_FOUR", "20", "42")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(controller.simulatorGroups("real", "", "", "", false, "THREE_FOUR", "101", "42")
                .getStatusCode().value()).isEqualTo(400);
    }

    private ExtendedModelMap lab(String source, String labMode, String stage, String mode, String probability) {
        var model = new ExtendedModelMap();
        controller.simulator("real", "", "", "", false, mode, "20", "100", "12", "42", probability,
                source, labMode, stage, "THREE_FOUR", model);
        return model;
    }

    private static SurveyResponse row(String id, String start, String status, String planning) {
        return new SurveyResponse(id, Instant.parse("2026-09-01T08:00:00Z"), true, "CODE", "THREE_FOUR",
                start, status, "NO", planning, 3, "TWO");
    }
}
