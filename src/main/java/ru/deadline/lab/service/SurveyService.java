package ru.deadline.lab.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Service;
import ru.deadline.lab.model.ImportResult;
import ru.deadline.lab.model.SurveyResponse;
import ru.deadline.lab.repository.SurveyRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SurveyService {
    private static final int MAX_ROWS = 5000;
    private static final int MAX_CSV_BYTES = 5 * 1024 * 1024;
    private static final List<String> HEADERS = List.of("externalId", "submittedAt", "eligible", "assignmentType",
            "allottedBand", "startBand", "submissionStatus", "extensionStatus", "planning", "difficulty", "otherDeadlines");
    private static final Set<String> ASSIGNMENTS = Set.of("CODE", "CALCULATIONS", "TEXT", "PRESENTATION", "LAB", "MIXED", "OTHER", "UNKNOWN");
    private static final Set<String> ALLOTTED = Set.of("SAME_DAY", "ONE", "TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS", "UNKNOWN");
    private static final Set<String> STARTS = Set.of("SAME_DAY", "ONE", "TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS", "AFTER", "NOT_STARTED", "UNKNOWN");
    private static final Set<String> STATUSES = Set.of("ON_TIME", "LATE", "NOT_SUBMITTED", "UNKNOWN");
    private static final Set<String> EXTENSIONS = Set.of("NO", "YES", "UNKNOWN");
    private static final Set<String> PLANS = Set.of("WRITTEN", "MENTAL", "NONE", "NOT_STARTED", "UNKNOWN");
    private static final Set<String> WORKLOADS = Set.of("ZERO", "ONE", "TWO", "THREE", "FOUR_PLUS", "UNKNOWN");
    private static final Map<String, Integer> MIN_DAYS = Map.of("SAME_DAY", 0, "ONE", 1, "TWO", 2,
            "THREE_FOUR", 3, "FIVE_SEVEN", 5, "EIGHT_PLUS", 8);
    private static final Map<String, Integer> MAX_DAYS = Map.of("SAME_DAY", 0, "ONE", 1, "TWO", 2,
            "THREE_FOUR", 4, "FIVE_SEVEN", 7, "EIGHT_PLUS", Integer.MAX_VALUE);

    private final SurveyRepository repository;

    public SurveyService(SurveyRepository repository) {
        this.repository = repository;
    }

    public List<SurveyResponse> responses(String dataset) {
        if ("demo".equals(dataset)) {
            return DemoData.responses();
        }
        if ("real".equals(dataset)) {
            return repository.findEligible();
        }
        throw new IllegalArgumentException("Выберите реальные или демонстрационные данные.");
    }

    public long realCount() {
        return repository.countEligible();
    }

    public long allCount() { return repository.countAll(); }

    public Instant collectionStart(String dataset) {
        return "real".equals(dataset) ? repository.earliestSubmission()
                : responses(dataset).stream().map(SurveyResponse::submittedAt).min(Instant::compareTo).orElse(null);
    }

    public Instant collectionEnd(String dataset) {
        return "real".equals(dataset) ? repository.latestSubmission()
                : responses(dataset).stream().map(SurveyResponse::submittedAt).max(Instant::compareTo).orElse(null);
    }

    public ImportResult importGoogleCsv(InputStream input, java.time.ZoneId zone) {
        // Imported self-reports retain cross-field inconsistencies instead of rewriting or dropping source answers.
        return importRows(GoogleCsvReader.read(input, zone), false);
    }

    public ImportResult submit(Map<String, String> fields, String id, Instant time) {
        if (!Set.of("YES", "NO").contains(fields.getOrDefault("eligible", ""))) {
            throw new IllegalArgumentException("Ответьте на первый вопрос.");
        }
        boolean eligible = "YES".equals(fields.get("eligible"));
        Map<String, String> codes = new java.util.HashMap<>();
        for (var question : SurveyCatalog.questions()) {
            if (question.field().equals("eligible")) continue;
            String value = eligible ? fields.get(question.field()) : "UNKNOWN";
            if (value == null || !SurveyCatalog.labels(question.field()).containsKey(value)) {
                throw new IllegalArgumentException("Выберите ответ: " + question.shortTitle());
            }
            codes.put(question.field(), value);
        }
        Integer difficulty = "UNKNOWN".equals(codes.get("difficulty")) ? null : Integer.valueOf(codes.get("difficulty"));
        var row = new SurveyResponse(id, time, eligible, "UNKNOWN", codes.get("allottedBand"), codes.get("startBand"),
                codes.get("submissionStatus"), codes.get("extensionStatus"), codes.get("planning"), difficulty,
                codes.get("otherDeadlines"));
        return importRows(List.of(row));
    }

    public ImportResult importRows(List<SurveyResponse> rows) {
        return importRows(rows, true);
    }

    private ImportResult importRows(List<SurveyResponse> rows, boolean requireConsistency) {
        if (rows == null || rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("Одна загрузка должна содержать не более 5000 ответов.");
        }
        List<SurveyResponse> normalized = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            SurveyResponse row = normalize(rows.get(i));
            List<String> errors = validate(row, requireConsistency);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Ответ " + (i + 1) + ": " + String.join(" ", errors));
            }
            if (!ids.add(row.externalId())) {
                throw new IllegalArgumentException("Ответ " + (i + 1) + ": повторяется externalId внутри одной загрузки.");
            }
            normalized.add(row);
        }
        // The repository opens its transaction only after every response in the batch has passed validation.
        return repository.importValidated(List.copyOf(normalized));
    }

    public ImportResult importCsv(InputStream input) {
        if (input == null) {
            throw new IllegalArgumentException("Выберите CSV-файл.");
        }
        try {
            byte[] bytes = input.readNBytes(MAX_CSV_BYTES + 1);
            if (bytes.length > MAX_CSV_BYTES) {
                throw new IllegalArgumentException("Размер CSV должен быть не более 5 МБ.");
            }
            int offset = bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF ? 3 : 0;
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).setTrim(true).get();
            List<SurveyResponse> rows = new ArrayList<>();
            try (var reader = new InputStreamReader(new ByteArrayInputStream(bytes, offset, bytes.length - offset), decoder);
                 CSVParser parser = format.parse(reader)) {
                if (!parser.getHeaderNames().equals(HEADERS)) {
                    throw new IllegalArgumentException("Столбцы CSV должны точно совпадать с шаблоном, включая порядок.");
                }
                for (CSVRecord record : parser) {
                    if (rows.size() == MAX_ROWS) {
                        throw new IllegalArgumentException("Одна загрузка должна содержать не более 5000 ответов.");
                    }
                    if (record.size() != HEADERS.size()) {
                        throw new IllegalArgumentException("Строка CSV " + (record.getRecordNumber() + 1) + ": неверное число столбцов.");
                    }
                    rows.add(parseRecord(record));
                }
            }
            return importRows(rows);
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalArgumentException("Не удалось прочитать CSV. Проверьте UTF-8, запятые и кавычки.", exception);
        }
    }

    public String csvTemplate() {
        return String.join(",", HEADERS) + "\n"
                + "example-001,2026-09-01T10:00:00Z,true,CODE,THREE_FOUR,ONE,ON_TIME,NO,MENTAL,3,TWO\n"
                + "example-002,2026-09-01T11:00:00Z,true,LAB,FIVE_SEVEN,SAME_DAY,LATE,NO,NONE,4,THREE\n";
    }

    public List<String> validate(SurveyResponse original) {
        return validate(original, true);
    }

    private List<String> validate(SurveyResponse original, boolean requireConsistency) {
        if (original == null) {
            return List.of("Ответ не может быть пустым.");
        }
        SurveyResponse row = normalize(original);
        List<String> errors = new ArrayList<>();
        if (row.externalId() == null || !row.externalId().matches("[A-Za-z0-9_.:-]{1,200}")) {
            errors.add("externalId должен содержать 1–200 латинских букв, цифр или символов _ . : -.");
        }
        if (row.submittedAt() == null || row.submittedAt().isBefore(Instant.parse("2000-01-01T00:00:00Z"))
                || row.submittedAt().isAfter(Instant.parse("2100-01-01T00:00:00Z"))) {
            errors.add("submittedAt должен быть датой ISO-8601 между 2000 и 2100 годами.");
        }
        checkCode(errors, "assignmentType", row.assignmentType(), ASSIGNMENTS);
        checkCode(errors, "allottedBand", row.allottedBand(), ALLOTTED);
        checkCode(errors, "startBand", row.startBand(), STARTS);
        checkCode(errors, "submissionStatus", row.submissionStatus(), STATUSES);
        checkCode(errors, "extensionStatus", row.extensionStatus(), EXTENSIONS);
        checkCode(errors, "planning", row.planning(), PLANS);
        checkCode(errors, "otherDeadlines", row.otherDeadlines(), WORKLOADS);
        if (row.difficulty() != null && (row.difficulty() < 1 || row.difficulty() > 5)) {
            errors.add("difficulty должен быть от 1 до 5 или пустым.");
        }
        if (row.eligible() && requireConsistency) {
            Integer startMinimum = row.startBand() == null ? null : MIN_DAYS.get(row.startBand());
            Integer allottedMaximum = row.allottedBand() == null ? null : MAX_DAYS.get(row.allottedBand());
            if (startMinimum != null && allottedMaximum != null && startMinimum > allottedMaximum) {
                errors.add("Начало работы не может быть раньше выдачи задания. Проверьте allottedBand и startBand.");
            }
            if ("NOT_STARTED".equals(row.startBand()) && ("ON_TIME".equals(row.submissionStatus()) || "LATE".equals(row.submissionStatus()))) {
                errors.add("Задание нельзя сдать, если работа ещё не начата.");
            }
            if ("AFTER".equals(row.startBand()) && "ON_TIME".equals(row.submissionStatus())) {
                errors.add("Начало после первоначального дедлайна несовместимо со сдачей вовремя.");
            }
            if ("NOT_STARTED".equals(row.planning()) && (startMinimum != null || "AFTER".equals(row.startBand())
                    || "ON_TIME".equals(row.submissionStatus()) || "LATE".equals(row.submissionStatus()))) {
                errors.add("Ответ «ещё не начал» в планировании противоречит указанному началу работы или сдаче.");
            }
        }
        return List.copyOf(errors);
    }

    private static void checkCode(List<String> errors, String field, String value, Set<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            errors.add("Недопустимый код в поле " + field + ".");
        }
    }

    private static SurveyResponse normalize(SurveyResponse row) {
        if (row == null) {
            return null;
        }
        return new SurveyResponse(row.externalId(), row.submittedAt() == null ? null : row.submittedAt().truncatedTo(ChronoUnit.MICROS),
                row.eligible(), code(row.assignmentType(), row.eligible()), code(row.allottedBand(), row.eligible()),
                code(row.startBand(), row.eligible()), code(row.submissionStatus(), row.eligible()),
                code(row.extensionStatus(), row.eligible()), code(row.planning(), row.eligible()), row.difficulty(),
                code(row.otherDeadlines(), row.eligible()));
    }

    private static String code(String value, boolean eligible) {
        return !eligible && (value == null || value.isBlank()) ? "UNKNOWN" : value;
    }

    private static SurveyResponse parseRecord(CSVRecord record) {
        long line = record.getRecordNumber() + 1;
        String eligible = record.get("eligible");
        if (!"true".equals(eligible) && !"false".equals(eligible)) {
            throw new IllegalArgumentException("Строка CSV " + line + ": eligible должен быть true или false.");
        }
        Instant submittedAt;
        Integer difficulty;
        try {
            submittedAt = Instant.parse(record.get("submittedAt"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Строка CSV " + line + ": неверная дата submittedAt. Используйте ISO-8601 с часовым поясом.");
        }
        try {
            difficulty = record.get("difficulty").isBlank() ? null : Integer.valueOf(record.get("difficulty"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Строка CSV " + line + ": difficulty должен быть целым числом или пустым.");
        }
        return new SurveyResponse(record.get("externalId"), submittedAt, Boolean.parseBoolean(eligible),
                record.get("assignmentType"), record.get("allottedBand"), record.get("startBand"),
                record.get("submissionStatus"), record.get("extensionStatus"), record.get("planning"), difficulty,
                record.get("otherDeadlines"));
    }
}
