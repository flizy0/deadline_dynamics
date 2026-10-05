package ru.deadline.lab.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.deadline.lab.model.ImportResult;
import ru.deadline.lab.model.SurveyEntity;
import ru.deadline.lab.model.SurveyResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SurveyRepository {
    private final SurveyJpaRepository jpa;
    private final EntityManager entityManager;

    public SurveyRepository(SurveyJpaRepository jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }
    public List<SurveyResponse> findEligible() {
        return jpa.findByEligibleTrueOrderBySubmittedAtAscExternalIdAsc().stream().map(SurveyEntity::toResponse).toList();
    }
    public long countEligible() { return jpa.countByEligibleTrue(); }
    public long countAll() { return jpa.count(); }
    public Instant earliestSubmission() { return jpa.earliestSubmission(); }
    public Instant latestSubmission() { return jpa.latestSubmission(); }

    @Transactional
    public ImportResult importValidated(List<SurveyResponse> rows) {
        // The transaction lock prevents concurrent imports from counting the same new response twice.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(5812173401)").getSingleResult();
        Map<String, SurveyResponse> existing = new HashMap<>();
        jpa.findAllById(rows.stream().map(SurveyResponse::externalId).toList())
                .forEach(entity -> existing.put(entity.toResponse().externalId(), entity.toResponse()));
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        for (SurveyResponse row : rows) {
            SurveyResponse previous = existing.get(row.externalId());
            if (row.equals(previous)) {
                unchanged++;
            } else {
                jpa.save(new SurveyEntity(row));
                if (previous == null) inserted++; else updated++;
            }
        }
        jpa.flush();
        return new ImportResult(inserted, updated, unchanged, rows.size());
    }
}
