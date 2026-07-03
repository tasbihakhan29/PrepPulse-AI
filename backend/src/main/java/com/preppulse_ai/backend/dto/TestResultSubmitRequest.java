package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultSubmitRequest {
    private UUID testId;
    private Double score;
    private Integer correctQuestions;
    private Integer totalQuestions;
    private Map<String, Double> topicScores;
}
