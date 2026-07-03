package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicPerformanceResponse {
    
    private String topic;
    private Double averageScore;
    private Integer questionsAttempted;
    private Double accuracy;
    private String status; // STRONG, WEAK, AVERAGE
}
