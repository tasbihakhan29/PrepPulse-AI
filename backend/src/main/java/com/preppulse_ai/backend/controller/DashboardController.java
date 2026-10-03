package com.preppulse_ai.backend.controller;

import com.preppulse_ai.backend.entity.TestAttempt;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.dto.TopicPerformanceResponse;
import com.preppulse_ai.backend.repository.AnswerEvaluationRepository;
import com.preppulse_ai.backend.repository.TestAttemptRepository;
import com.preppulse_ai.backend.repository.UserRepository;
import com.preppulse_ai.backend.service.HistoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final HistoryAnalyticsService historyAnalyticsService;

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

        List<TestAttempt> attempts = testAttemptRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(attempt -> Boolean.TRUE.equals(attempt.getSubmitted()))
            .collect(Collectors.toList());
        int totalTests = attempts.size();
        double averageScore = round(attempts.stream()
            .map(TestAttempt::getPercentage)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average().orElse(0.0));

        int studyStreak = calculateStreak(attempts);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("averageScore", averageScore);
        stats.put("studyStreak", studyStreak);
        stats.put("aiEvaluations", answerEvaluationRepository.countByUserId(userId));

        // 3. Performance Trend (last 5 tests in chronological order)
        List<Map<String, Object>> performanceTrend = new ArrayList<>();
        int limit = Math.min(attempts.size(), 5);
        List<TestAttempt> recentAttempts = attempts.subList(0, limit);
        
        // Reverse to make it chronological (oldest to newest) for chart
        List<TestAttempt> chronologicalResults = new ArrayList<>(recentAttempts);
        Collections.reverse(chronologicalResults);

        for (TestAttempt attempt : chronologicalResults) {
            Map<String, Object> point = new HashMap<>();
            point.put("id", attempt.getId().toString());
            point.put("name", attempt.getTest().getExamType());
            point.put("score", Math.round(valueOrZero(attempt.getPercentage())));
            LocalDate localDate = eventDate(attempt);
            point.put("date", localDate.toString());
            performanceTrend.add(point);
        }

        // 4. Recent Activity (last 5 tests in reverse chronological order)
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        for (TestAttempt attempt : recentAttempts) {
            Map<String, Object> act = new HashMap<>();
            act.put("id", attempt.getId().toString());
            act.put("name", attempt.getTest().getExamType());
            act.put("score", Math.round(valueOrZero(attempt.getPercentage())));
            LocalDate localDate = eventDate(attempt);
            act.put("date", localDate.toString());
            recentActivity.add(act);
        }

        List<String> strongTopics = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();
        for (TopicPerformanceResponse topic : historyAnalyticsService.getTopicPerformance(userId)) {
            if ("STRONG".equals(topic.getStatus())) {
                strongTopics.add(topic.getTopic());
            } else if ("WEAK".equals(topic.getStatus())) {
                weakTopics.add(topic.getTopic());
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

    private LocalDate eventDate(TestAttempt attempt) {
        return (attempt.getEndTime() != null ? attempt.getEndTime() : attempt.getStartTime())
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int calculateStreak(List<TestAttempt> attempts) {
        if (attempts.isEmpty()) {
            return 0;
        }

        Set<LocalDate> dates = attempts.stream()
                .map(this::eventDate)
                .collect(Collectors.toCollection(TreeSet::new));

        // Convert to list for traversal
        List<LocalDate> sortedDates = new ArrayList<>(dates);
        Collections.reverse(sortedDates); // Newest first

        if (sortedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
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
