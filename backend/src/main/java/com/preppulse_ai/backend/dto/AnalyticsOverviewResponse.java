package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    
    private Integer totalTestsTaken;
    private Integer totalEvaluations;
    private Double averageScore;
    private Double bestScore;
    private Integer currentStudyStreak;
    private Integer longestStudyStreak;
    private Integer hoursStudied;
    private Integer totalQuestionsAttempted;
}
