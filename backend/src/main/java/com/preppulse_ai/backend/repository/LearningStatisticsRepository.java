package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.LearningStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningStatisticsRepository extends JpaRepository<LearningStatistics, UUID> {
    
    List<LearningStatistics> findByUserId(UUID userId);
    
    Optional<LearningStatistics> findByUserIdAndTopic(UUID userId, String topic);
    
    @Query("SELECT ls FROM LearningStatistics ls WHERE ls.user.id = :userId ORDER BY ls.averagePercentage DESC")
    List<LearningStatistics> findByUserIdOrderByAveragePercentageDesc(@Param("userId") UUID userId);
    
    @Query("SELECT ls FROM LearningStatistics ls WHERE ls.user.id = :userId AND ls.averagePercentage < 60 ORDER BY ls.averagePercentage ASC")
    List<LearningStatistics> findWeakTopicsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT ls FROM LearningStatistics ls WHERE ls.user.id = :userId AND ls.averagePercentage >= 70 ORDER BY ls.averagePercentage DESC")
    List<LearningStatistics> findStrongTopicsByUserId(@Param("userId") UUID userId);
}
