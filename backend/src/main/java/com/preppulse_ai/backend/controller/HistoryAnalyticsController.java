package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.security.AuthenticatedUserResolver;
import com.preppulse_ai.backend.service.HistoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryAnalyticsController {

    private final HistoryAnalyticsService historyAnalyticsService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsOverviewResponse> getAnalyticsOverview(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        AnalyticsOverviewResponse response = historyAnalyticsService.getAnalyticsOverview(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/performance-chart")
    public ResponseEntity<List<PerformanceChartData>> getPerformanceChartData(
            @RequestParam(defaultValue = "30") int days,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        List<PerformanceChartData> response = historyAnalyticsService.getPerformanceChartData(user.getId(), days);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topic-performance")
    public ResponseEntity<List<TopicPerformanceResponse>> getTopicPerformance(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        List<TopicPerformanceResponse> response = historyAnalyticsService.getTopicPerformance(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapDataResponse>> getHeatmapData(
            @RequestParam(defaultValue = "365") int days,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        List<HeatmapDataResponse> response = historyAnalyticsService.getHeatmapData(user.getId(), days);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/question-type-distribution")
    public ResponseEntity<List<QuestionTypeDistributionResponse>> getQuestionTypeDistribution(
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        List<QuestionTypeDistributionResponse> response =
                historyAnalyticsService.getQuestionTypeDistribution(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/difficulty-analysis")
    public ResponseEntity<List<DifficultyAnalysisResponse>> getDifficultyAnalysis(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        List<DifficultyAnalysisResponse> response = historyAnalyticsService.getDifficultyAnalysis(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights")
    public ResponseEntity<List<LearningInsightResponse>> getLearningInsights(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        List<LearningInsightResponse> response = historyAnalyticsService.getLearningInsights(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        RecommendationResponse response = historyAnalyticsService.getRecommendations(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tests")
    public ResponseEntity<Page<TestHistoryResponse>> getTestHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "endTime"));
        Page<TestHistoryResponse> response = historyAnalyticsService.getTestHistory(
                user.getId(), search, examType, questionType, difficulty, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evaluations")
    public ResponseEntity<Page<EvaluationHistoryResponse>> getEvaluationHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EvaluationHistoryResponse> response = historyAnalyticsService.getEvaluationHistory(
                user.getId(), search, topic, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/flashcards")
    public ResponseEntity<Page<FlashcardHistoryResponse>> getFlashcardHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FlashcardHistoryResponse> response = historyAnalyticsService.getFlashcardHistory(
                user.getId(), search, topic, pageable
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tests/{attemptId}")
    public ResponseEntity<Void> deleteTestHistory(
            @PathVariable UUID attemptId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        historyAnalyticsService.deleteTestHistory(attemptId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/evaluations/{submissionId}")
    public ResponseEntity<Void> deleteEvaluationHistory(
            @PathVariable UUID submissionId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        historyAnalyticsService.deleteEvaluationHistory(submissionId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/flashcards/{flashcardId}")
    public ResponseEntity<Void> deleteFlashcardHistory(
            @PathVariable UUID flashcardId,
            Principal principal
    ) {
        User user = authenticatedUserResolver.resolve(principal);
        historyAnalyticsService.deleteFlashcardHistory(flashcardId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        String csvContent = historyAnalyticsService.exportCsv(user.getId());
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "preppulse-learning-report.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Principal principal) {
        User user = authenticatedUserResolver.resolve(principal);
        byte[] pdfBytes = historyAnalyticsService.exportPdf(user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "preppulse-learning-report.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
