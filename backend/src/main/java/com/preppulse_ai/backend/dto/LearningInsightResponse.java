package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningInsightResponse {
    
    private String insight;
    private String type; // STRENGTH, WEAKNESS, IMPROVEMENT, RECOMMENDATION
    private String topic;
    private Double value; // percentage or score
}
