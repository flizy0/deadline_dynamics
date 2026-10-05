package ru.deadline.lab.service;

import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GoogleCsvReaderTest {
    private final ZoneId zone = ZoneId.of("Asia/Qyzylorda");

    @Test
    void importsActualTrilingualGoogleLabelsAndIgnoresEmailColumn() throws Exception {
        var rows = GoogleCsvReader.read(stream(csv("YES", "ONE", false)), zone);
        assertThat(rows).hasSize(1);
        var row = rows.getFirst();
        assertThat(row.submittedAt()).isEqualTo(Instant.parse("2026-09-29T05:00:00Z"));
        assertThat(row.difficulty()).isEqualTo(3);
        assertThat(row.allottedBand()).isEqualTo("THREE_FOUR");
        assertThat(row.startBand()).isEqualTo("ONE");
        assertThat(row.externalId()).startsWith("google-");
        assertThat(row.toString()).doesNotContain("private@example.org");
        assertThat(rows).isEqualTo(GoogleCsvReader.read(stream(csv("YES", "ONE", false)), zone));
    }

    @Test
    void identicalSameSecondResponsesRemainSeparateAndKeepStableIds() throws Exception {
        var rows = GoogleCsvReader.read(stream(csv("YES", "ONE", true)), zone);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).externalId()).isNotEqualTo(rows.get(1).externalId());
        assertThat(rows).isEqualTo(GoogleCsvReader.read(stream(csv("YES", "ONE", true)), zone));
    }

    @Test
    void screenedOutRespondentsMayLeaveMainQuestionsEmpty() throws Exception {
        var row = GoogleCsvReader.read(stream(csv("NO", "", false)), zone).getFirst();
        assertThat(row.eligible()).isFalse();
        assertThat(row.startBand()).isEqualTo("UNKNOWN");
        assertThat(row.difficulty()).isNull();
    }

    @Test
    void unknownAnswerIsRejectedInsteadOfInventingMapping() throws Exception {
        String data = csv("YES", "ONE", false).replace("1 күн бұрын / За 1 день / 1 day before", "unrecognized");
        assertThatIllegalArgumentException().isThrownBy(() -> GoogleCsvReader.read(stream(data), zone)).withMessageContaining("Строка 2");
    }

    @Test
    void handlesTimezonesAndRejectsImpossibleDates() {
        assertThat(GoogleCsvReader.parseTime("2026-09-29T05:00:00Z", zone)).isEqualTo(Instant.parse("2026-09-29T05:00:00Z"));
        assertThat(GoogleCsvReader.parseTime("9/29/2026 10:00:00", zone)).isEqualTo(Instant.parse("2026-09-29T05:00:00Z"));
        assertThatIllegalArgumentException().isThrownBy(() -> GoogleCsvReader.parseTime("31.02.2026 10:00:00", zone));
        assertThatIllegalArgumentException().isThrownBy(() -> GoogleCsvReader.read(null, zone));
    }

    private String csv(String eligible, String start, boolean duplicate) throws Exception {
        StringWriter output = new StringWriter();
        var headers = new ArrayList<String>();
        headers.add("Отметка времени");
        SurveyCatalog.questions().forEach(q -> headers.add(q.title()));
        headers.add("Email");
        var codes = List.of(eligible, "THREE_FOUR", start, "ON_TIME", "NO", "MENTAL", "3", "TWO");
        var values = new ArrayList<String>();
        values.add("29.09.2026 10:00:00");
        for (int i = 0; i < 8; i++) {
            int index = i;
            values.add(i > 0 && eligible.equals("NO") ? "" : SurveyCatalog.questions().get(i).options().stream()
                    .filter(o -> o.code().equals(codes.get(index))).findFirst().orElseThrow().label());
        }
        values.add("private@example.org");
        try (var printer = CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).get().print(output)) {
            printer.printRecord(values);
            if (duplicate) printer.printRecord(values);
        }
        return output.toString();
    }
    private ByteArrayInputStream stream(String value) { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
}
