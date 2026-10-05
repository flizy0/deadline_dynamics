package ru.deadline.lab.web;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.deadline.lab.config.LocaleConfig;
import ru.deadline.lab.config.SecurityConfig;
import ru.deadline.lab.model.ImportResult;
import ru.deadline.lab.model.SimulationResult;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.service.DemoData;
import ru.deadline.lab.service.StatisticsService;
import ru.deadline.lab.service.SurveyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {PageController.class, SurveyController.class, AdminController.class},
        properties = {"ADMIN_USERNAME=admin", "ADMIN_PASSWORD=unit-test-only-password"})
@Import({SecurityConfig.class, LocaleConfig.class, StatisticsService.class, SiteText.class})
class WebFlowTest {
    @Autowired MockMvc mvc;
    @Autowired StatisticsService statistics;
    @Autowired MessageSource messages;
    @MockitoBean SurveyService survey;

    @BeforeEach
    void setup() {
        when(survey.responses("real")).thenReturn(List.of());
        when(survey.responses("demo")).thenReturn(DemoData.responses());
        when(survey.csvTemplate()).thenReturn("externalId,submittedAt\n");
    }

    @Test
    void allPublicPagesRenderInEveryLanguageWithRealAndDemoData() throws Exception {
        for (String language : List.of("ru", "kk", "en")) {
            for (String dataset : List.of("real", "demo")) {
                for (String url : List.of("/", "/results", "/data", "/frequencies", "/simulator", "/method", "/source", "/connection", "/survey", "/login")) {
                    MvcResult result = mvc.perform(get(url).param("dataset", dataset).param("lang", language))
                            .andExpect(status().isOk())
                            .andExpect(model().attribute("currentLanguage", language)).andReturn();
                    assertRenderedText(result, language);
                }
            }
        }
    }

