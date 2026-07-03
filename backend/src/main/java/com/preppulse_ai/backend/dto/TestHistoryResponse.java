package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestHistoryResponse {
    
    private UUID testId;
    private UUID attemptId;
    private String examType;
    private String questionType;
    private String difficulty;
    private Double score;
    private Double percentage;
    private Integer timeTaken;
    private OffsetDateTime date;
    private Integer totalQuestions;
    private Integer correctQuestions;
}
