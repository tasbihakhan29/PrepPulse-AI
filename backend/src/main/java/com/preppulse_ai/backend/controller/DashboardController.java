package com.preppulse_ai.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.entity.Flashcard;
import com.preppulse_ai.backend.entity.GeneratedTest;
import com.preppulse_ai.backend.entity.TestResult;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.repository.FlashcardRepository;
import com.preppulse_ai.backend.repository.GeneratedTestRepository;
import com.preppulse_ai.backend.repository.TestResultRepository;
import com.preppulse_ai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final FlashcardRepository flashcardRepository;
    private final GeneratedTestRepository generatedTestRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new SecurityException("No authenticated principal found.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + principal.getName()));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(Principal principal) {
        User user = getAuthenticatedUser(principal);
        UUID userId = user.getId();

        log.info("Fetching dashboard data for user: {}", user.getEmail());

        // 1. Fetch test results & flashcards
        List<TestResult> results = testResultRepository.findAllByUserIdOrderByCompletedAtDesc(userId);
        long flashcardsCount = flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();

        // 2. Compute stats
        int totalTests = results.size();
        double averageScore = 0.0;
        if (totalTests > 0) {
            double sum = results.stream().mapToDouble(TestResult::getScore).sum();
            averageScore = Math.round((sum / totalTests) * 10.0) / 10.0;
        }

        int studyStreak = calculateStreak(results);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("averageScore", averageScore);
        stats.put("studyStreak", studyStreak);
        stats.put("flashcardsGenerated", flashcardsCount);
        stats.put("aiEvaluations", totalTests); // Count each completed test as an evaluation

        // 3. Performance Trend (last 5 tests in chronological order)
        List<Map<String, Object>> performanceTrend = new ArrayList<>();
        int limit = Math.min(results.size(), 5);
        List<TestResult> recentResults = results.subList(0, limit);
        
        // Reverse to make it chronological (oldest to newest) for chart
        List<TestResult> chronologicalResults = new ArrayList<>(recentResults);
        Collections.reverse(chronologicalResults);

        for (int i = 0; i < chronologicalResults.size(); i++) {
            TestResult res = chronologicalResults.get(i);
            Map<String, Object> point = new HashMap<>();
            point.put("id", res.getId().toString());
            point.put("name", res.getTest().getExamType());
            point.put("score", res.getScore().intValue());
            // Format date to local date string
            LocalDate localDate = res.getCompletedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            point.put("date", localDate.toString());
            performanceTrend.add(point);
        }

        // 4. Recent Activity (last 5 tests in reverse chronological order)
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        for (TestResult res : recentResults) {
            Map<String, Object> act = new HashMap<>();
            act.put("id", res.getId().toString());
            act.put("name", res.getTest().getExamType());
            act.put("score", res.getScore().intValue());
            LocalDate localDate = res.getCompletedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            act.put("date", localDate.toString());
            recentActivity.add(act);
        }

        // 5. Learning Insights (Strong Areas vs Needs Improvement)
        Map<String, Double> topicTotalScores = new HashMap<>();
        Map<String, Integer> topicCounts = new HashMap<>();

        for (TestResult res : results) {
            try {
                String json = res.getTopicScoresJson();
                if (json != null && !json.trim().isEmpty()) {
                    Map<String, Double> scores = objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
                    for (Map.Entry<String, Double> entry : scores.entrySet()) {
                        String topic = entry.getKey().trim();
                        Double score = entry.getValue();
                        topicTotalScores.put(topic, topicTotalScores.getOrDefault(topic, 0.0) + score);
                        topicCounts.put(topic, topicCounts.getOrDefault(topic, 0) + 1);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse topic scores JSON for result ID: {}", res.getId(), e);
            }
        }

        List<String> strongTopics = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();

        for (String topic : topicTotalScores.keySet()) {
            double avg = topicTotalScores.get(topic) / topicCounts.get(topic);
            if (avg >= 70.0) {
                strongTopics.add(topic);
            } else {
                weakTopics.add(topic);
            }
        }

        Map<String, Object> insights = new HashMap<>();
        insights.put("strongTopics", strongTopics);
        insights.put("weakTopics", weakTopics);

        // 6. Aggregate response
        Map<String, Object> dashboardResponse = new HashMap<>();
        dashboardResponse.put("stats", stats);
        dashboardResponse.put("performanceTrend", performanceTrend);
        dashboardResponse.put("recentActivity", recentActivity);
        dashboardResponse.put("insights", insights);

        return ResponseEntity.ok(dashboardResponse);
    }

    /**
     * Calculates consecutive days of study streak from test results.
     */
    private int calculateStreak(List<TestResult> results) {
        if (results.isEmpty()) {
            return 0;
        }

        // Map to unique local dates, sorted descending
        Set<LocalDate> dates = results.stream()
                .map(r -> r.getCompletedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        // Convert to list for traversal
        List<LocalDate> sortedDates = new ArrayList<>(dates);
        Collections.reverse(sortedDates); // Newest first

        if (sortedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate latestDate = sortedDates.get(0);

        // If the latest test is older than yesterday, the streak is broken
        if (!latestDate.equals(today) && !latestDate.equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        for (int i = 0; i < sortedDates.size() - 1; i++) {
            LocalDate current = sortedDates.get(i);
            LocalDate next = sortedDates.get(i + 1);
            if (current.minusDays(1).equals(next)) {
                streak++;
            } else if (current.equals(next)) {
                // Same day, do nothing
            } else {
                break; // Gap found, end streak count
            }
        }

        return streak;
    }
}
