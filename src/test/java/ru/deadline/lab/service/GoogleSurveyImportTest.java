package ru.deadline.lab.service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.deadline.lab.model.ImportResult;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.repository.SurveyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleSurveyImportTest {
    private final SurveyRepository repository = mock(SurveyRepository.class);
    private final SurveyService survey = new SurveyService(repository);
    private final ZoneId zone = ZoneId.of("Asia/Qyzylorda");

    @Test
    void googleImportPreservesInconsistentAnswersAndRepeatableIds() throws Exception {
        String csv = csv(List.of(answers("2026-09-29T05:00:00Z", "SAME_DAY", "THREE_FOUR", "ON_TIME", "MENTAL", "3"),
                answers("2026-09-29T06:00:00Z", "THREE_FOUR", "AFTER", "ON_TIME", "NOT_STARTED", "4")));
        when(repository.importValidated(anyList())).thenAnswer(invocation -> {
            List<SurveyResponse> rows = invocation.getArgument(0);
            return new ImportResult(rows.size(), 0, 0, rows.size());
        });

        assertThat(survey.importGoogleCsv(stream(csv), zone).received()).isEqualTo(2);
        survey.importGoogleCsv(stream(csv), zone);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SurveyResponse>> captured = ArgumentCaptor.forClass(List.class);
        verify(repository, times(2)).importValidated(captured.capture());
        assertThat(captured.getAllValues().get(0)).isEqualTo(captured.getAllValues().get(1));
        assertThat(captured.getValue()).hasSize(2);
        assertThat(captured.getValue().getFirst()).satisfies(row -> {
            assertThat(row.allottedBand()).isEqualTo("SAME_DAY");
            assertThat(row.startBand()).isEqualTo("THREE_FOUR");
            assertThat(row.submissionStatus()).isEqualTo("ON_TIME");
            assertThat(row.planning()).isEqualTo("MENTAL");
            assertThat(row.difficulty()).isEqualTo(3);
            assertThat(row.submittedAt()).isEqualTo(Instant.parse("2026-09-29T05:00:00Z"));
        });
        assertThat(captured.getValue().get(1)).satisfies(row -> {
            assertThat(row.startBand()).isEqualTo("AFTER");
            assertThat(row.submissionStatus()).isEqualTo("ON_TIME");
            assertThat(row.planning()).isEqualTo("NOT_STARTED");
        });
    }

    @Test
    void publicValidationCanonicalImportsAndSurveyKeepStrictConsistency() throws Exception {
        String csv = csv(List.of(answers("2026-09-29T05:00:00Z", "SAME_DAY", "THREE_FOUR", "ON_TIME", "MENTAL", "3")));
        var imported = GoogleCsvReader.read(stream(csv), zone);
        assertThat(survey.validate(imported.getFirst())).isNotEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> survey.importRows(imported));
        String canonical = survey.csvTemplate().split("\n", 2)[0] + "\n"
                + "strict-001,2026-09-29T05:00:00Z,true,UNKNOWN,SAME_DAY,THREE_FOUR,ON_TIME,NO,MENTAL,3,TWO\n";
        assertThatIllegalArgumentException().isThrownBy(() -> survey.importCsv(stream(canonical)));
        Map<String, String> fields = Map.of("eligible", "YES", "allottedBand", "SAME_DAY", "startBand", "THREE_FOUR",
                "submissionStatus", "ON_TIME", "extensionStatus", "NO", "planning", "MENTAL", "difficulty", "3", "otherDeadlines", "TWO");
        assertThatIllegalArgumentException().isThrownBy(() -> survey.submit(fields, "strict-survey", Instant.parse("2026-09-29T05:00:00Z")));
        verifyNoInteractions(repository);
    }

    @Test
    void structuralFailureAfterInconsistentAnswerRejectsEntireGoogleBatch() throws Exception {
        var first = answers("2026-09-29T05:00:00Z", "SAME_DAY", "THREE_FOUR", "ON_TIME", "MENTAL", "3");
        for (var invalid : List.of(
                answers("1999-01-01T00:00:00Z", "THREE_FOUR", "ONE", "ON_TIME", "MENTAL", "3"),
                answers("invalid-date", "THREE_FOUR", "ONE", "ON_TIME", "MENTAL", "3"),
                answers("2026-09-29T06:00:00Z", "THREE_FOUR", "ONE", "INVALID_STATUS", "MENTAL", "3"),
                answers("2026-09-29T06:00:00Z", "THREE_FOUR", "ONE", "ON_TIME", "MENTAL", "6"))) {
            String data = csv(List.of(first, invalid));
            assertThatIllegalArgumentException().isThrownBy(() -> survey.importGoogleCsv(stream(data), zone));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void malformedUtf8AndMissingQuestionHeadersCannotReachPersistence() {
        assertThatIllegalArgumentException().isThrownBy(() -> survey.importGoogleCsv(
                new ByteArrayInputStream(new byte[] {(byte) 0xC3, (byte) 0x28}), zone));
        assertThatIllegalArgumentException().isThrownBy(() -> survey.importGoogleCsv(stream("submittedAt,eligible\n2026-09-29T05:00:00Z,YES\n"), zone));
        verifyNoInteractions(repository);
    }

    private List<String> answers(String timestamp, String allotted, String start, String outcome, String planning, String difficulty) {
        return List.of(timestamp, "YES", allotted, start, outcome, "NO", planning, difficulty, "TWO");
    }

    private String csv(List<List<String>> rows) throws Exception {
        var output = new StringWriter();
        var headers = new ArrayList<String>();
        headers.add("submittedAt");
        SurveyCatalog.questions().forEach(question -> headers.add(question.field()));
        try (var printer = CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).get().print(output)) {
            for (var row : rows) printer.printRecord(row);
        }
        return output.toString();
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
