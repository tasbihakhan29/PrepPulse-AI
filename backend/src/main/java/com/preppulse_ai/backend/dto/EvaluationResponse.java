package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    
    private UUID submissionId;
    private UUID evaluationId;
    private Double score;
    private Double maxMarks;
    private String conceptualAccuracy;
    private String technicalCorrectness;
    private String presentation;
    private String diagramFeedback;
    private List<String> improvements;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> keywordsMissing;
    private String evaluationJson;
}
