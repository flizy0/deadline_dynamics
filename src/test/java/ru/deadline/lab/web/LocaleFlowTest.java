package ru.deadline.lab.web;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.deadline.lab.config.LocaleConfig;
import ru.deadline.lab.config.SecurityConfig;
import ru.deadline.lab.service.SurveyService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SurveyController.class, properties = "ADMIN_PASSWORD=unit-test-only-password")
@Import({SecurityConfig.class, LocaleConfig.class, SiteText.class})
class LocaleFlowTest {
    @Autowired MockMvc mvc;
    @MockitoBean SurveyService survey;

    @Test
    void everyLocaleContainsEverySharedAndFeatureMessage() throws Exception {
        for (String basename : new String[]{"messages", "story", "experiment", "explore", "lab", "study", "report", "overview"}) {
            var baseline = loadMessages(basename + ".properties");
            for (String language : new String[]{"ru", "kk", "en"}) {
                var localized = loadMessages(basename + "_" + language + ".properties");
                assertThat(localized.keySet()).as(basename + " in " + language)
                        .containsExactlyInAnyOrderElementsOf(baseline.keySet());
                assertThat(localized.values()).allSatisfy(value -> assertThat(value.toString()).isNotBlank());
            }
        }
    }

    @Test
    void russianIsDefaultAndAllSurveyLanguagesKeepStoredCodes() throws Exception {
        mvc.perform(get("/survey")).andExpect(model().attribute("currentLanguage", "ru"))
                .andExpect(content().string(containsString("Когда начали работу и когда сдали?")));
        for (var item : Map.of("en", "When did you start and when did you submit?",
                "kk", "Жұмысты қашан бастап, қашан тапсырдыңыз?",
                "ru", "Когда начали работу и когда сдали?").entrySet()) {
            mvc.perform(get("/survey").param("lang", item.getKey()))
                    .andExpect(status().isOk()).andExpect(model().attribute("currentLanguage", item.getKey()))
                    .andExpect(content().string(containsString(item.getValue())))
                    .andExpect(content().string(containsString("name=\"startBand\"")))
                    .andExpect(content().string(containsString("value=\"ON_TIME\"")))
                    .andExpect(content().string(not(containsString("??question."))));
        }
    }

    @Test
    void localePersistsAcrossNavigationAndInvalidLanguageDoesNotReplaceIt() throws Exception {
        var result = mvc.perform(get("/survey?lang=kk")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        mvc.perform(get("/survey").session(session)).andExpect(model().attribute("currentLanguage", "kk"));
        mvc.perform(get("/survey?lang=invalid").session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("currentLanguage", "kk"));
    }

    @Test
    void languageLinksPreserveFiltersSimulatorParametersAndRepeatedQueryValues() throws Exception {
        var result = mvc.perform(get(URI.create("/survey?dataset=demo&start=ONE&mode=manual&p=62&size=20&runs=50000"
                + "&threshold=12&seed=123&tag=first&tag=second&note=a%26b&lang=ru"))).andReturn();
        @SuppressWarnings("unchecked")
        var links = (Map<String, String>) result.getModelAndView().getModel().get("languageUrls");
        assertThat(links.get("en")).contains("dataset=demo", "start=ONE", "mode=manual", "p=62",
                "size=20", "runs=50000", "threshold=12", "seed=123", "tag=first&tag=second", "note=a%26b", "lang=en")
                .doesNotContain("lang=ru");
        @SuppressWarnings("unchecked")
        var datasetLinks = (Map<String, String>) result.getModelAndView().getModel().get("datasetUrls");
        assertThat(datasetLinks.get("real")).contains("dataset=real", "start=ONE", "p=62",
                "tag=first&tag=second", "note=a%26b", "lang=ru").doesNotContain("dataset=demo");
    }

    @Test
    void surveyValidationUsesSelectedLanguageAndLanguageLinksNeverContainPostedAnswers() throws Exception {
        var result = mvc.perform(get("/survey?lang=en")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        when(survey.submit(anyMap(), anyString(), any())).thenThrow(new IllegalArgumentException(
                "Ответ 1: Начало работы не может быть раньше выдачи задания. Проверьте allottedBand и startBand."));
        var failure = mvc.perform(post("/survey").session(session).with(csrf())
                .param("eligible", "YES").param("startBand", "EIGHT_PLUS")).andExpect(status().isOk())
                .andExpect(content().string(containsString("The starting time is earlier than the assignment was given.")))
                .andReturn();
        @SuppressWarnings("unchecked")
        var links = (Map<String, String>) failure.getModelAndView().getModel().get("languageUrls");
        assertThat(links.values()).allSatisfy(url -> assertThat(url).doesNotContain("eligible", "startBand", "_csrf"));
        @SuppressWarnings("unchecked")
        var datasets = (Map<String, String>) failure.getModelAndView().getModel().get("datasetUrls");
        assertThat(datasets.values()).allSatisfy(url -> assertThat(url).doesNotContain("eligible", "startBand", "_csrf"));
        assertThat(failure.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("Начало работы не может быть раньше выдачи задания");
    }

    private Properties loadMessages(String name) throws Exception {
        var properties = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(input).as(name).isNotNull();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
