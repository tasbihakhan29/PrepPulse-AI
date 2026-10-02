package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.AnswerSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerSubmissionRepository extends JpaRepository<AnswerSubmission, UUID> {
    
    Page<AnswerSubmission> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT s FROM AnswerSubmission s WHERE s.user.id = :userId AND LOWER(s.question) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.createdAt DESC")
    Page<AnswerSubmission> searchByQuestion(@Param("userId") UUID userId, @Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM AnswerSubmission s WHERE s.user.id = :userId AND LOWER(s.question) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY s.createdAt DESC")
    Page<AnswerSubmission> findByUserIdAndQuestionContainingIgnoreCase(@Param("userId") UUID userId, @Param("search") String search, Pageable pageable);

    // Re-evaluation prevention query
    Optional<AnswerSubmission> findFirstByFileHashAndQuestionAndMarksLimitAndEvaluationStatus(
            String fileHash, String question, Double marksLimit, String evaluationStatus
    );
    
    @Query("SELECT COUNT(s) FROM AnswerSubmission s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    List<AnswerSubmission> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT s FROM AnswerSubmission s WHERE s.user.id = :userId " +
           "AND (:search IS NULL OR LOWER(s.question) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.topic) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:topic IS NULL OR LOWER(s.topic) = LOWER(:topic)) " +
           "AND (:status IS NULL OR s.evaluationStatus = :status)")
    Page<AnswerSubmission> findFiltered(@Param("userId") UUID userId, 
                                        @Param("search") String search, 
                                        @Param("topic") String topic, 
                                        @Param("status") String status, 
                                        Pageable pageable);

    @Query("SELECT s FROM AnswerSubmission s WHERE s.user.id = :userId " +
            "AND EXISTS (SELECT ae.id FROM AnswerEvaluation ae WHERE ae.submission.id = s.id) " +
            "AND (LOWER(s.question) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%')) OR LOWER(s.topic) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))) " +
            "AND (COALESCE(:topic, '') = '' OR LOWER(s.topic) = LOWER(:topic))")
    Page<AnswerSubmission> findCompletedFiltered(@Param("userId") UUID userId,
                                                        @Param("search") String search,
                                                        @Param("topic") String topic,
                                                        Pageable pageable);
}
