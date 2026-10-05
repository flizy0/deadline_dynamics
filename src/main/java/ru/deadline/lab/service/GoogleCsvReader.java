package ru.deadline.lab.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;
import ru.deadline.lab.model.SurveyResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class GoogleCsvReader {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private GoogleCsvReader() { }

    public static List<SurveyResponse> read(InputStream input, ZoneId zone) {
        if (input == null || zone == null) throw new IllegalArgumentException("Нужны CSV и часовой пояс таблицы.");
        try {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("CSV больше 5 МБ.");
            int offset = bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF ? 3 : 0;
            var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
            var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).setTrim(true).get();
            List<SurveyResponse> result = new ArrayList<>();
            Map<String, Integer> occurrences = new HashMap<>();
            try (var reader = new InputStreamReader(new ByteArrayInputStream(bytes, offset, bytes.length - offset), decoder);
                 var parser = format.parse(reader)) {
                Map<String, String> headers = mapHeaders(parser.getHeaderNames());
                for (var record : parser) {
                    if (record.getRecordNumber() > 5000) throw new IllegalArgumentException("Не более 5000 строк в одной загрузке.");
                    if (record.size() != parser.getHeaderNames().size()) throw new IllegalArgumentException("Неверное число столбцов в строке " + (record.getRecordNumber() + 1));
                    try {
                        Instant time = parseTime(record.get(headers.get("submittedAt")), zone);
                        boolean eligible = SurveyCatalog.decode("eligible", record.get(headers.get("eligible"))).equals("YES");
                        Map<String, String> codes = new HashMap<>();
                        for (var question : SurveyCatalog.questions()) {
                            if (question.field().equals("eligible")) continue;
                            codes.put(question.field(), eligible ? SurveyCatalog.decode(question.field(), record.get(headers.get(question.field()))) : "UNKNOWN");
                        }
                        Integer difficulty = codes.get("difficulty").equals("UNKNOWN") ? null : Integer.valueOf(codes.get("difficulty"));
                        var row = new SurveyResponse("pending", time, eligible, "UNKNOWN", codes.get("allottedBand"), codes.get("startBand"),
                                codes.get("submissionStatus"), codes.get("extensionStatus"), codes.get("planning"), difficulty, codes.get("otherDeadlines"));
                        // Preserve identical same-second respondents while keeping full-export imports repeatable.
                        String signature = "google-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                .digest(row.toString().getBytes(StandardCharsets.UTF_8)));
                        int occurrence = occurrences.merge(signature, 1, Integer::sum);
                        String id = occurrence == 1 ? signature : signature + "-" + occurrence;
                        result.add(new SurveyResponse(id, time, eligible, "UNKNOWN", row.allottedBand(), row.startBand(),
                                row.submissionStatus(), row.extensionStatus(), row.planning(), difficulty, row.otherDeadlines()));
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalArgumentException("Строка " + (record.getRecordNumber() + 1) + ": " + exception.getMessage());
                    }
                }
            }
            return List.copyOf(result);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Не удалось прочитать CSV. Нужен UTF-8, разделитель запятая и корректные кавычки.", exception);
        }
    }

    private static Map<String, String> mapHeaders(List<String> names) {
        Map<String, String> result = new HashMap<>();
        for (String name : names) {
            String normalized = SurveyCatalog.normalize(name);
            String field = null;
            if (List.of("timestamp", "отметка времени", "уақыт белгісі", "submittedat").contains(normalized)) field = "submittedAt";
            for (var question : SurveyCatalog.questions()) {
                if (normalized.equals(SurveyCatalog.normalize(question.title()))
                        || normalized.equals(SurveyCatalog.normalize(question.field()))
                        || Arrays.stream(question.title().split(" / ")).anyMatch(p -> SurveyCatalog.normalize(p).equals(normalized))) field = question.field();
            }
            if (field != null && result.put(field, name) != null) throw new IllegalArgumentException("Дублируется столбец вопроса: " + field);
        }
        List<String> required = new ArrayList<>(SurveyCatalog.questions().stream().map(SurveyCatalog.Question::field).toList());
        required.add("submittedAt");
        if (!result.keySet().containsAll(required)) throw new IllegalArgumentException("Не найдены столбцы анкеты или отметка времени. Загрузите CSV именно этой Google Form.");
        return result;
    }

    public static Instant parseTime(String text, ZoneId zone) {
        try { return Instant.parse(text); } catch (DateTimeParseException ignored) { }
        for (String pattern : List.of("d.M.uuuu H:mm:ss", "M/d/uuuu H:mm:ss", "uuuu-MM-dd H:mm:ss", "uuuu/MM/dd H:mm:ss", "uuuu-MM-dd'T'HH:mm:ss")) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)).atZone(zone).toInstant();
            } catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Неизвестный формат времени. Поддерживаются ISO-8601, дд.ММ.гггг и M/d/yyyy с часами, минутами и секундами.");
    }
}
