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
public class StartTestResponse {
    
    private UUID attemptId;
    private UUID testId;
    private String examType;
    private String questionType;
    private String difficulty;
    private Integer totalQuestions;
    private OffsetDateTime startTime;
    private List<QuestionDto> questions;
    private Map<UUID, String> savedAnswers;
    private Integer duration; // in seconds
}
