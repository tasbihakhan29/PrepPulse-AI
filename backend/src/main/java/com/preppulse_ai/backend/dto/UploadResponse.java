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
public class UploadResponse {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private Integer academicConfidenceScore;
    private String extractedText;
}
