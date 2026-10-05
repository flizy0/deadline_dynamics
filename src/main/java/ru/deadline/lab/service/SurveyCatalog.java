package ru.deadline.lab.service;

import ru.deadline.lab.model.SurveyResponse;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One dictionary for the form, imported answers, tables and chart labels. */
public final class SurveyCatalog {
    public static final String GOOGLE_FORM = "https://docs.google.com/forms/d/e/1FAIpQLSdqIg5NleKUHx8wAzEpLAQ-f8V7Taui6vn56bHmvWKAeX6hYg/viewform";
    private static final String UNKNOWN = "Есімде жоқ / Не помню / I do not remember";
    private static final String NOT_STARTED = "Әлі бастаған жоқпын / Ещё не начал(а) / I have not started yet";
    private static final String YES = "Иә / Да / Yes";
    private static final String NO = "Жоқ / Нет / No";
    private static final List<Option> DAYS = options(
            "SAME_DAY", "Сол күні / В тот же день / On the due date",
            "ONE", "1 күн бұрын / За 1 день / 1 day before",
            "TWO", "2 күн бұрын / За 2 дня / 2 days before",
            "THREE_FOUR", "3–4 күн бұрын / За 3–4 дня / 3–4 days before",
            "FIVE_SEVEN", "5–7 күн бұрын / За 5–7 дней / 5–7 days before",
            "EIGHT_PLUS", "8 немесе одан көп күн бұрын / За 8 и более дней / 8 or more days before",
            "UNKNOWN", UNKNOWN);
    private static final List<Question> QUESTIONS = List.of(
            new Question("eligible", "Участие", "Соңғы 14 күнде бастапқы тапсыру мерзімі өтіп кеткен жеке оқу тапсырмаңыз болды ма? / Было ли у вас индивидуальное задание, первоначальный дедлайн которого прошёл за последние 14 дней? / Have you had an individual assignment whose original deadline passed within the last 14 days?", options("YES", YES, "NO", NO)),
            new Question("allottedBand", "Выданный срок", "Тапсырма бастапқы дедлайннан қанша күн бұрын берілді? / За сколько календарных дней до первоначального дедлайна выдали задание? / How many calendar days before the original deadline was the assignment given?", DAYS),
            new Question("startBand", "Начало работы", "Тапсырманы бастапқы дедлайннан қанша күн бұрын орындай бастадыңыз? / За сколько календарных дней до первоначального дедлайна вы начали выполнять задание? / How many calendar days before the original deadline did you start working on the assignment?", startOptions()),
            new Question("submissionStatus", "Результат сдачи", "Бастапқы дедлайнға қатысты тапсырманы қашан тапсырдыңыз? / Когда вы сдали задание относительно первоначального дедлайна? / When did you submit the assignment relative to the original deadline?", options(
                    "ON_TIME", "Белгіленген уақыттан кешіктірмей / Не позже установленного времени / By the original deadline",
                    "LATE", "Белгіленген уақыттан кейін / Позже установленного времени / After the original deadline",
                    "NOT_SUBMITTED", "Әлі тапсырған жоқпын / Пока не сдал(а) / I have not submitted it yet", "UNKNOWN", UNKNOWN)),
            new Question("extensionStatus", "Перенос срока", "Оқытушының рұқсатымен сіз үшін тапсыру мерзімі өзгертілді ме? / Переносили ли вам дедлайн с разрешения преподавателя? / Was your deadline changed with the instructor's permission?", options("NO", NO, "YES", YES,
                    "UNKNOWN", "Білмеймін немесе есімде жоқ / Не знаю или не помню / I do not know or remember")),
            new Question("planning", "Планирование", "Жұмысты бастамас бұрын орындау жоспарыңыз болды ма? / Был ли у вас план выполнения до начала работы? / Did you have a plan before you started working?", options(
                    "WRITTEN", "Жазылған жоспар болды / Был записанный план / I had a written plan",
                    "MENTAL", "Ойымда шамамен жоспар болды / Был примерный план в голове / I had a rough plan in mind",
                    "NONE", "Жоспар болған жоқ / Плана не было / I had no plan", "NOT_STARTED", NOT_STARTED, "UNKNOWN", UNKNOWN)),
            new Question("difficulty", "Сложность", "Тапсырманы алған кезде ол қаншалықты күрделі болып көрінді? / Насколько сложным казалось задание, когда вы его получили? / How difficult did the assignment seem when you received it?", options(
                    "1", "Өте жеңіл / Очень простым / Very easy", "2", "Біршама жеңіл / Скорее простым / Rather easy",
                    "3", "Орташа / Средней сложности / Moderately difficult", "4", "Біршама күрделі / Скорее сложным / Rather difficult",
                    "5", "Өте күрделі / Очень сложным / Very difficult", "UNKNOWN", "Бағалай алмаймын / Не могу оценить / I cannot assess it")),
            new Question("otherDeadlines", "Другие дедлайны", "Осы тапсырмамен бір күнтізбелік аптада тағы қанша тапсырма тапсыру керек болды? / Сколько других заданий нужно было сдать на той же календарной неделе? / How many other assignments were due in the same calendar week?", options(
                    "ZERO", "0", "ONE", "1", "TWO", "2", "THREE", "3", "FOUR_PLUS", "4 немесе одан көп / 4 и больше / 4 or more", "UNKNOWN", UNKNOWN)));

