package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.TestResult;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.repository.UserRepository;
import com.preppulse_ai.backend.service.PracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@Slf4j
public class PracticeController {

    private final UserRepository userRepository;
    private final PracticeService practiceService;

    // Cache of active generation parameters
    private final Map<UUID, TestGenerationRequest> activeGenerations = new ConcurrentHashMap<>();

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new SecurityException("No authenticated principal found.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + principal.getName()));
    }

    /**
     * Uploads a study note PDF or pastes notes.
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "textContent", required = false) String textContent) throws IOException {
        
        User user = getAuthenticatedUser(principal);
        log.info("Received upload request from user: {}", user.getEmail());
        UploadResponse response = practiceService.uploadMaterial(user, file, textContent);
        return ResponseEntity.ok(response);
    }

    /**
     * Prepares parameters for test generation and registers a unique generation ID.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            Principal principal,
            @RequestBody TestGenerationRequest request) {
        
        User user = getAuthenticatedUser(principal);
        log.info("Registered quiz generation request for user: {}", user.getEmail());
        
        UUID generationId = UUID.randomUUID();
        activeGenerations.put(generationId, request);
        
        return ResponseEntity.ok(Map.of("generationId", generationId.toString()));
    }

    /**
     * Starts the Server-Sent Events stream for generating questions based on generationId.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            Principal principal,
            @RequestParam("generationId") String genIdStr) {
        
        User user = getAuthenticatedUser(principal);
        UUID generationId = UUID.fromString(genIdStr);
        
        TestGenerationRequest request = activeGenerations.remove(generationId);
        if (request == null) {
            log.error("Generation session ID not found: {}", generationId);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Generation session expired or not found."));
                emitter.complete();
            } catch (Exception e) {
                // ignore
            }
            return emitter;
        }

        log.info("Initiating SSE streaming event for session: {}", generationId);
        return practiceService.generateTestStream(user, request);
    }


    /**
     * Submits quiz results.
     */
//    @PostMapping("/submit-results")
//    public ResponseEntity<TestResult> submitResults(
//            Principal principal,
//            @RequestBody TestResultSubmitRequest request) {
//
//        User user = getAuthenticatedUser(principal);
//        log.info("Submitting test results for user: {}", user.getEmail());
//        TestResult result = practiceService.submitTestResult(user, request);
//        return ResponseEntity.ok(result);
//    }
    @PostMapping("/submit-results")
    public ResponseEntity<TestResultResponse> submitResults(
            Principal principal,
            @RequestBody TestResultSubmitRequest request) {

        User user = getAuthenticatedUser(principal);

        TestResultResponse response =
                practiceService.submitTestResult(user, request);

        return ResponseEntity.ok(response);
    }
    /**
     * Gets recommendation weak areas.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(Principal principal) {
        User user = getAuthenticatedUser(principal);
        Map<String, Object> recommendations = practiceService.getRecommendations(user);
        return ResponseEntity.ok(recommendations);
    }
}
