package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {
    
    private UUID attemptId;
    private UUID testId;
    private String examType;
    private Double score;
    private Double percentage;
    private Integer timeTaken;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer skipped;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer tabSwitchCount;
    private Map<String, Double> topicScores;
    private List<QuestionReviewResponse> questionReviews;
}
