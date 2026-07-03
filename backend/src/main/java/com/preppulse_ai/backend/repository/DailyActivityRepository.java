package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, UUID> {
    
    List<DailyActivity> findByUserIdOrderByActivityDateDesc(UUID userId);
    
    List<DailyActivity> findByUserIdOrderByActivityDateAsc(UUID userId);
    
    Optional<DailyActivity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);
    
    @Query("SELECT da FROM DailyActivity da WHERE da.user.id = :userId AND da.activityDate >= :startDate ORDER BY da.activityDate ASC")
    List<DailyActivity> findByUserIdAndActivityDateAfterOrderByActivityDateAsc(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate
    );
    
    @Query("SELECT COUNT(da) FROM DailyActivity da WHERE da.user.id = :userId AND (da.testsAttempted > 0 OR da.questionsAttempted > 0 OR da.evaluationsCompleted > 0)")
    Long countActiveDaysByUserId(@Param("userId") UUID userId);
}
