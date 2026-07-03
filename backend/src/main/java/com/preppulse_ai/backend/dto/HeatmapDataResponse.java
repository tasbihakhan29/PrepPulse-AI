package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDataResponse {
    
    private LocalDate date;
    private Integer activityLevel; // 0-5 based on activity intensity
    private Integer testsAttempted;
    private Integer questionsSolved;
    private Integer evaluationsCompleted;
}
