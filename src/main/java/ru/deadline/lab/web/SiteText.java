package ru.deadline.lab.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;
import ru.deadline.lab.service.SurveyCatalog;

@ControllerAdvice
public class SiteText {
    private final MessageSource messages;

    public SiteText(MessageSource messages) {
        this.messages = messages;
    }

    @ModelAttribute
    public void localeContext(HttpServletRequest request, Model model, Locale locale) {
        model.addAttribute("currentLanguage", locale.getLanguage());
        Map<String, String> urls = new LinkedHashMap<>();
        // Only the URL query is copied: POST answers and CSRF tokens never become language links.
        String query = request.getQueryString();
        String currentUrl = request.getRequestURI() + (query == null ? "" : "?" + query);
        for (String language : new String[]{"kk", "ru", "en"}) {
            urls.put(language, UriComponentsBuilder.fromUriString(currentUrl)
                    .replaceQueryParam("lang", language).build(true).toUriString());
        }
        model.addAttribute("languageUrls", urls);
        Map<String, String> datasetUrls = new LinkedHashMap<>();
        for (String dataset : new String[]{"real", "demo"}) {
            datasetUrls.put(dataset, UriComponentsBuilder.fromUriString(currentUrl)
                    .replaceQueryParam("dataset", dataset).build(true).toUriString());
        }
        model.addAttribute("datasetUrls", datasetUrls);
    }

    public String option(String field, String code) {
        return text("option." + field + "." + code);
    }

    public String label(String field, String original) {
        return SurveyCatalog.question(field).options().stream()
                .filter(option -> option.ru().equals(original) || option.label().equals(original))
                .findFirst().map(option -> option(field, option.code())).orElse(original);
    }

    public String surveyError(String original) {
        if ("Ответьте на первый вопрос.".equals(original)) return text("survey.error.eligible");
        if ("Выберите ответ".equals(original)) return text("survey.error.required");
        for (var question : SurveyCatalog.questions()) {
            if (("Выберите ответ: " + question.shortTitle()).equals(original)) {
                return messages.getMessage("survey.error.field", new Object[]{text("short." + question.field())},
                        LocaleContextHolder.getLocale());
            }
        }
        if (original != null && original.contains("Начало работы не может быть раньше выдачи задания")) {
            return text("survey.error.beforeAssigned");
        }
        if (original != null && original.contains("Задание нельзя сдать, если работа ещё не начата")) {
            return text("survey.error.notStarted");
        }
        if (original != null && original.contains("Начало после первоначального дедлайна")) {
            return text("survey.error.afterDeadline");
        }
        if (original != null && original.contains("в планировании противоречит")) {
            return text("survey.error.plan");
        }
        return text("survey.error.required");
    }

    public String text(String key) {
        return messages.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    public String text(String key, Object... arguments) {
        return messages.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}
