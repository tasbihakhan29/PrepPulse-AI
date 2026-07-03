package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.dto.QuestionReviewResponse;
import com.preppulse_ai.backend.dto.TestResultResponse;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.security.AuthenticatedUserResolver;
import com.preppulse_ai.backend.service.TestTakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final TestTakingService testTakingService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/{attemptId}")
    public ResponseEntity<TestResultResponse> getResult(
            @PathVariable UUID attemptId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        return ResponseEntity.ok(testTakingService.getTestResult(attemptId, user));
    }

    @GetMapping("/{attemptId}/review")
    public ResponseEntity<List<QuestionReviewResponse>> getReview(
            @PathVariable UUID attemptId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        return ResponseEntity.ok(testTakingService.getTestReview(attemptId, user));
    }
}
