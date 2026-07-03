package com.preppulse_ai.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAnswerRequest {
    
    @NotNull
    private UUID attemptId;
    
    @NotNull
    private UUID questionId;
    
    private String selectedAnswer;
}
