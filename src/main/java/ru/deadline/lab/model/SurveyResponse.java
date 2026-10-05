package ru.deadline.lab.model;

import java.time.Instant;

public record SurveyResponse(
        String externalId,
        Instant submittedAt,
        boolean eligible,
        String assignmentType,
        String allottedBand,
        String startBand,
        String submissionStatus,
        String extensionStatus,
        String planning,
        Integer difficulty,
        String otherDeadlines) {
}
