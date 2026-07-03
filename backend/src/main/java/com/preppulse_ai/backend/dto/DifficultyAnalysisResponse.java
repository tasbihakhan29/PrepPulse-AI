package com.preppulse_ai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyAnalysisResponse {
    
    private String difficulty; // Easy, Medium, Hard, Mixed
    private Integer count;
    private Double averageScore;
    private Double accuracy;
}
