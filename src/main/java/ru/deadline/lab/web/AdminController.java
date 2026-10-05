package ru.deadline.lab.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.deadline.lab.service.SurveyService;

@Controller
public class AdminController {
    private final SurveyService surveys;
    private final SiteText siteText;

    public AdminController(SurveyService surveys, SiteText siteText) {
        this.surveys = surveys;
        this.siteText = siteText;
    }

    @GetMapping("/admin")
    String admin(Model model) {
        model.addAttribute("page", "admin");
        model.addAttribute("dataset", "real");
        model.addAttribute("realCount", surveys.realCount());
        model.addAttribute("allCount", surveys.allCount());
        return "admin";
    }

    @PostMapping("/admin/import")
    String importCsv(@RequestParam("file") MultipartFile file,
                     @RequestParam(defaultValue = "google") String format,
                     @RequestParam(defaultValue = "Asia/Qyzylorda") String timezone, RedirectAttributes redirect) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            redirect.addFlashAttribute("error", siteText.text("admin.error.file"));
            return "redirect:/admin";
        }
        try (var input = file.getInputStream()) {
            if (!java.util.List.of("google", "canonical").contains(format)) throw new IllegalArgumentException("Неизвестный формат CSV.");
            var result = "google".equals(format) ? surveys.importGoogleCsv(input, java.time.ZoneId.of(timezone)) : surveys.importCsv(input);
            redirect.addFlashAttribute("message", siteText.text("admin.success", result.inserted(),
                    result.updated(), result.unchanged()));
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", siteText.text("admin.error.validation"));
            redirect.addFlashAttribute("errorDetail", ex.getMessage());
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", siteText.text("admin.error.failed"));
        }
        return "redirect:/admin";
    }
}
