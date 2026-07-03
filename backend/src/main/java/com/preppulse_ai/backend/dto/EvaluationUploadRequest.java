package com.preppulse_ai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationUploadRequest {
    
    @NotBlank(message = "Question is required")
    private String question;
    
    private String topic;
    
    @NotNull(message = "Marks limit is required")
    private Double marksLimit;
}
