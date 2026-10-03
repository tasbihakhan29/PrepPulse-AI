package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    
    private String recommendedTopic;
    private String suggestedDifficulty;
    private String suggestedQuestionType;
    private Integer recommendedDailyGoal;
}
