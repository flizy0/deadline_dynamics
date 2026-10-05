package ru.deadline.lab.service;

import ru.deadline.lab.model.SurveyResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DemoData {
    private static final List<SurveyResponse> ROWS = create();

    private DemoData() {
    }

    public static List<SurveyResponse> responses() {
        return ROWS;
    }

    private static List<SurveyResponse> create() {
        Random random = new Random(20260928);
        String[] assignments = {"CODE", "CALCULATIONS", "TEXT", "PRESENTATION", "LAB", "MIXED"};
        String[] allotted = {"TWO", "THREE_FOUR", "FIVE_SEVEN", "ONE", "SAME_DAY", "EIGHT_PLUS"};
        String[] starts = {"SAME_DAY", "ONE", "TWO", "THREE_FOUR", "FIVE_SEVEN", "EIGHT_PLUS"};
        String[] planning = {"WRITTEN", "MENTAL", "NONE"};
        String[] workload = {"ZERO", "ONE", "TWO", "THREE", "FOUR_PLUS", "UNKNOWN"};
        List<SurveyResponse> rows = new ArrayList<>();
        Instant epoch = Instant.parse("2026-09-01T08:00:00Z");
        for (int i = 0; i < 144; i++) {
            String allottedBand = allotted[i % allotted.length];
            int latestAllowedIndex = switch (allottedBand) {
                case "SAME_DAY" -> 0;
                case "ONE" -> 1;
                case "TWO" -> 2;
                case "THREE_FOUR" -> 3;
                case "FIVE_SEVEN" -> 4;
                default -> 5;
            };
            String start = starts[random.nextInt(latestAllowedIndex + 1)];
            String status = random.nextDouble() < ("SAME_DAY".equals(start) ? 0.48 : 0.23) ? "LATE" : "ON_TIME";
            if (i % 17 == 0) {
                start = "NOT_STARTED";
                status = "NOT_SUBMITTED";
            } else if (i % 19 == 0) {
                start = "AFTER";
                status = "LATE";
            } else if (i % 23 == 0) {
                status = "UNKNOWN";
            }
            rows.add(new SurveyResponse("demo-" + (i + 1), epoch.plusSeconds(i * 2900L), true,
                    assignments[random.nextInt(assignments.length)], allottedBand, start, status,
                    i % 11 == 0 ? "YES" : "NO", planning[random.nextInt(planning.length)],
                    1 + random.nextInt(5), workload[random.nextInt(workload.length)]));
        }
        return List.copyOf(rows);
    }
}
