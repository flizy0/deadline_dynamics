package ru.deadline.lab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.deadline.lab.model.SurveyEntity;
import java.time.Instant;
import java.util.List;

public interface SurveyJpaRepository extends JpaRepository<SurveyEntity, String> {
    List<SurveyEntity> findByEligibleTrueOrderBySubmittedAtAscExternalIdAsc();
    long countByEligibleTrue();

    @Query("select min(response.submittedAt) from SurveyEntity response")
    Instant earliestSubmission();

    @Query("select max(response.submittedAt) from SurveyEntity response")
    Instant latestSubmission();
}