    @Test
    void dashboardAndFrequencyPagesKeepServerCalculatedDatasetValues() throws Exception {
        mvc.perform(get("/").param("dataset", "demo").param("compareBand", "FIVE_SEVEN"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dataset", "demo"))
                .andExpect(model().attribute("totalDataset", DemoData.responses().size()))
                .andExpect(model().attribute("summary", statistics.summarize(DemoData.responses())))
                .andExpect(model().attribute("compareBand", "FIVE_SEVEN"))
                .andExpect(model().attribute("comparison", statistics.compare(DemoData.responses(), "FIVE_SEVEN")));
        mvc.perform(get("/frequencies").param("dataset", "demo"))
                .andExpect(status().isOk()).andExpect(model().attribute("dataset", "demo"))
                .andExpect(model().attribute("tables", hasSize(7)));
        mvc.perform(get("/simulator").param("dataset", "real"))
                .andExpect(status().isOk()).andExpect(model().attribute("emptySimulation", true))
                .andExpect(model().attributeDoesNotExist("result"));
    }

    @Test
    void observedProbabilityUsesOnlySubmittedOutcomes() throws Exception {
        List<SurveyResponse> rows = List.of(row("a", "ON_TIME"), row("b", "ON_TIME"),
                row("c", "LATE"), row("d", "NOT_SUBMITTED"), row("e", "UNKNOWN"));
        when(survey.responses("real")).thenReturn(rows);
        MvcResult response = mvc.perform(get("/simulator").param("dataset", "real"))
                .andExpect(status().isOk()).andExpect(view().name("simulator")).andReturn();
        Summary summary = (Summary) response.getModelAndView().getModel().get("summary");
        SimulationResult result = simulation(response);

        assertThat(summary.total()).isEqualTo(5);
        assertThat(summary.known()).isEqualTo(3);
        assertThat(summary.pending()).isEqualTo(1);
        assertThat(summary.unknown()).isEqualTo(1);
        assertThat(result.probability()).isCloseTo(2.0 / 3.0, within(1e-12));
    }

    @Test
    void observedProbabilityRespectsCurrentFilters() throws Exception {
        List<SurveyResponse> filtered = DemoData.responses().stream()
                .filter(row -> "THREE_FOUR".equals(row.allottedBand()))
                .filter(row -> "NO".equals(row.extensionStatus())).toList();
        Summary expected = statistics.summarize(filtered);
        MvcResult response = mvc.perform(get("/simulator").param("dataset", "demo")
                .param("allotted", "THREE_FOUR").param("excludeExtensions", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("summary", expected)).andReturn();

        assertThat(expected.known()).isPositive();
        assertThat(simulation(response).probability()).isCloseTo(expected.onTimePercent() / 100.0, within(1e-12));
    }

    @Test
    void manualProbabilitySupportsBoundaryValuesSmallGroupsAndFiftyThousandRuns() throws Exception {
        for (int percentage : List.of(0, 100)) {
            MvcResult response = mvc.perform(get("/simulator").param("mode", "manual")
                    .param("p", Integer.toString(percentage)).param("size", "1")
                    .param("runs", "50000").param("threshold", "1").param("seed", "17"))
                    .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("errorKey", "error"))
                    .andReturn();
            SimulationResult result = simulation(response);
            double probability = percentage / 100.0;
            assertThat(result.probability()).isEqualTo(probability);
            assertThat(result.runs()).isEqualTo(50_000);
            assertThat(result.expected()).isEqualTo(probability);
            assertThat(result.theoreticalTail()).isEqualTo(probability);
            assertThat(result.empiricalTail()).isEqualTo(probability);
            assertThat(result.firstGroup()).containsExactly(percentage == 100);
            assertThat(result.histogram().stream().mapToInt(bin -> bin.observed()).sum()).isEqualTo(50_000);
            assertRenderedText(response, "ru");
        }
        MvcResult randomSeed = mvc.perform(get("/simulator").param("mode", "manual")
                .param("p", "62.4").param("seed", ""))
                .andExpect(status().isOk()).andExpect(model().attribute("seed", ""))
                .andExpect(model().attributeDoesNotExist("errorKey")).andReturn();
        assertThat(simulation(randomSeed).size()).isEqualTo(20);
    }

    @Test
    void invalidSimulationInputRendersLocalizedErrorWithoutNonFiniteValues() throws Exception {
        for (String language : List.of("ru", "kk", "en")) {
            MvcResult response = mvc.perform(get("/simulator").param("size", "oops").param("lang", language))
                    .andExpect(status().isOk()).andExpect(model().attributeExists("errorKey"))
                    .andExpect(model().attributeDoesNotExist("result")).andReturn();
            assertRenderedText(response, language);
        }
        for (String invalid : List.of("NaN", "Infinity", "-Infinity", "oops", "101", "-1")) {
            MvcResult manualInvalid = mvc.perform(get("/simulator").param("mode", "manual").param("p", invalid))
                    .andExpect(status().isOk()).andExpect(model().attributeExists("errorKey"))
                    .andExpect(model().attribute("p", "62"))
                    .andExpect(model().attributeDoesNotExist("result")).andReturn();
            assertRenderedText(manualInvalid, "ru");
        }
        MvcResult commaDecimal = mvc.perform(get("/simulator").param("mode", "manual").param("p", "62,4")
                .param("dataset", "demo").param("seed", "17"))
                .andExpect(status().isOk()).andExpect(model().attribute("p", "62.4"))
                .andExpect(model().attributeDoesNotExist("errorKey")).andReturn();
        assertThat(simulation(commaDecimal).probability()).isCloseTo(0.624, within(1e-12));
    }

    @Test
    void singleGroupEndpointReturnsServerCalculatedOutcomesAndRejectsInvalidInput() throws Exception {
        mvc.perform(get("/simulator/group").param("p", "100").param("size", "3").param("seed", "17"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.size").value(3)).andExpect(jsonPath("$.probability").value(1.0))
                .andExpect(jsonPath("$.successes").value(3)).andExpect(jsonPath("$.outcomes", hasSize(3)))
                .andExpect(jsonPath("$.outcomes[0]").value(true));
        mvc.perform(get("/simulator/group").param("p", "0").param("size", "1").param("seed", "17"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.successes").value(0))
                .andExpect(jsonPath("$.outcomes[0]").value(false));
        for (Map<String, String> invalid : List.of(Map.of("size", "0", "p", "50"),
                Map.of("size", "101", "p", "50"), Map.of("size", "20", "p", "101"),
                Map.of("size", "20", "p", "NaN"), Map.of("size", "oops", "p", "50"))) {
            mvc.perform(get("/simulator/group").param("p", invalid.get("p")).param("size", invalid.get("size")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorKey").isNotEmpty());
        }
    }

    @Test
    void pairedEndpointSerializesObservedModelsAndKeepsSameGroupSize() throws Exception {
        var scenarios = statistics.labScenarios(DemoData.responses(), "THREE_FOUR");
        mvc.perform(get("/simulator/groups").param("dataset", "demo").param("size", "100").param("seed", "17"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.earlier.size").value(100)).andExpect(jsonPath("$.finalDay.size").value(100))
                .andExpect(jsonPath("$.earlier.probability").value(scenarios.get(1).probability()))
                .andExpect(jsonPath("$.finalDay.probability").value(scenarios.get(2).probability()))
                .andExpect(jsonPath("$.earlier.outcomes", hasSize(100)))
                .andExpect(jsonPath("$.finalDay.outcomes", hasSize(100)));
        mvc.perform(get("/simulator/groups").param("dataset", "real"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorKey").value("experiment.error.comparisonUnavailable"));
        mvc.perform(get("/simulator/groups").param("dataset", "demo").param("size", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorKey").value("experiment.error.size"));
    }

    @Test
    void importsRequireLoginAndStateChangesRequireCsrf() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/survey")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/import").with(user("researcher").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("researcher").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void researcherCanAuthenticateAndImportCanonicalCsv() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "unit-test-only-password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername("admin"));
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        when(survey.importCsv(any(InputStream.class))).thenReturn(new ImportResult(2, 0, 0, 2));
        MockMultipartFile file = new MockMultipartFile("file", "responses.csv", "text/csv",
                "externalId,submittedAt\n".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/import").file(file).param("format", "canonical")
                .with(user("researcher").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("message"));
        verify(survey).importCsv(any(InputStream.class));
        verify(survey, never()).importGoogleCsv(any(InputStream.class), any());
    }

    @Test
    void surveyUsesServerSessionIdAndRefreshDoesNotSubmitTwice() throws Exception {
        var session = (MockHttpSession) mvc.perform(get("/survey")).andReturn().getRequest().getSession();
        mvc.perform(post("/survey").session(session).with(csrf()).param("eligible", "NO"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/survey/thanks"));
        mvc.perform(post("/survey").session(session).with(csrf()).param("eligible", "NO"))
                .andExpect(status().is3xxRedirection());
        verify(survey, times(1)).submit(anyMap(), org.mockito.ArgumentMatchers.startsWith("web-"), any());
        for (String language : List.of("ru", "kk", "en")) {
            MvcResult response = mvc.perform(get("/survey/thanks").session(session).param("lang", language))
                    .andExpect(status().isOk()).andExpect(view().name("thanks"))
                    .andExpect(model().attribute("eligible", false)).andReturn();
            assertRenderedText(response, language);
        }
    }

    @Test
    void invalidSurveyShowsValidationErrorAndKeepsOriginalAnswerCodes() throws Exception {
        var session = (MockHttpSession) mvc.perform(get("/survey")).andReturn().getRequest().getSession();
        when(survey.submit(anyMap(), anyString(), any())).thenThrow(new IllegalArgumentException("Выберите ответ"));
        MvcResult response = mvc.perform(post("/survey").session(session).with(csrf())
                .param("eligible", "YES").param("startBand", "TWO"))
                .andExpect(status().isOk()).andExpect(view().name("survey"))
                .andExpect(model().attributeExists("error")).andReturn();
        Map<?, ?> values = (Map<?, ?>) response.getModelAndView().getModel().get("values");
        assertThat(values.get("eligible")).isEqualTo("YES");
        assertThat(values.get("startBand")).isEqualTo("TWO");
        assertThat(response.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("name=\"startBand\"", "value=\"TWO\"", "checked=\"checked\"");
    }

    @Test
    void reportsAreDownloadsAndDoNotExposeRawRespondents() throws Exception {
        for (String language : List.of("ru", "kk", "en")) {
            MvcResult response = mvc.perform(get("/report.csv").param("dataset", "demo").param("lang", language))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("attachment")))
                    .andExpect(content().string(not(containsString("externalId")))).andReturn();
            String csv = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(csv).doesNotContain("??", "NaN", "Infinity", "undefined");
            String totalLabel = messages.getMessage("report.total", null, Locale.forLanguageTag(language));
            try (var parser = CSVFormat.DEFAULT.parse(new StringReader(csv.replaceFirst("^\\uFEFF", "")))) {
                assertThat(parser.getRecords()).anySatisfy(row -> {
                    assertThat(row.get(0)).isEqualTo(totalLabel);
                    assertThat(row.get(1)).isEqualTo(Integer.toString(DemoData.responses().size()));
                });
            }
        }
        mvc.perform(get("/csv-template.csv")).andExpect(status().isOk());
    }

    private static SimulationResult simulation(MvcResult response) {
        Object result = response.getModelAndView().getModel().get("result");
        assertThat(result).isInstanceOf(SimulationResult.class);
        return (SimulationResult) result;
    }

    private static void assertRenderedText(MvcResult response, String language) throws Exception {
        String html = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("lang=\"" + language + "\"")
                .doesNotContain("??", "NaN", "Infinity", "undefined");
    }

    private static SurveyResponse row(String id, String status) {
        return new SurveyResponse(id, Instant.parse("2026-09-01T08:00:00Z"), true, "CODE", "THREE_FOUR",
                "ONE", status, "NO", "MENTAL", 3, "TWO");
    }
}
