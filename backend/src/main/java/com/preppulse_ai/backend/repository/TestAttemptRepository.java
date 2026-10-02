package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.TestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {
    
    Optional<TestAttempt> findByUserIdAndTestIdAndSubmittedFalse(UUID userId, UUID testId);
    
    @Query("SELECT a FROM TestAttempt a WHERE a.user.id = :userId AND a.test.id = :testId ORDER BY a.createdAt DESC")
    List<TestAttempt> findAllByUserIdAndTestIdOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("testId") UUID testId);
    
    @Query("SELECT a FROM TestAttempt a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<TestAttempt> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    Page<TestAttempt> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM TestAttempt a WHERE a.user.id = :userId AND a.submitted = true")
    long countByUserIdAndSubmittedTrue(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(a) FROM TestAttempt a WHERE a.user.id = :userId AND a.submitted = true AND a.startTime >= :start AND a.startTime < :end")
    long countByUserIdAndSubmittedTrueAndStartTimeBetween(@Param("userId") UUID userId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT a FROM TestAttempt a JOIN a.test t WHERE a.user.id = :userId AND a.submitted = true " +
           "AND (LOWER(t.examType) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%')) OR LOWER(t.questionType) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%')) OR LOWER(t.difficulty) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%'))) " +
           "AND t.examType = COALESCE(:examType, t.examType) " +
           "AND t.questionType = COALESCE(:questionType, t.questionType) " +
           "AND t.difficulty = COALESCE(:difficulty, t.difficulty)")
    Page<TestAttempt> findFiltered(@Param("userId") UUID userId, 
                                  @Param("search") String search, 
                                  @Param("examType") String examType, 
                                  @Param("questionType") String questionType, 
                                  @Param("difficulty") String difficulty, 
                                  Pageable pageable);
}
