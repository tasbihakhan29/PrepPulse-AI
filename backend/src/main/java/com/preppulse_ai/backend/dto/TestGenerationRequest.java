package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestGenerationRequest {
    private UUID sourceMaterialId;
    private String examType;
    private String questionType;
    private String difficulty;
    private Integer questionCount;
    private String markingScheme;
    private String topicFocus;
}
