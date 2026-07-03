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
public class FlashcardHistoryResponse {
    
    private UUID id;
    private String question;
    private String answer;
    private String topic;
    private OffsetDateTime createdAt;
}
