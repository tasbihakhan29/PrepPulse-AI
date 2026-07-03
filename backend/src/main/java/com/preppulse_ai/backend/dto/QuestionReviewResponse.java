package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewResponse {
    
    private UUID questionId;
    private Integer questionNumber;
    private String question;
    private List<String> options;
    private String correctAnswer;
    private String userAnswer;
    private String questionType;
    private String status; // CORRECT, WRONG, SKIPPED
    private Double marksObtained;
    private String explanation;
    private String topic;
    private String difficulty;
}
