package ru.deadline.lab.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.deadline.lab.service.SurveyCatalog;
import ru.deadline.lab.service.SurveyService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Controller
public class SurveyController {
    private final SurveyService survey;
    private final SiteText siteText;
    public SurveyController(SurveyService survey, SiteText siteText) {
        this.survey = survey;
        this.siteText = siteText;
    }

    @GetMapping("/survey")
    public String form(HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("surveyComplete"))) return "redirect:/survey/thanks";
        if (session.getAttribute("surveyId") == null) {
            session.setAttribute("surveyId", "web-" + UUID.randomUUID());
        }
        populate(model, Map.of());
        return "survey";
    }

    @PostMapping("/survey")
    public String submit(@RequestParam Map<String, String> values, HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("surveyComplete"))) return "redirect:/survey/thanks";
        if (session.getAttribute("surveyId") == null) return "redirect:/survey";
        if (session.getAttribute("surveyTime") == null) session.setAttribute("surveyTime", Instant.now());
        try {
            survey.submit(values, (String) session.getAttribute("surveyId"), (Instant) session.getAttribute("surveyTime"));
            session.setAttribute("surveyComplete", true);
            session.setAttribute("surveyEligible", "YES".equals(values.get("eligible")));
            return "redirect:/survey/thanks";
        } catch (IllegalArgumentException exception) {
            session.removeAttribute("surveyTime");
            model.addAttribute("error", siteText.surveyError(exception.getMessage()));
        } catch (DataAccessException exception) {
            model.addAttribute("error", siteText.text("survey.error.storage"));
        }
        populate(model, values);
        return "survey";
    }

    @GetMapping("/survey/thanks")
    public String thanks(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("surveyComplete"))) return "redirect:/survey";
        populate(model, Map.of());
        model.addAttribute("eligible", Boolean.TRUE.equals(session.getAttribute("surveyEligible")));
        return "thanks";
    }

    private void populate(Model model, Map<String, String> values) {
        model.addAttribute("questions", SurveyCatalog.questions());
        model.addAttribute("values", values);
        model.addAttribute("page", "survey");
        model.addAttribute("dataset", "real");
        model.addAttribute("googleForm", SurveyCatalog.GOOGLE_FORM);
    }
}
