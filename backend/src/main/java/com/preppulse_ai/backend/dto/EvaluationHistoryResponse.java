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
public class EvaluationHistoryResponse {
    private UUID id;
    private UUID submissionId;
    private UUID evaluationId;
    private String question;
    private String topic;
    private Double marksLimit;
    private Double score;
    private Double marksObtained;
    private Double maxMarks;
    private String evaluationStatus;
    private String fileUrl;
    private String fileType;
    private OffsetDateTime createdAt;
}
