package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    List<TestResult> findAllByUserIdOrderByCompletedAtDesc(UUID userId);
    
    @Query("SELECT tr FROM TestResult tr WHERE tr.user.id = :userId AND tr.test.id = :testId")
    List<TestResult> findByTestId(@Param("userId") UUID userId, @Param("testId") UUID testId);
    
    @Query("SELECT tr FROM TestResult tr WHERE tr.user.id = :userId AND tr.completedAt >= :start AND tr.completedAt < :end")
    List<TestResult> findByUserIdAndCompletedAtBetween(@Param("userId") UUID userId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
