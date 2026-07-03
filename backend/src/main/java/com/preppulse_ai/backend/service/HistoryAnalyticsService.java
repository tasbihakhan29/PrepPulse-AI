package com.preppulse_ai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.*;
import com.preppulse_ai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryAnalyticsService {

    private final TestAttemptRepository testAttemptRepository;
    private final TestResultRepository testResultRepository;
    private final AnswerSubmissionRepository answerSubmissionRepository;
    private final FlashcardRepository flashcardRepository;
    private final GeneratedTestRepository generatedTestRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final LearningStatisticsRepository learningStatisticsRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final UserRepository userRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AnalyticsOverviewResponse getAnalyticsOverview(UUID userId) {
        // Calculate from existing tables
        List<TestAttempt> allAttempts = testAttemptRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        long totalTestsTaken = allAttempts.stream().filter(TestAttempt::getSubmitted).count();
        
        long totalEvaluations = answerSubmissionRepository.countByUserId(userId);
        
        List<Flashcard> allFlashcards = flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        long flashcardsGenerated = allFlashcards.size();

        // Calculate average and best score from test_results
        List<TestResult> testResults = testResultRepository.findAllByUserIdOrderByCompletedAtDesc(userId);
        Double averageScore = testResults.stream()
                .mapToDouble(TestResult::getScore)
                .average()
                .orElse(0.0);
        Double bestScore = testResults.stream()
                .mapToDouble(TestResult::getScore)
                .max()
                .orElse(0.0);

        // Calculate study streaks
        Integer currentStudyStreak = calculateCurrentStudyStreak(userId);
        Integer longestStudyStreak = calculateLongestStudyStreak(userId);

        // Calculate hours studied from test_attempts
        List<TestAttempt> attempts = testAttemptRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        int totalSeconds = attempts.stream()
                .filter(TestAttempt::getSubmitted)
                .mapToInt(a -> a.getTimeTaken() != null ? a.getTimeTaken() : 0)
                .sum();
        int hoursStudied = totalSeconds / 3600;

        // Total questions attempted
        int totalQuestionsAttempted = testResults.stream()
                .mapToInt(TestResult::getTotalQuestions)
                .sum();

        return AnalyticsOverviewResponse.builder()
                .totalTestsTaken((int) totalTestsTaken)
                .totalEvaluations((int) totalEvaluations)
                .flashcardsGenerated((int) flashcardsGenerated)
                .averageScore(averageScore)
                .bestScore(bestScore)
                .currentStudyStreak(currentStudyStreak)
                .longestStudyStreak(longestStudyStreak)
                .hoursStudied(hoursStudied)
                .totalQuestionsAttempted(totalQuestionsAttempted)
                .build();
    }

    public List<PerformanceChartData> getPerformanceChartData(UUID userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<DailyActivity> activities = dailyActivityRepository
                .findByUserIdAndActivityDateAfterOrderByActivityDateAsc(userId, startDate);

        return activities.stream()
                .map(da -> {
                    // Calculate average score for this day from test_attempts
                    double avgScore = getAverageScoreForDate(userId, da.getActivityDate());
                    double accuracy = getAccuracyForDate(userId, da.getActivityDate());

                    return PerformanceChartData.builder()
                            .date(da.getActivityDate().toString())
                            .score(avgScore)
                            .accuracy(accuracy)
                            .questionsAttempted(da.getQuestionsAttempted())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TopicPerformanceResponse> getTopicPerformance(UUID userId) {
        List<LearningStatistics> stats = learningStatisticsRepository.findByUserId(userId);

        return stats.stream()
                .filter(ls -> ls.getTopic() != null && !ls.getTopic().isEmpty())
                .map(ls -> {
                    double accuracy = ls.getTotalQuestions() > 0
                            ? (ls.getCorrectQuestions() * 100.0) / ls.getTotalQuestions()
                            : 0.0;
                    String status = accuracy >= 70 ? "STRONG" : accuracy >= 50 ? "AVERAGE" : "WEAK";

                    return TopicPerformanceResponse.builder()
                            .topic(ls.getTopic())
                            .averageScore(ls.getAverageScore())
                            .questionsAttempted(ls.getTotalQuestions())
                            .accuracy(accuracy)
                            .status(status)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<HeatmapDataResponse> getHeatmapData(UUID userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<DailyActivity> activities = dailyActivityRepository
                .findByUserIdAndActivityDateAfterOrderByActivityDateAsc(userId, startDate);

        return activities.stream()
                .map(da -> {
                    int activityLevel = calculateActivityLevel(da);
                    return HeatmapDataResponse.builder()
                            .date(da.getActivityDate())
                            .activityLevel(activityLevel)
                            .testsAttempted(da.getTestsAttempted())
                            .questionsSolved(da.getQuestionsAttempted())
                            .evaluationsCompleted(da.getEvaluationsCompleted())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<QuestionTypeDistributionResponse> getQuestionTypeDistribution(UUID userId) {
        List<GeneratedTest> tests = generatedTestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Integer> typeCount = new HashMap<>();

        for (GeneratedTest test : tests) {
            String type = test.getQuestionType();
            typeCount.put(type, typeCount.getOrDefault(type, 0) + test.getTotalQuestions());
        }

        int total = typeCount.values().stream().mapToInt(Integer::intValue).sum();

        return typeCount.entrySet().stream()
                .map(entry -> QuestionTypeDistributionResponse.builder()
                        .questionType(entry.getKey())
                        .count(entry.getValue())
                        .percentage(total > 0 ? (entry.getValue() * 100.0) / total : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    public List<DifficultyAnalysisResponse> getDifficultyAnalysis(UUID userId) {
        List<GeneratedTest> tests = generatedTestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<String, List<Double>> difficultyScores = new HashMap<>();

        for (GeneratedTest test : tests) {
            String difficulty = test.getDifficulty();
            List<TestResult> results = testResultRepository.findByTestId(userId, test.getId());
            if (!results.isEmpty()) {
                difficultyScores.computeIfAbsent(difficulty, k -> new ArrayList<>())
                        .add(results.get(0).getScore());
            }
        }

        return difficultyScores.entrySet().stream()
                .map(entry -> {
                    List<Double> scores = entry.getValue();
                    double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double accuracy = avgScore / 4.0 * 100; // Assuming max 4 marks per question

                    return DifficultyAnalysisResponse.builder()
                            .difficulty(entry.getKey())
                            .count(scores.size())
                            .averageScore(avgScore)
                            .accuracy(accuracy)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<LearningInsightResponse> getLearningInsights(UUID userId) {
        List<LearningInsightResponse> insights = new ArrayList<>();

        // Get topic performance
        List<TopicPerformanceResponse> topicPerformance = getTopicPerformance(userId);

        // Strong topics
        List<TopicPerformanceResponse> strongTopics = topicPerformance.stream()
                .filter(t -> "STRONG".equals(t.getStatus()))
                .collect(Collectors.toList());
        if (!strongTopics.isEmpty()) {
            String topics = strongTopics.stream()
                    .map(TopicPerformanceResponse::getTopic)
                    .collect(Collectors.joining(", "));
            insights.add(LearningInsightResponse.builder()
                    .insight("You consistently perform well in " + topics + ".")
                    .type("STRENGTH")
                    .topic(strongTopics.get(0).getTopic())
                    .value(strongTopics.get(0).getAccuracy())
                    .build());
        }

        // Weak topics
        List<TopicPerformanceResponse> weakTopics = topicPerformance.stream()
                .filter(t -> "WEAK".equals(t.getStatus()))
                .collect(Collectors.toList());
        if (!weakTopics.isEmpty()) {
            String topics = weakTopics.stream()
                    .map(TopicPerformanceResponse::getTopic)
                    .collect(Collectors.joining(", "));
            insights.add(LearningInsightResponse.builder()
                    .insight("Revision recommended for " + topics + ".")
                    .type("WEAKNESS")
                    .topic(weakTopics.get(0).getTopic())
                    .value(weakTopics.get(0).getAccuracy())
                    .build());
        }

        // Question type analysis
        List<QuestionTypeDistributionResponse> typeDist = getQuestionTypeDistribution(userId);
        if (!typeDist.isEmpty()) {
            String dominantType = typeDist.stream()
                    .max(Comparator.comparing(QuestionTypeDistributionResponse::getCount))
                    .map(QuestionTypeDistributionResponse::getQuestionType)
                    .orElse("MCQ");
            insights.add(LearningInsightResponse.builder()
                    .insight("Your strongest exam pattern is " + dominantType + ".")
                    .type("RECOMMENDATION")
                    .topic(dominantType)
                    .value(typeDist.get(0).getPercentage())
                    .build());
        }

        // Improvement analysis
        List<PerformanceChartData> chartData = getPerformanceChartData(userId, 30);
        if (chartData.size() >= 2) {
            double recentAvg = chartData.stream()
                    .skip(Math.max(0, chartData.size() - 7))
                    .mapToDouble(PerformanceChartData::getAccuracy)
                    .average()
                    .orElse(0.0);
            double olderAvg = chartData.stream()
                    .limit(Math.max(0, chartData.size() - 7))
                    .mapToDouble(PerformanceChartData::getAccuracy)
                    .average()
                    .orElse(0.0);

            if (recentAvg > olderAvg) {
                double improvement = ((recentAvg - olderAvg) / olderAvg) * 100;
                insights.add(LearningInsightResponse.builder()
                        .insight("Your accuracy has improved by " + String.format("%.1f", improvement) + "% in the last week.")
                        .type("IMPROVEMENT")
                        .value(improvement)
                        .build());
            }
        }

        return insights;
    }

    public RecommendationResponse getRecommendations(UUID userId) {
        List<TopicPerformanceResponse> weakTopics = learningStatisticsRepository
                .findWeakTopicsByUserId(userId)
                .stream()
                .map(ls -> TopicPerformanceResponse.builder()
                        .topic(ls.getTopic())
                        .averageScore(ls.getAverageScore())
                        .questionsAttempted(ls.getTotalQuestions())
                        .accuracy(ls.getAveragePercentage())
                        .status("WEAK")
                        .build())
                .collect(Collectors.toList());

        String recommendedTopic = weakTopics.isEmpty() ? "General" : weakTopics.get(0).getTopic();
        String suggestedDifficulty = weakTopics.isEmpty() ? "Medium" : "Easy";
        String suggestedQuestionType = "MCQ";
        Integer recommendedDailyGoal = 20;
        Integer recommendedFlashcards = 10;

        return RecommendationResponse.builder()
                .recommendedTopic(recommendedTopic)
                .suggestedDifficulty(suggestedDifficulty)
                .suggestedQuestionType(suggestedQuestionType)
                .recommendedDailyGoal(recommendedDailyGoal)
                .recommendedFlashcards(recommendedFlashcards)
                .build();
    }

    public Page<TestHistoryResponse> getTestHistory(UUID userId, String search, String examType, String questionType, String difficulty, Pageable pageable) {
        Page<TestAttempt> attempts = testAttemptRepository.findFiltered(userId, search, examType, questionType, difficulty, pageable);

        return attempts.map(attempt -> {
            GeneratedTest test = attempt.getTest();
            return TestHistoryResponse.builder()
                    .testId(test.getId())
                    .attemptId(attempt.getId())
                    .examType(test.getExamType())
                    .questionType(test.getQuestionType())
                    .difficulty(test.getDifficulty())
                    .score(attempt.getScore())
                    .percentage(attempt.getPercentage())
                    .timeTaken(attempt.getTimeTaken())
                    .date(attempt.getStartTime())
                    .totalQuestions(test.getTotalQuestions())
                    .correctQuestions(calculateCorrectCount(attempt.getId()))
                    .build();
        });
    }

    public Page<EvaluationHistoryResponse> getEvaluationHistory(UUID userId, String search, Pageable pageable) {
        return getEvaluationHistory(userId, search, null, null, pageable);
    }

    public Page<EvaluationHistoryResponse> getEvaluationHistory(UUID userId, String search, String topic, String status, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search;
        String normalizedTopic = (topic == null || topic.isBlank()) ? null : topic;
        String normalizedStatus = (status == null || status.isBlank()) ? null : status;

        Page<AnswerSubmission> submissions = answerSubmissionRepository.findFiltered(
                userId,
                normalizedSearch,
                normalizedTopic,
                normalizedStatus,
                pageable
        );

        return submissions.map(submission -> {
            Optional<AnswerEvaluation> evaluationOpt = answerEvaluationRepository.findBySubmissionId(submission.getId());
            return EvaluationHistoryResponse.builder()
                    .submissionId(submission.getId())
                    .evaluationId(evaluationOpt.map(AnswerEvaluation::getId).orElse(null))
                    .question(submission.getQuestion())
                    .topic(submission.getTopic())
                    .marksLimit(submission.getMarksLimit())
                    .score(evaluationOpt.map(AnswerEvaluation::getScore).orElse(null))
                    .maxMarks(evaluationOpt.map(AnswerEvaluation::getMaxMarks).orElse(submission.getMarksLimit()))
                    .evaluationStatus(submission.getEvaluationStatus())
                    .fileUrl(submission.getFileUrl())
                    .fileType(submission.getFileType())
                    .createdAt(submission.getCreatedAt())
                    .build();
        });
    }

    public Page<FlashcardHistoryResponse> getFlashcardHistory(UUID userId, String topic, Pageable pageable) {
        return getFlashcardHistory(userId, null, topic, pageable);
    }

    public Page<FlashcardHistoryResponse> getFlashcardHistory(UUID userId, String search, String topic, Pageable pageable) {
        Page<Flashcard> flashcards = flashcardRepository.findFiltered(
                userId,
                (search == null || search.isBlank()) ? null : search,
                (topic == null || topic.isBlank()) ? null : topic,
                pageable
        );

        return flashcards.map(flashcard -> FlashcardHistoryResponse.builder()
                .id(flashcard.getId())
                .question(flashcard.getQuestion())
                .answer(flashcard.getAnswer())
                .topic(flashcard.getTopic())
                .createdAt(flashcard.getCreatedAt())
                .build());
    }

    @Transactional
    public void deleteTestHistory(UUID attemptId, UUID userId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own history");
        }

        // Delete attempt answers first
        attemptAnswerRepository.deleteByAttemptId(attemptId);
        // Delete attempt
        testAttemptRepository.delete(attempt);
    }

    @Transactional
    public void deleteEvaluationHistory(UUID submissionId, UUID userId) {
        AnswerSubmission submission = answerSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!submission.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own history");
        }

        answerSubmissionRepository.delete(submission);
    }

    @Transactional
    public void deleteFlashcardHistory(UUID flashcardId, UUID userId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new RuntimeException("Flashcard not found"));

        if (!flashcard.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own history");
        }

        flashcardRepository.delete(flashcard);
    }

    @Transactional
    public void updateLearningStatistics(UUID userId) {
        // This method should be called after test submission, evaluation, etc.
        // It aggregates data and updates learning_statistics table
        // For brevity, this is a simplified version
        log.info("Updating learning statistics for user: {}", userId);
    }

    @Transactional
    public void updateDailyActivity(UUID userId, LocalDate date) {
        // This method updates daily_activity table
        Optional<DailyActivity> existing = dailyActivityRepository.findByUserIdAndActivityDate(userId, date);

        DailyActivity activity;
        if (existing.isPresent()) {
            activity = existing.get();
        } else {
            activity = DailyActivity.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .activityDate(date)
                    .build();
        }

        // Calculate activity for the day
        int testsAttempted = (int) testAttemptRepository.countByUserIdAndSubmittedTrueAndStartTimeBetween(
                userId,
                date.atStartOfDay().atOffset(ZoneOffset.UTC),
                date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        );

        activity.setTestsAttempted(testsAttempted);
        dailyActivityRepository.save(activity);
    }

    // Helper methods
    private Integer calculateCurrentStudyStreak(UUID userId) {
        List<DailyActivity> activities = dailyActivityRepository.findByUserIdOrderByActivityDateDesc(userId);
        int streak = 0;
        LocalDate currentDate = LocalDate.now();

        for (DailyActivity activity : activities) {
            if (activity.getActivityDate().equals(currentDate.minusDays(streak)) ||
                activity.getActivityDate().equals(currentDate)) {
                if (activity.getTestsAttempted() > 0 || activity.getQuestionsAttempted() > 0) {
                    streak++;
                }
            } else {
                break;
            }
        }

        return streak;
    }

    private Integer calculateLongestStudyStreak(UUID userId) {
        List<DailyActivity> activities = dailyActivityRepository.findByUserIdOrderByActivityDateAsc(userId);
        int longestStreak = 0;
        int currentStreak = 0;
        LocalDate previousDate = null;

        for (DailyActivity activity : activities) {
            if (activity.getTestsAttempted() > 0 || activity.getQuestionsAttempted() > 0) {
                if (previousDate == null || activity.getActivityDate().equals(previousDate.plusDays(1))) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                longestStreak = Math.max(longestStreak, currentStreak);
                previousDate = activity.getActivityDate();
            } else {
                currentStreak = 0;
            }
        }

        return longestStreak;
    }

    private int calculateActivityLevel(DailyActivity activity) {
        int totalActivity = activity.getTestsAttempted() +
                           activity.getQuestionsAttempted() +
                           activity.getEvaluationsCompleted() +
                           activity.getFlashcardsReviewed();

        if (totalActivity == 0) return 0;
        if (totalActivity <= 5) return 1;
        if (totalActivity <= 10) return 2;
        if (totalActivity <= 20) return 3;
        if (totalActivity <= 40) return 4;
        return 5;
    }

    private double getAverageScoreForDate(UUID userId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<TestResult> results = testResultRepository.findByUserIdAndCompletedAtBetween(userId, start, end);
        return results.stream()
                .mapToDouble(TestResult::getScore)
                .average()
                .orElse(0.0);
    }

    private double getAccuracyForDate(UUID userId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<TestResult> results = testResultRepository.findByUserIdAndCompletedAtBetween(userId, start, end);
        return results.stream()
                .mapToDouble(r -> r.getTotalQuestions() > 0 ? (r.getCorrectQuestions() * 100.0) / r.getTotalQuestions() : 0.0)
                .average()
                .orElse(0.0);
    }

    private int calculateCorrectCount(UUID attemptId) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        return (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
    }

    public String exportCsv(UUID userId) {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM
        csv.append('\ufeff');
        csv.append("Date,Test ID,Attempt ID,Exam Type,Question Type,Difficulty,Score,Percentage,Time Taken (sec),Correct Questions,Total Questions\n");
        
        List<TestAttempt> attempts = testAttemptRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        for (TestAttempt attempt : attempts) {
            if (!Boolean.TRUE.equals(attempt.getSubmitted())) {
                continue;
            }
            GeneratedTest test = attempt.getTest();
            csv.append("\"").append(attempt.getStartTime()).append("\",");
            csv.append("\"").append(test.getId()).append("\",");
            csv.append("\"").append(attempt.getId()).append("\",");
            csv.append("\"").append(test.getExamType()).append("\",");
            csv.append("\"").append(test.getQuestionType()).append("\",");
            csv.append("\"").append(test.getDifficulty()).append("\",");
            csv.append(attempt.getScore()).append(",");
            csv.append(attempt.getPercentage()).append(",");
            csv.append(attempt.getTimeTaken() != null ? attempt.getTimeTaken() : 0).append(",");
            csv.append(calculateCorrectCount(attempt.getId())).append(",");
            csv.append(test.getTotalQuestions()).append("\n");
        }
        return csv.toString();
    }

    public byte[] exportPdf(UUID userId) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            AnalyticsOverviewResponse overview = getAnalyticsOverview(userId);
            List<TopicPerformanceResponse> topics = getTopicPerformance(userId);
            RecommendationResponse recs = getRecommendations(userId);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header Banner
                contentStream.setNonStrokingColor(79, 70, 229); // #4F46E5 (Indigo)
                contentStream.addRect(0, 700, 612, 92);
                contentStream.fill();
                
                // Title in white
                contentStream.setNonStrokingColor(255, 255, 255);
                drawText(contentStream, "PREPPULSE AI - LEARNING ANALYTICS REPORT", 40, 740, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                
                // Report details in white
                drawText(contentStream, "Report Generated: " + LocalDate.now(), 40, 715, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                
                // Body content
                contentStream.setNonStrokingColor(0, 0, 0); // Black
                drawText(contentStream, "OVERALL PERFORMANCE SUMMARY", 40, 650, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                
                contentStream.setStrokingColor(226, 232, 240); // light gray
                contentStream.moveTo(40, 640);
                contentStream.lineTo(572, 640);
                contentStream.stroke();
                
                int statsY = 615;
                drawText(contentStream, "Total Tests Attempted: " + overview.getTotalTestsTaken(), 50, statsY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "Total AI Evaluations: " + overview.getTotalEvaluations(), 50, statsY - 20, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "Questions Answered: " + overview.getTotalQuestionsAttempted(), 50, statsY - 40, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                
                drawText(contentStream, "Average Score Percentage: " + String.format("%.1f%%", overview.getAverageScore()), 320, statsY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "Best Score Percentage: " + String.format("%.1f%%", overview.getBestScore()), 320, statsY - 20, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "Current Study Streak: " + overview.getCurrentStudyStreak() + " Days", 320, statsY - 40, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                
                // Topics Section
                drawText(contentStream, "TOPIC PERFORMANCE DETAILS", 40, 520, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.moveTo(40, 510);
                contentStream.lineTo(572, 510);
                contentStream.stroke();
                
                // Table Headers
                int tableHeaderY = 490;
                drawText(contentStream, "Topic Focus Area", 50, tableHeaderY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                drawText(contentStream, "Avg Accuracy", 250, tableHeaderY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                drawText(contentStream, "Questions Solved", 380, tableHeaderY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                drawText(contentStream, "Learning Level", 480, tableHeaderY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                
                contentStream.moveTo(40, 480);
                contentStream.lineTo(572, 480);
                contentStream.stroke();
                
                int tableY = 460;
                for (TopicPerformanceResponse topic : topics) {
                    if (tableY < 230) {
                        break; // Keep it on one page cleanly
                    }
                    drawText(contentStream, topic.getTopic(), 50, tableY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    drawText(contentStream, String.format("%.1f%%", topic.getAccuracy()), 250, tableY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    drawText(contentStream, String.valueOf(topic.getQuestionsAttempted()), 380, tableY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    
                    // Style status color based on level
                    if ("STRONG".equalsIgnoreCase(topic.getStatus())) {
                        contentStream.setNonStrokingColor(16, 185, 129); // #10B981 Green
                    } else if ("WEAK".equalsIgnoreCase(topic.getStatus())) {
                        contentStream.setNonStrokingColor(239, 68, 68); // #EF4444 Red
                    } else {
                        contentStream.setNonStrokingColor(245, 158, 11); // #F59E0B Yellow
                    }
                    drawText(contentStream, topic.getStatus(), 480, tableY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                    contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                    
                    tableY -= 20;
                }
                
                // Recommendations Section
                int recHeaderY = Math.min(tableY - 20, 200);
                drawText(contentStream, "SMART RECOMMENDATIONS", 40, recHeaderY, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.moveTo(40, recHeaderY - 10);
                contentStream.lineTo(572, recHeaderY - 10);
                contentStream.stroke();
                
                int recY = recHeaderY - 30;
                drawText(contentStream, "• Recommended focus topic: " + recs.getRecommendedTopic(), 50, recY, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "• Suggested study difficulty: " + recs.getSuggestedDifficulty(), 50, recY - 20, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "• Recommended daily question target: " + recs.getRecommendedDailyGoal() + " questions", 50, recY - 40, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                drawText(contentStream, "• Targeted flashcard reviews to perform: " + recs.getRecommendedFlashcards() + " cards", 50, recY - 60, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                
                // Footer
                contentStream.setNonStrokingColor(148, 163, 184); // slate gray
                drawText(contentStream, "PrepPulse AI - Complete Analytics & Study Assistance. Confidential Report.", 40, 30, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("Could not generate PDF learning report", e);
        }
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y, PDType1Font font, float fontSize) throws java.io.IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "");
        contentStream.endText();
    }

    @Transactional
    public void recordTestAttempt(TestAttempt attempt) {
        updateDailyActivity(attempt.getUser().getId(), attempt.getStartTime().atZoneSameInstant(ZoneOffset.UTC).toLocalDate());
        updateLearningStatisticsFromAttempt(attempt);
    }

    @Transactional
    public void recordAnswerEvaluation(AnswerSubmission submission) {
        updateDailyActivity(submission.getUser().getId(), submission.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate());
        updateLearningStatisticsFromEvaluation(submission);
    }

    @Transactional
    public void recordFlashcardActivity(UUID userId, int reviewedCount) {
        updateDailyActivity(userId, LocalDate.now(ZoneOffset.UTC));
        Optional<DailyActivity> existing = dailyActivityRepository.findByUserIdAndActivityDate(userId, LocalDate.now(ZoneOffset.UTC));
        DailyActivity activity = existing.orElseGet(() -> DailyActivity.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .activityDate(LocalDate.now(ZoneOffset.UTC))
                .build());
        activity.setFlashcardsReviewed((activity.getFlashcardsReviewed() == null ? 0 : activity.getFlashcardsReviewed()) + reviewedCount);
        dailyActivityRepository.save(activity);
    }

    private void updateLearningStatisticsFromAttempt(TestAttempt attempt) {
        if (attempt.getTest() == null) return;

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        Map<String, List<AttemptAnswer>> byTopic = answers.stream()
                .collect(Collectors.groupingBy(a -> {
                    String topic = a.getQuestion() != null ? a.getQuestion().getTopic() : null;
                    return (topic == null || topic.isBlank()) ? "General" : topic;
                }));

        byTopic.forEach((topic, topicAnswers) -> upsertLearningStatistics(attempt, topic, topicAnswers));
    }

    private void updateLearningStatisticsFromEvaluation(AnswerSubmission submission) {
        upsertLearningStatisticsFromEvaluation(submission);
    }

    private void upsertLearningStatistics(TestAttempt attempt, String topic, List<AttemptAnswer> topicAnswers) {
        String examType = attempt.getTest().getExamType();
        String questionType = attempt.getTest().getQuestionType();
        String difficulty = attempt.getTest().getDifficulty();

        LearningStatistics stats = learningStatisticsRepository
                .findByUserIdAndTopic(attempt.getUser().getId(), topic)
                .filter(existing -> Objects.equals(existing.getExamType(), examType)
                        && Objects.equals(existing.getQuestionType(), questionType)
                        && Objects.equals(existing.getDifficulty(), difficulty))
                .orElseGet(() -> LearningStatistics.builder()
                        .user(attempt.getUser())
                        .topic(topic)
                        .examType(examType)
                        .questionType(questionType)
                        .difficulty(difficulty)
                        .build());

        int attempted = topicAnswers.size();
        int correct = (int) topicAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        int wrong = (int) topicAnswers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();
        int skipped = Math.max(0, attempted - correct - wrong);

        stats.setTotalQuestions((stats.getTotalQuestions() == null ? 0 : stats.getTotalQuestions()) + attempted);
        stats.setCorrectQuestions((stats.getCorrectQuestions() == null ? 0 : stats.getCorrectQuestions()) + correct);
        stats.setWrongQuestions((stats.getWrongQuestions() == null ? 0 : stats.getWrongQuestions()) + wrong);
        stats.setSkippedQuestions((stats.getSkippedQuestions() == null ? 0 : stats.getSkippedQuestions()) + skipped);
        stats.setAverageScore(attempt.getScore());
        stats.setAveragePercentage(attempt.getPercentage());
        stats.setTotalTimeSpent((stats.getTotalTimeSpent() == null ? 0 : stats.getTotalTimeSpent()) + (attempt.getTimeTaken() == null ? 0 : attempt.getTimeTaken()));
        stats.setLastActivityDate(attempt.getEndTime() != null ? attempt.getEndTime() : attempt.getStartTime());
        stats.setUpdatedAt(OffsetDateTime.now());

        learningStatisticsRepository.save(stats);
    }

    private void upsertLearningStatisticsFromEvaluation(AnswerSubmission submission) {
        String topic = submission.getTopic() == null ? "General" : submission.getTopic();
        LearningStatistics stats = learningStatisticsRepository.findByUserIdAndTopic(submission.getUser().getId(), topic)
                .orElseGet(() -> LearningStatistics.builder()
                        .user(submission.getUser())
                        .topic(topic)
                        .build());
        stats.setTotalQuestions((stats.getTotalQuestions() == null ? 0 : stats.getTotalQuestions()) + 1);
        stats.setWrongQuestions(stats.getWrongQuestions() == null ? 0 : stats.getWrongQuestions());
        stats.setUpdatedAt(OffsetDateTime.now());
        learningStatisticsRepository.save(stats);
    }
}
