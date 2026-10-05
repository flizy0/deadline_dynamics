package ru.deadline.lab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "survey_responses")
public class SurveyEntity {
    @Id @Column(length = 200)
    private String externalId;
    @Column(nullable = false)
    private Instant submittedAt;
    @Column(nullable = false)
    private boolean eligible;
    @Column(nullable = false, length = 20)
    private String assignmentType;
    @Column(nullable = false, length = 20)
    private String allottedBand;
    @Column(nullable = false, length = 20)
    private String startBand;
    @Column(nullable = false, length = 20)
    private String submissionStatus;
    @Column(nullable = false, length = 20)
    private String extensionStatus;
    @Column(nullable = false, length = 20)
    private String planning;
    private Integer difficulty;
    @Column(nullable = false, length = 20)
    private String otherDeadlines;

    protected SurveyEntity() { }

    public SurveyEntity(SurveyResponse row) {
        externalId = row.externalId();
        submittedAt = row.submittedAt();
        eligible = row.eligible();
        assignmentType = row.assignmentType();
        allottedBand = row.allottedBand();
        startBand = row.startBand();
        submissionStatus = row.submissionStatus();
        extensionStatus = row.extensionStatus();
        planning = row.planning();
        difficulty = row.difficulty();
        otherDeadlines = row.otherDeadlines();
    }

    public SurveyResponse toResponse() {
        return new SurveyResponse(externalId, submittedAt, eligible, assignmentType, allottedBand,
                startBand, submissionStatus, extensionStatus, planning, difficulty, otherDeadlines);
    }
}
