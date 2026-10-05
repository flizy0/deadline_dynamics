package ru.deadline.lab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.deadline.lab.model.ImportResult;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.repository.SurveyRepository;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SurveyServiceTest {
    private SurveyRepository repository;
    private SurveyService service;

    @BeforeEach
    void setUp() {
        repository = mock(SurveyRepository.class);
        service = new SurveyService(repository);
        when(repository.importValidated(anyList())).thenAnswer(invocation -> {
            List<SurveyResponse> rows = invocation.getArgument(0);
            return new ImportResult(rows.size(), 0, 0, rows.size());
        });
    }

    @Test
    void invalidFinalRowPreventsAnyRepositoryWrite() {
        SurveyResponse invalid = new SurveyResponse("invalid", Instant.parse("2026-09-01T08:00:00Z"), true,
                "CODE", "TWO", "FIVE_SEVEN", "ON_TIME", "NO", "MENTAL", 3, "TWO");

        assertThatIllegalArgumentException().isThrownBy(() -> service.importRows(List.of(valid("first"), invalid)))
                .withMessageContaining("Ответ 2").withMessageContaining("раньше выдачи");
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateIdsInBatchAreRejectedWithoutPartialWrites() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.importRows(List.of(valid("duplicate"), valid("duplicate"))))
                .withMessageContaining("повторяется externalId");
        verifyNoInteractions(repository);
    }

    @Test
    void unknownFieldsAndInvalidDifficultyAreExplained() {
        var invalid = new SurveyResponse("bad", Instant.parse("2026-09-01T08:00:00Z"), true,
                "INVENTED", "TWO", null, "ON_TIME", "NO", "MENTAL", 9, "TWO");
        assertThat(service.validate(invalid)).anyMatch(error -> error.contains("assignmentType"))
                .anyMatch(error -> error.contains("startBand")).anyMatch(error -> error.contains("difficulty"));
        assertThat(service.validate(null)).isNotEmpty();
    }

    @Test
    void impossibleCompletionStatusIsRejected() {
        var after = new SurveyResponse("after", Instant.parse("2026-09-01T08:00:00Z"), true,
                "CODE", "TWO", "AFTER", "ON_TIME", "NO", "NONE", 3, "TWO");
        var notStarted = new SurveyResponse("none", Instant.parse("2026-09-01T08:00:00Z"), true,
                "CODE", "TWO", "NOT_STARTED", "LATE", "NO", "NONE", 3, "TWO");
        assertThat(service.validate(after)).anyMatch(error -> error.contains("после первоначального"));
        assertThat(service.validate(notStarted)).anyMatch(error -> error.contains("ещё не начата"));
    }

    @Test
    void notStartedPlanningCannotContradictKnownStartOrSubmission() {
        var contradictory = new SurveyResponse("plan", Instant.parse("2026-09-01T08:00:00Z"), true,
                "UNKNOWN", "THREE_FOUR", "ONE", "ON_TIME", "NO", "NOT_STARTED", 3, "TWO");
        assertThat(service.validate(contradictory)).anyMatch(error -> error.contains("планировании"));
        assertThatIllegalArgumentException().isThrownBy(() -> service.importRows(List.of(contradictory)));
        verifyNoInteractions(repository);
        var notStarted = new SurveyResponse("not-started", Instant.parse("2026-09-01T08:00:00Z"), true,
                "UNKNOWN", "THREE_FOUR", "NOT_STARTED", "NOT_SUBMITTED", "NO", "NOT_STARTED", 3, "TWO");
        assertThat(service.validate(notStarted)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void ineligibleAnswersCanOmitDetailsAndTimestampsAreNormalizedForPostgres() {
        var ineligible = new SurveyResponse("screened-out", Instant.parse("2026-09-01T08:00:00.123456789Z"),
                false, null, null, null, null, null, null, null, null);
        service.importRows(List.of(ineligible));

        ArgumentCaptor<List<SurveyResponse>> argument = ArgumentCaptor.forClass(List.class);
        verify(repository).importValidated(argument.capture());
        var stored = argument.getValue().getFirst();
        assertThat(stored.submittedAt()).isEqualTo(Instant.parse("2026-09-01T08:00:00.123456Z"));
        assertThat(stored.assignmentType()).isEqualTo("UNKNOWN");
        assertThat(stored.submissionStatus()).isEqualTo("UNKNOWN");
        assertThat(stored.eligible()).isFalse();
    }

    @Test
    void csvAcceptsBomAndCanonicalTemplate() {
        ImportResult result = service.importCsv(stream("\uFEFF" + service.csvTemplate()));
        assertThat(result.inserted()).isEqualTo(2);
        assertThat(result.received()).isEqualTo(2);
    }

    @Test
    void csvRejectsWrongHeadersAndColumnCount() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.importCsv(stream(service.csvTemplate().replace("externalId", "id"))))
                .withMessageContaining("Столбцы CSV");
        assertThatIllegalArgumentException().isThrownBy(() -> service.importCsv(stream(service.csvTemplate().replace(",3,TWO", ",3"))))
                .withMessageContaining("число столбцов");
        verifyNoInteractions(repository);
    }

    @Test
    void csvDoesNotSilentlyTurnInvalidBooleansIntoFalse() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.importCsv(stream(service.csvTemplate().replace(",true,", ",yes,"))))
                .withMessageContaining("eligible должен быть true или false");
        verifyNoInteractions(repository);
    }

    @Test
    void csvRejectsMalformedAndInvalidUtf8Files() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.importCsv(stream(service.csvTemplate() + "\"unterminated")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.importCsv(new ByteArrayInputStream(new byte[]{(byte) 0xC3, 0x28})));
        verifyNoInteractions(repository);
    }

    @Test
    void csvSupportsAnEmptyOptionalDifficulty() {
        ImportResult result = service.importCsv(stream(service.csvTemplate().replace(",3,TWO", ",,TWO")));
        assertThat(result.received()).isEqualTo(2);
    }

    @Test
    void batchSizeIsBoundedBeforeAccessingDatabase() {
        List<SurveyResponse> tooMany = IntStream.range(0, 5001).mapToObj(i -> valid("id-" + i)).toList();
        assertThatIllegalArgumentException().isThrownBy(() -> service.importRows(tooMany)).withMessageContaining("5000");
        verifyNoInteractions(repository);
    }

    @Test
    void demoIsDeterministicValidAndDoesNotReadDatabase() {
        List<SurveyResponse> demo = service.responses("demo");
        assertThat(demo).hasSize(144).isEqualTo(service.responses("demo"));
        assertThat(demo.stream().map(SurveyResponse::externalId).distinct().count()).isEqualTo(144);
        assertThat(demo).allMatch(row -> service.validate(row).isEmpty());
        verifyNoInteractions(repository);
    }

    private SurveyResponse valid(String id) {
        return new SurveyResponse(id, Instant.parse("2026-09-01T08:00:00Z"), true,
                "CODE", "THREE_FOUR", "ONE", "ON_TIME", "NO", "MENTAL", 3, "TWO");
    }

    private ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
