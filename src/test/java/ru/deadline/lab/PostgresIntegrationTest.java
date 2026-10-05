package ru.deadline.lab;

import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.repository.SurveyRepository;
import ru.deadline.lab.service.SurveyCatalog;
import ru.deadline.lab.service.SurveyService;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "ADMIN_PASSWORD=integration-test-only-password")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DEADLINE_PG_TEST", matches = "true")
class PostgresIntegrationTest {
    @Autowired SurveyRepository repository;
    @Autowired SurveyService surveys;
    @Autowired MockMvc mvc;
    @Autowired Environment environment;

    @BeforeEach
    void requireDedicatedEmptyDatabase() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .matches("jdbc:postgresql://127\\.0\\.0\\.1:55432/deadline_lab_verify_[a-f0-9]+");
        assertThat(repository.countAll()).isZero();
    }

    @Test
    void postgresStoresUpdatesAndDeduplicatesNormalizedResponses() {
        var first = row("verify-001", "ON_TIME", true);
        assertThat(surveys.importRows(List.of(first)).inserted()).isEqualTo(1);
        assertThat(surveys.importRows(List.of(first)).unchanged()).isEqualTo(1);
        assertThat(surveys.importRows(List.of(row("verify-001", "LATE", true))).updated()).isEqualTo(1);
        assertThat(repository.findEligible()).singleElement().satisfies(value -> {
            assertThat(value.submissionStatus()).isEqualTo("LATE");
            assertThat(value.submittedAt()).isEqualTo(Instant.parse("2026-09-29T05:00:00.123456Z"));
        });
        surveys.importRows(List.of(row("verify-no", "UNKNOWN", false)));
        assertThat(repository.countAll()).isEqualTo(2);
        assertThat(repository.countEligible()).isEqualTo(1);
    }

    @Test
    void googleCsvIsIdempotentAcrossPersistentImports() throws Exception {
        String csv = googleCsv();
        assertThat(surveys.importGoogleCsv(stream(csv), ZoneId.of("Asia/Qyzylorda")).inserted()).isEqualTo(1);
        assertThat(surveys.importGoogleCsv(stream(csv), ZoneId.of("Asia/Qyzylorda")).unchanged()).isEqualTo(1);
        assertThat(repository.countEligible()).isEqualTo(1);
        assertThat(repository.findEligible().getFirst().difficulty()).isEqualTo(3);
    }

    @Test
    void googleSelfReportInconsistenciesPersistUnchangedAndRemainIdempotent() {
        String csv = "submittedAt,eligible,allottedBand,startBand,submissionStatus,extensionStatus,planning,difficulty,otherDeadlines\n"
                + "2026-09-29T05:00:00Z,YES,SAME_DAY,THREE_FOUR,ON_TIME,NO,MENTAL,3,TWO\n"
                + "2026-09-29T06:00:00Z,YES,THREE_FOUR,AFTER,ON_TIME,NO,NOT_STARTED,4,TWO\n";
        assertThat(surveys.importGoogleCsv(stream(csv), ZoneId.of("Asia/Qyzylorda")).inserted()).isEqualTo(2);
        assertThat(surveys.importGoogleCsv(stream(csv), ZoneId.of("Asia/Qyzylorda")).unchanged()).isEqualTo(2);
        var rows = repository.findEligible();
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().allottedBand()).isEqualTo("SAME_DAY");
        assertThat(rows.getFirst().startBand()).isEqualTo("THREE_FOUR");
        assertThat(rows.get(1).startBand()).isEqualTo("AFTER");
        assertThat(rows.get(1).submissionStatus()).isEqualTo("ON_TIME");
        assertThat(rows.get(1).planning()).isEqualTo("NOT_STARTED");
        assertThat(surveys.validate(rows.getFirst())).isNotEmpty();
        assertThat(surveys.validate(rows.get(1))).isNotEmpty();
    }

    @Test
    void realCollectionDatesIncludeIneligibleSourceExtrema() throws Exception {
        assertThat(surveys.collectionStart("real")).isNull();
        assertThat(surveys.collectionEnd("real")).isNull();
        Instant earliest = Instant.parse("2026-08-31T20:05:00Z");
        Instant latest = Instant.parse("2026-09-02T19:30:00Z");
        surveys.importRows(List.of(timedRow("first-ineligible", earliest, false),
                timedRow("middle-eligible", Instant.parse("2026-09-01T08:00:00Z"), true),
                timedRow("last-ineligible", latest, false)));
        assertThat(repository.countAll()).isEqualTo(3);
        assertThat(repository.countEligible()).isEqualTo(1);
        assertThat(surveys.collectionStart("real")).isEqualTo(earliest);
        assertThat(surveys.collectionEnd("real")).isEqualTo(latest);
        var dashboard = mvc.perform(get("/").param("allotted", "ONE").param("planning", "NONE"))
                .andExpect(status().isOk()).andExpect(model().attribute("allCount", 3L))
                .andExpect(model().attribute("collectionStart", "01.09.2026"))
                .andExpect(model().attribute("collectionEnd", "03.09.2026")).andReturn();
        assertThat(((Summary) dashboard.getModelAndView().getModel().get("summary")).total()).isEqualTo(1);
    }

    @Test
    void completeSurveyPersistsOnceAndAppearsInResults() throws Exception {
        var session = (MockHttpSession) mvc.perform(get("/survey")).andReturn().getRequest().getSession();
        mvc.perform(post("/survey").session(session).with(csrf())
                .param("eligible", "YES").param("allottedBand", "THREE_FOUR")
                .param("startBand", "ONE").param("submissionStatus", "ON_TIME")
                .param("extensionStatus", "NO").param("planning", "MENTAL")
                .param("difficulty", "3").param("otherDeadlines", "TWO"))
                .andExpect(redirectedUrl("/survey/thanks"));
        mvc.perform(post("/survey").session(session).with(csrf()).param("eligible", "NO"))
                .andExpect(redirectedUrl("/survey/thanks"));
        assertThat(repository.countAll()).isEqualTo(1);
        var dashboard = mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(model().attribute("realCount", 1L)).andReturn();
        var summary = (Summary) dashboard.getModelAndView().getModel().get("summary");
        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.onTime()).isEqualTo(1);
        assertThat(summary.known()).isEqualTo(1);
        mvc.perform(get("/frequencies")).andExpect(status().isOk());
    }

    @Test
    void oneInvalidResponseRejectsWholeImportBeforeDatabaseWrites() {
        assertThatIllegalArgumentException().isThrownBy(() -> surveys.importRows(List.of(
                row("valid", "ON_TIME", true), row("invalid", "BAD_STATUS", true))));
        assertThat(repository.countAll()).isZero();
    }

    @Test
    void authenticatedCsvUploadPersistsAndInvalidUploadKeepsPreviousData() throws Exception {
        var valid = new MockMultipartFile("file", "responses.csv", "text/csv", googleCsv().getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/import").file(valid).with(user("researcher").roles("ADMIN"))
                .with(csrf()).param("format", "google").param("timezone", "Asia/Qyzylorda"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("message", containsString("новых 1")));
        var invalid = new MockMultipartFile("file", "broken.csv", "text/csv", "bad,header\n1,2".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/import").file(invalid).with(user("researcher").roles("ADMIN"))
                .with(csrf()).param("format", "google"))
                .andExpect(redirectedUrl("/admin")).andExpect(flash().attributeExists("error"));
        assertThat(repository.countAll()).isEqualTo(1);
    }

    private SurveyResponse row(String id, String status, boolean eligible) {
        return new SurveyResponse(id, Instant.parse("2026-09-29T05:00:00.123456789Z"), eligible, "UNKNOWN",
                "THREE_FOUR", "ONE", status, "NO", "MENTAL", 3, "TWO");
    }

    private SurveyResponse timedRow(String id, Instant submittedAt, boolean eligible) {
        return new SurveyResponse(id, submittedAt, eligible, "UNKNOWN", "THREE_FOUR", "ONE", eligible ? "ON_TIME" : "UNKNOWN",
                "NO", "MENTAL", 3, "TWO");
    }

    private String googleCsv() throws Exception {
        var output = new StringWriter();
        var headers = new ArrayList<String>();
        headers.add("Отметка времени");
        SurveyCatalog.questions().forEach(question -> headers.add(question.title()));
        var codes = List.of("YES", "THREE_FOUR", "ONE", "ON_TIME", "NO", "MENTAL", "3", "TWO");
        var answers = new ArrayList<String>();
        answers.add("29.09.2026 10:00:00");
        for (int i = 0; i < codes.size(); i++) {
            answers.add(SurveyCatalog.labels(SurveyCatalog.questions().get(i).field()).get(codes.get(i)));
        }
        try (var printer = CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).get().print(output)) {
            printer.printRecord(answers);
        }
        return output.toString();
    }

    private ByteArrayInputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
