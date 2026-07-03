package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.security.AuthenticatedUserResolver;
import com.preppulse_ai.backend.service.EvaluatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/evaluator")
@RequiredArgsConstructor
public class EvaluatorController {

    private final EvaluatorService evaluatorService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping("/upload")
    public ResponseEntity<EvaluationUploadResponse> uploadAnswer(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam("marksLimit") Double marksLimit
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        EvaluationUploadRequest request = EvaluationUploadRequest.builder()
                .question(question)
                .topic(topic)
                .marksLimit(marksLimit)
                .build();

        try {
            EvaluationUploadResponse response = evaluatorService.uploadAnswer(file, request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload answer: " + e.getMessage(), e);
        }
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluateAnswer(
            Principal principal,
            @RequestParam("submissionId") String submissionId
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        EvaluationResponse response = evaluatorService.evaluateAnswer(
                java.util.UUID.fromString(submissionId),
                user
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<EvaluationHistoryResponse>> getEvaluationHistory(
            Principal principal,
            @RequestParam(value = "search", required = false) String searchQuery,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        Page<EvaluationHistoryResponse> response = evaluatorService.getEvaluationHistory(
                user,
                searchQuery,
                page,
                size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationResponse> getEvaluationById(
            Principal principal,
            @PathVariable("id") String id
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        EvaluationResponse response = evaluatorService.getEvaluationById(
                java.util.UUID.fromString(id),
                user
        );
        return ResponseEntity.ok(response);
    }
}
