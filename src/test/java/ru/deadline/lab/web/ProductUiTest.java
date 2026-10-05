package ru.deadline.lab.web;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import ru.deadline.lab.config.LocaleConfig;
import ru.deadline.lab.config.SecurityConfig;
import ru.deadline.lab.model.Summary;
import ru.deadline.lab.service.DemoData;
import ru.deadline.lab.service.StatisticsService;
import ru.deadline.lab.service.SurveyCatalog;
import ru.deadline.lab.service.SurveyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = PageController.class, properties = "ADMIN_PASSWORD=unit-test-only-password")
@Import({SecurityConfig.class, LocaleConfig.class, StatisticsService.class, SiteText.class})
class ProductUiTest {
    private static final List<String> PUBLIC_ROUTES = List.of("/", "/data", "/simulator", "/method");
    private static final Pattern NAV = Pattern.compile("<nav\\b[^>]*class=\"main-nav\"[^>]*>(.*?)</nav>", Pattern.DOTALL);
    private static final Pattern LINK = Pattern.compile("<a\\b[^>]*href=\"([^\"]+)\"", Pattern.DOTALL);

    @Autowired MockMvc mvc;
    @Autowired StatisticsService statistics;
    @Autowired MessageSource messages;
    @MockitoBean SurveyService survey;

    @BeforeEach
    void setup() {
        when(survey.responses("real")).thenReturn(List.of());
        when(survey.responses("demo")).thenReturn(DemoData.responses());
        when(survey.realCount()).thenReturn(0L);
        when(survey.allCount()).thenReturn(0L);
    }

    @Test
    void fourPublicDestinationsAndBrandRenderInEveryLanguageAndDataset() throws Exception {
        for (String language : List.of("ru", "kk", "en")) {
            for (String dataset : List.of("real", "demo")) {
                for (String route : PUBLIC_ROUTES) {
                    var response = mvc.perform(get(route).param("dataset", dataset).param("lang", language))
                            .andExpect(status().isOk()).andExpect(model().attribute("currentLanguage", language))
                            .andReturn();
                    String html = rendered(response, language);
                    assertThat(html).contains("Deadline Dynamics").doesNotContain("До дедлайна");
                    assertThat(Pattern.compile("<title>[^<]*Deadline Dynamics[^<]*</title>").matcher(html).find()).isTrue();
                    var nav = NAV.matcher(html);
                    assertThat(nav.find()).as("Public navigation on %s (%s)", route, language).isTrue();
                    assertThat(paths(nav.group(1))).containsExactlyElementsOf(PUBLIC_ROUTES);
                    assertThat(paths(html)).doesNotContain("/survey", "/admin", "/login", "/csv-template.csv");
                }
            }
        }
    }

    @Test
    void dataAndSourceKeepCompatibleRoutesWithoutReintroducingInternalWorkflows() throws Exception {
        mvc.perform(get("/data").param("dataset", "demo")).andExpect(status().isOk()).andExpect(view().name("frequencies"));
        mvc.perform(get("/frequencies").param("dataset", "demo")).andExpect(status().isOk()).andExpect(view().name("frequencies"));
        for (String route : List.of("/source", "/connection")) {
            String html = rendered(mvc.perform(get(route).param("lang", "en"))
                    .andExpect(status().isOk()).andExpect(view().name("connection"))
                    .andExpect(model().attribute("googleForm", SurveyCatalog.GOOGLE_FORM)).andReturn(), "en");
            assertThat(html).contains(SurveyCatalog.GOOGLE_FORM, "Google Forms");
            assertThat(paths(html)).doesNotContain("/survey", "/admin", "/csv-template.csv");
        }
    }