    private SurveyCatalog() { }
    public static List<Question> questions() { return QUESTIONS; }
    public static Question question(String field) {
        return QUESTIONS.stream().filter(q -> q.field().equals(field)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный вопрос: " + field));
    }
    public static Map<String, String> labels(String field) {
        Map<String, String> labels = new LinkedHashMap<>();
        question(field).options().forEach(option -> labels.put(option.code(), option.ru()));
        return Collections.unmodifiableMap(labels);
    }
    public static String value(SurveyResponse row, String field) {
        return switch (field) {
            case "eligible" -> row.eligible() ? "YES" : "NO";
            case "allottedBand" -> row.allottedBand();
            case "startBand" -> row.startBand();
            case "submissionStatus" -> row.submissionStatus();
            case "extensionStatus" -> row.extensionStatus();
            case "planning" -> row.planning();
            case "difficulty" -> row.difficulty() == null ? "UNKNOWN" : row.difficulty().toString();
            case "otherDeadlines" -> row.otherDeadlines();
            default -> throw new IllegalArgumentException("Неизвестное поле");
        };
    }
    public static String decode(String field, String text) {
        String normalized = normalize(text);
        for (Option option : question(field).options()) {
            if (normalized.equals(normalize(option.code())) || normalized.equals(normalize(option.label()))
                    || Arrays.stream(option.label().split(" / ")).anyMatch(part -> normalize(part).equals(normalized))) {
                return option.code();
            }
            if (field.equals("difficulty") && !option.code().equals("UNKNOWN")
                    && normalized.equals(normalize(option.code() + ". " + option.label()))) return option.code();
        }
        throw new IllegalArgumentException("Неизвестный вариант ответа: " + question(field).shortTitle());
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .replace('\u00a0', ' ').replace('–', '-').replace('—', '-').replace('ё', 'е')
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
    private static List<Option> startOptions() {
        var result = new java.util.ArrayList<>(DAYS);
        result.set(0, new Option("SAME_DAY", "Дедлайн күні, белгіленген уақытқа дейін / В день дедлайна, до установленного времени / On the due date, before the deadline"));
        result.add(result.size() - 1, new Option("AFTER", "Бастапқы дедлайн өткеннен кейін / После первоначального дедлайна / After the original deadline"));
        result.add(result.size() - 1, new Option("NOT_STARTED", NOT_STARTED));
        return List.copyOf(result);
    }
    private static List<Option> options(String... pairs) {
        var result = new java.util.ArrayList<Option>();
        for (int i = 0; i < pairs.length; i += 2) result.add(new Option(pairs[i], pairs[i + 1]));
        return List.copyOf(result);
    }
    public record Option(String code, String label) {
        public String ru() { String[] parts = label.split(" / "); return parts.length == 3 ? parts[1] : label; }
    }
    public record Question(String field, String shortTitle, String title, List<Option> options) { }
}
