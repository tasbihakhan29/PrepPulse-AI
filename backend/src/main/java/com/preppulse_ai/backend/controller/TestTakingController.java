package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.security.AuthenticatedUserResolver;
import com.preppulse_ai.backend.service.TestTakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestTakingController {

    private final TestTakingService testTakingService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping("/start")
    public ResponseEntity<StartTestResponse> startTest(
            @Valid @RequestBody StartTestRequest request,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        StartTestResponse response = testTakingService.startTest(request.getTestId(), user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<SaveAnswerResponse> saveAnswer(
            @Valid @RequestBody SaveAnswerRequest request,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        SaveAnswerResponse response = testTakingService.saveAnswer(request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<TestResultResponse> submitTest(
            @Valid @RequestBody SubmitTestRequest request,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        TestResultResponse response = testTakingService.submitTest(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{testId}")
    public ResponseEntity<TestDto> getTest(
            @PathVariable UUID testId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        TestDto response = testTakingService.getTest(testId, user);
        return ResponseEntity.ok(response);
    }
}