    @Test
    void overviewShowsTheSampleAndDataContainsAllSevenChapters() throws Exception {
        var overview = mvc.perform(get("/").param("dataset", "demo"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("comparison", "secondaryFindings"))
                .andReturn();
        String overviewHtml = rendered(overview, "ru");
        assertThat(Pattern.compile("<canvas\\b").matcher(overviewHtml).results().count()).isEqualTo(3);
        assertThat(overviewHtml).contains("overview-startBand", "overview-planning", "overview-difficulty", "overview-source-title")
                .doesNotContain("id=\"overview-data\"", "frequency-chart-data", "name=\"allotted\"");
        String dataHtml = rendered(mvc.perform(get("/data").param("dataset", "demo")
                .param("variable", "planning").param("analysis", "relationship").param("view", "lollipop"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("sections", "dataVariables"))
                .andReturn(), "ru");
        for (String field : StatisticsService.DATA_VARIABLES) assertThat(dataHtml).contains("id=\"chapter-" + field + "\"");
        assertThat(Pattern.compile("<canvas\\b").matcher(dataHtml).results().count()).isEqualTo(13);
        assertThat(dataHtml).contains("data-report-payload", "data-report-view")
                .doesNotContain("<form", "name=\"variable\"", "name=\"analysis\"", "name=\"view\"", "name=\"allotted\"");
    }

    @Test
    void methodCountsAreTheActualSelectedSampleRatherThanIllustrativeNumbers() throws Exception {
        var response = mvc.perform(get("/method").param("dataset", "demo").param("allotted", "THREE_FOUR")
                .param("planning", "MENTAL").param("excludeExtensions", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalDataset", DemoData.responses().size()))
                .andExpect(model().attribute("allCount", (long) DemoData.responses().size())).andReturn();
        var rows = DemoData.responses();
        assertThat(response.getModelAndView().getModel().get("summary")).isEqualTo(statistics.summarize(rows));
        assertThat(((Summary) response.getModelAndView().getModel().get("summary")).known())
                .isLessThanOrEqualTo(rows.size());
        String html = rendered(response, "ru");
        assertThat(Pattern.compile("data-formula=\"").matcher(html).results().count()).isEqualTo(17);
        assertThat(Pattern.compile("data-formula-group=\"").matcher(html).results().count()).isEqualTo(5);
        assertThat(html).contains("data-method-timeline", "data-worked-example").doesNotContain("data-quiz", "<form", "dataset-notice");
    }

    @Test
    void realIsDefaultAndHiddenLegacyFiltersCannotChangeThePublicReport() throws Exception {
        var realRows = DemoData.responses().subList(0, 29);
        when(survey.responses("real")).thenReturn(realRows);
        when(survey.realCount()).thenReturn(29L);
        when(survey.allCount()).thenReturn(52L);
        for (String route : List.of("/", "/data", "/method")) {
            var response = mvc.perform(get(route).param("start", "UNKNOWN").param("planning", "UNKNOWN"))
                    .andExpect(status().isOk()).andExpect(model().attribute("dataset", "real"))
                    .andExpect(model().attribute("summary", statistics.summarize(realRows)))
                    .andExpect(model().attribute("allCount", 52L)).andReturn();
            rendered(response, "ru");
        }
        mvc.perform(get("/simulator")).andExpect(status().isOk()).andExpect(model().attribute("dataset", "real"));
    }

    @Test
    void languageSwitchRetainsExplorerAndProgressiveLabStateAndThenPersists() throws Exception {
        List<String> urls = List.of(
                "/data?dataset=demo&variable=planning&analysis=relationship&view=lollipop&allotted=THREE_FOUR&planning=MENTAL&excludeExtensions=true&lang=ru",
                "/simulator?dataset=demo&source=custom&labMode=single&stage=repeat&p=62.4&size=20&runs=10000&threshold=12&seed=123&compareBand=FIVE_SEVEN&lang=ru",
                "/simulator?dataset=demo&source=earlier&labMode=compare&stage=group&size=20&compareBand=THREE_FOUR&start=ONE&lang=ru");
        for (String url : urls) {
            MvcResult response = mvc.perform(get(URI.create(url))).andExpect(status().isOk()).andReturn();
            @SuppressWarnings("unchecked")
            var languageUrls = (Map<String, String>) response.getModelAndView().getModel().get("languageUrls");
            String expected = url.replace("lang=ru", "lang=kk");
            assertThat(languageUrls.get("kk")).isEqualTo(expected);
            @SuppressWarnings("unchecked")
            var datasetUrls = (Map<String, String>) response.getModelAndView().getModel().get("datasetUrls");
            var expectedDataset = UriComponentsBuilder.fromUriString(url.replace("dataset=demo", "dataset=real")).build();
            var actualDataset = UriComponentsBuilder.fromUriString(datasetUrls.get("real")).build();
            assertThat(actualDataset.getPath()).isEqualTo(expectedDataset.getPath());
            assertThat(actualDataset.getQueryParams()).isEqualTo(expectedDataset.getQueryParams());
            var switched = mvc.perform(get(URI.create(languageUrls.get("kk"))))
                    .andExpect(status().isOk()).andExpect(model().attribute("currentLanguage", "kk")).andReturn();
            var session = (MockHttpSession) switched.getRequest().getSession();
            mvc.perform(get("/source").session(session)).andExpect(status().isOk())
                    .andExpect(model().attribute("currentLanguage", "kk"));
        }
    }

    @Test
    void noDataAndInvalidInputNeverBecomeProbabilityZeroOrNonFiniteOutput() throws Exception {
        for (String language : List.of("ru", "kk", "en")) {
            for (String route : List.of("/data?dataset=real&analysis=relationship", "/simulator?dataset=real&source=earlier",
                    "/simulator?dataset=real&labMode=compare&stage=repeat")) {
                rendered(mvc.perform(get(URI.create(route + "&lang=" + language)))
                        .andExpect(status().isOk()).andReturn(), language);
            }
            mvc.perform(get("/simulator").param("dataset", "real").param("source", "overall").param("lang", language))
                    .andExpect(status().isOk()).andExpect(model().attribute("emptySimulation", true))
                    .andExpect(model().attributeDoesNotExist("result"));
            for (String invalid : List.of("NaN", "Infinity", "oops", "101", "-1")) {
                var response = mvc.perform(get("/simulator").param("source", "custom").param("p", invalid)
                        .param("lang", language)).andExpect(status().isOk())
                        .andExpect(model().attributeExists("errorKey"))
                        .andExpect(model().attributeDoesNotExist("result")).andReturn();
                rendered(response, language);
            }
        }
    }

    @Test
    void firstLabExperienceIsOneGroupAndAdvancedStatisticsStayCollapsed() throws Exception {
        var response = mvc.perform(get("/simulator").param("dataset", "demo"))
                .andExpect(status().isOk()).andExpect(model().attribute("source", "overall"))
                .andExpect(model().attribute("stage", "group")).andExpect(model().attribute("labMode", "single"))
                .andReturn();
        String html = rendered(response, "ru");
        assertThat(html).contains("data-lab-dots", "data-lab-repeat", "id=\"lab-experiment\"")
                .doesNotContain("id=\"lab-distribution\"", "data-viz-tab");
        var details = Pattern.compile("<details\\b[^>]*>").matcher(html);
        assertThat(details.results().map(match -> match.group()).toList()).allSatisfy(tag -> assertThat(tag).doesNotContain(" open"));
    }

    @Test
    void newProductBundlesAreCompleteAndTheBrandIsIdenticalInEveryLanguage() throws Exception {
        for (String basename : List.of("explore", "lab", "study", "report", "overview")) {
            Properties baseline = bundle(basename + ".properties");
            for (String language : List.of("ru", "kk", "en")) {
                Properties localized = bundle(basename + "_" + language + ".properties");
                assertThat(localized.keySet()).as(basename + " in " + language)
                        .containsExactlyInAnyOrderElementsOf(baseline.keySet());
                assertThat(localized.values()).allSatisfy(value -> assertThat(value.toString()).isNotBlank());
            }
        }
        for (String language : List.of("ru", "kk", "en")) {
            assertThat(messages.getMessage("brand.name", null, java.util.Locale.forLanguageTag(language)))
                    .isEqualTo("Deadline Dynamics");
        }
    }

    private String rendered(MvcResult response, String language) throws Exception {
        String html = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("lang=\"" + language + "\"").doesNotContain("??", "NaN", "Infinity", "undefined");
        return html;
    }

    private static List<String> paths(String html) {
        var links = LINK.matcher(html);
        List<String> paths = new ArrayList<>();
        while (links.find()) {
            String href = links.group(1).replace("&amp;", "&");
            if (href.startsWith("/")) paths.add(URI.create(href).getPath());
        }
        return paths;
    }

    private Properties bundle(String name) throws Exception {
        var result = new Properties();
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(stream).as(name).isNotNull();
            result.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return result;
    }
}
