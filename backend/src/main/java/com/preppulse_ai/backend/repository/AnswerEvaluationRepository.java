package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, UUID> {
    Optional<AnswerEvaluation> findBySubmissionId(UUID submissionId);

    @Query("SELECT ae FROM AnswerEvaluation ae WHERE ae.submission.user.id = :userId ORDER BY ae.createdAt ASC")
    List<AnswerEvaluation> findByUserIdOrderByCreatedAtAsc(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT ae.submission.id) FROM AnswerEvaluation ae WHERE ae.submission.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
