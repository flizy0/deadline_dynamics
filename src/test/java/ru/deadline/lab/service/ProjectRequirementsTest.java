package ru.deadline.lab.service;

import org.junit.jupiter.api.Test;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.repository.SurveyRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectRequirementsTest {
    private final StatisticsService statistics = new StatisticsService();

    @Test
    void frequencyIncludesUnknownAndHasCorrectPercentages() {
        var table = statistics.frequency(List.of(row("ONE", "ON_TIME", "NO"),
                row("ONE", "LATE", "NO"), row("UNKNOWN", "UNKNOWN", "NO")), "startBand");
        assertThat(table.total()).isEqualTo(3);
        assertThat(table.rows().stream().mapToInt(r -> r.count()).sum()).isEqualTo(3);
        assertThat(table.rows().stream().mapToDouble(r -> r.percent()).sum()).isCloseTo(100, within(1e-9));
        assertThat(table.modes()).contains("За 1 день");
        assertThat(table.modeCodes()).containsExactly("ONE");
        assertThat(table.modeCount()).isEqualTo(2);
        assertThat(statistics.frequency(List.of(), "planning").modes()).isEmpty();
    }

    @Test
    void comparisonExcludesExtensionsAndRequiresSameAllottedBand() {
        var result = statistics.compare(List.of(row("ONE", "ON_TIME", "NO"),
                row("SAME_DAY", "LATE", "NO"), row("SAME_DAY", "ON_TIME", "YES")), "THREE_FOUR");
        assertThat(result.earlier().known()).isEqualTo(1);
        assertThat(result.sameDay().known()).isEqualTo(1);
        assertThat(result.difference()).isEqualTo(100);
        assertThat(result.conclusion()).contains("Мало");
        assertThat(result.sufficient()).isFalse();
        assertThat(statistics.compare(List.of(), "TWO").difference()).isNull();
    }

    @Test
    void comparisonSeparatesPendingOutcomesAndExposesLocalizedGroupCodes() {
        var result = statistics.compare(List.of(row("ONE", "ON_TIME", "NO"),
                row("ONE", "NOT_SUBMITTED", "NO"), row("SAME_DAY", "LATE", "NO")), "THREE_FOUR");
        assertThat(result.earlier().known()).isEqualTo(1);
        assertThat(result.earlier().pending()).isEqualTo(1);
        assertThat(result.earlier().onTimePercent()).isEqualTo(100);
        assertThat(result.excluded()).isEqualTo(1);
        assertThat(statistics.groups(List.of(row("ONE", "ON_TIME", "NO")), "planning"))
                .anySatisfy(group -> {
                    assertThat(group.field()).isEqualTo("planning");
                    assertThat(group.code()).isEqualTo("MENTAL");
                    assertThat(group.summary().known()).isEqualTo(1);
                });
    }

    @Test
    void groupedSimulationCoversAllRunsAndConstantDataset() {
        var grouped = statistics.groupSimulation(statistics.simulate(20, 0, 100, 1, 7));
        assertThat(grouped.count()).isEqualTo(100);
        assertThat(grouped.mean()).isZero();
        assertThat(grouped.deviation()).isZero();
        assertThat(grouped.bins()).hasSize(1);
        assertThat(grouped.bins().getFirst().cumulative()).isEqualTo(100);
    }

    @Test
    void catalogMatchesEightQuestionsAndActualUnnumberedDifficultyLabels() {
        assertThat(SurveyCatalog.questions()).hasSize(8);
        assertThat(SurveyCatalog.decode("difficulty", "Орташа / Средней сложности / Moderately difficult")).isEqualTo("3");
        assertThat(SurveyCatalog.decode("planning", "Әлі бастаған жоқпын / Ещё не начал(а) / I have not started yet"))
                .isEqualTo("NOT_STARTED");
    }

    @Test
    void malformedGoogleCsvNeverWritesAnything() {
        var repository = mock(SurveyRepository.class);
        var service = new SurveyService(repository);
        var input = new ByteArrayInputStream("Timestamp,unrelated\n2026-09-29T12:00:00Z,yes".getBytes(StandardCharsets.UTF_8));
        assertThatIllegalArgumentException().isThrownBy(() -> service.importGoogleCsv(input, ZoneId.of("Asia/Qyzylorda")));
        verifyNoInteractions(repository);
    }

    private SurveyResponse row(String start, String status, String extension) {
        return new SurveyResponse(start + status + extension, Instant.parse("2026-09-29T10:00:00Z"), true,
                "UNKNOWN", "THREE_FOUR", start, status, extension, "MENTAL", 3, "TWO");
    }
}
