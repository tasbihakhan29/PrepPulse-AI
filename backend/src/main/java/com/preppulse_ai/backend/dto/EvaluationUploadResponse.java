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
public class EvaluationUploadResponse {
    
    private UUID submissionId;
    private String fileUrl;
    private String fileType;
    private String originalFileName;
    private Long fileSize;
    private String evaluationStatus;
}
