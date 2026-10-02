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

        @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getAnalyticsOverview(UUID userId) {
        List<TestAttempt> attempts = completedAttempts(userId);
        long totalTestsTaken = attempts.size();
        long totalEvaluations = answerEvaluationRepository.countByUserId(userId);
        List<Flashcard> allFlashcards = flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        long flashcardsGenerated = allFlashcards.size();

        Double averageScore = attempts.stream()
            .map(TestAttempt::getPercentage)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        Double bestScore = attempts.stream()
            .map(TestAttempt::getPercentage)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Set<LocalDate> activityDates = getActivityDates(userId);
        Integer currentStudyStreak = calculateCurrentStudyStreak(activityDates);
        Integer longestStudyStreak = calculateLongestStudyStreak(activityDates);

        int totalSeconds = attempts.stream()
                .mapToInt(a -> a.getTimeTaken() != null ? a.getTimeTaken() : 0)
                .sum();
        int hoursStudied = totalSeconds / 3600;

        int totalQuestionsAttempted = attempts.stream()
            .mapToInt(a -> a.getTest().getTotalQuestions() == null ? 0 : a.getTest().getTotalQuestions())
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

    @Transactional(readOnly = true)
    public List<PerformanceChartData> getPerformanceChartData(UUID userId, int days) {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(days);
        Map<LocalDate, List<TestAttempt>> attemptsByDate = completedAttempts(userId).stream()
                .filter(attempt -> !eventDate(attempt).isBefore(startDate))
                .collect(Collectors.groupingBy(this::eventDate, TreeMap::new, Collectors.toList()));

        return attemptsByDate.entrySet().stream().map(entry -> {
            List<TestAttempt> attempts = entry.getValue();
            return PerformanceChartData.builder()
                    .date(entry.getKey().toString())
                    .score(attempts.stream().map(TestAttempt::getPercentage).filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue).average().orElse(0.0))
                    .accuracy(attempts.stream().mapToDouble(this::attemptAccuracy).average().orElse(0.0))
                    .questionsAttempted(attempts.stream().mapToInt(this::questionCount).sum())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopicPerformanceResponse> getTopicPerformance(UUID userId) {
        Map<String, TopicAggregate> aggregates = new HashMap<>();
        for (TestAttempt attempt : completedAttempts(userId)) {
            List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
            if (!answers.isEmpty()) {
                for (AttemptAnswer answer : answers) {
                    String topic = answer.getQuestion().getTopic();
                    topic = topic == null || topic.isBlank() ? "General" : topic;
                    aggregates.computeIfAbsent(topic, ignored -> new TopicAggregate())
                            .add(Boolean.TRUE.equals(answer.getIsCorrect()));
                }
            } else {
                addLegacyTopics(aggregates, attempt, userId);
            }
        }
        return aggregates.entrySet().stream().map(entry -> {
            TopicAggregate aggregate = entry.getValue();
            double accuracy = aggregate.total == 0 ? 0.0 : aggregate.correct * 100.0 / aggregate.total;
            return TopicPerformanceResponse.builder().topic(entry.getKey())
                    .averageScore(accuracy).questionsAttempted(aggregate.total).accuracy(accuracy)
                    .status(accuracy >= 70 ? "STRONG" : accuracy >= 50 ? "AVERAGE" : "WEAK")
                    .build();
        }).sorted(Comparator.comparing(TopicPerformanceResponse::getTopic)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HeatmapDataResponse> getHeatmapData(UUID userId, int days) {
        int requestedDays = Math.max(1, days);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(requestedDays - 1L);
        Map<LocalDate, ActivityAggregate> activity = new TreeMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            activity.put(date, new ActivityAggregate());
        }
        for (TestAttempt attempt : completedAttempts(userId)) {
            LocalDate date = eventDate(attempt);
            if (!date.isBefore(startDate)) {
                activity.computeIfAbsent(date, ignored -> new ActivityAggregate()).addTest(questionCount(attempt), attempt.getTimeTaken());
            }
        }
        for (AnswerEvaluation evaluation : answerEvaluationRepository.findByUserIdOrderByCreatedAtAsc(userId)) {
            LocalDate date = evaluation.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
            if (!date.isBefore(startDate)) activity.computeIfAbsent(date, ignored -> new ActivityAggregate()).evaluations++;
        }
        for (Flashcard flashcard : flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId)) {
            LocalDate date = flashcard.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
            if (!date.isBefore(startDate)) activity.computeIfAbsent(date, ignored -> new ActivityAggregate()).flashcards++;
        }
        return activity.entrySet().stream().map(entry -> {
            ActivityAggregate value = entry.getValue();
            int total = value.tests + value.questions + value.evaluations + value.flashcards;
            int level = total == 0 ? 0 : total <= 5 ? 1 : total <= 10 ? 2 : total <= 20 ? 3 : total <= 40 ? 4 : 5;
            return HeatmapDataResponse.builder().date(entry.getKey()).activityLevel(level)
                    .testsAttempted(value.tests).questionsSolved(value.questions)
                    .evaluationsCompleted(value.evaluations).build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionTypeDistributionResponse> getQuestionTypeDistribution(UUID userId) {
        List<GeneratedTest> tests = generatedTestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Integer> typeCount = new HashMap<>();

        for (GeneratedTest test : tests) {
            for (GeneratedQuestion question : generatedQuestionRepository.findAllByTestId(test.getId())) {
                String type = inferQuestionType(question);
                if (type == null && !"Mixed".equalsIgnoreCase(test.getQuestionType())) type = test.getQuestionType();
                if (type != null) typeCount.merge(type, 1, Integer::sum);
            }
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

    @Transactional(readOnly = true)
    public List<DifficultyAnalysisResponse> getDifficultyAnalysis(UUID userId) {
        List<GeneratedTest> tests = generatedTestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<String, List<Double>> difficultyScores = new HashMap<>();

        for (GeneratedTest test : tests) {
            for (TestAttempt attempt : completedAttempts(userId).stream()
                    .filter(candidate -> candidate.getTest().getId().equals(test.getId())).collect(Collectors.toList())) {
                List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
                if (!answers.isEmpty()) {
                    for (AttemptAnswer answer : answers) {
                        String difficulty = answer.getQuestion().getDifficulty();
                        if (difficulty != null && !difficulty.isBlank()) {
                            difficultyScores.computeIfAbsent(difficulty, ignored -> new ArrayList<>())
                                    .add(Boolean.TRUE.equals(answer.getIsCorrect()) ? 100.0 : 0.0);
                        }
                    }
                } else if (test.getDifficulty() != null && !"Mixed".equalsIgnoreCase(test.getDifficulty())) {
                    difficultyScores.computeIfAbsent(test.getDifficulty(), ignored -> new ArrayList<>())
                            .add(valueOrZero(attempt.getPercentage()));
                }
            }
        }

        return difficultyScores.entrySet().stream()
                .map(entry -> {
                    List<Double> scores = entry.getValue();
                    double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return DifficultyAnalysisResponse.builder()
                            .difficulty(entry.getKey())
                            .count(scores.size())
                            .averageScore(avgScore)
                            .accuracy(avgScore)
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
                    .value(typeDist.stream()
                        .filter(type -> dominantType.equals(type.getQuestionType()))
                        .map(QuestionTypeDistributionResponse::getPercentage)
                        .findFirst().orElse(0.0))
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
                double improvement = olderAvg == 0.0 ? recentAvg : ((recentAvg - olderAvg) / olderAvg) * 100;
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
                    .date(attempt.getEndTime() != null ? attempt.getEndTime() : attempt.getStartTime())
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

        Page<AnswerSubmission> submissions = answerSubmissionRepository.findCompletedFiltered(
                userId,
                normalizedSearch,
                normalizedTopic,
                pageable
        );

        return submissions.map(submission -> {
            Optional<AnswerEvaluation> evaluationOpt = answerEvaluationRepository.findBySubmissionId(submission.getId());
            return EvaluationHistoryResponse.builder()
                    .id(submission.getId())
                    .submissionId(submission.getId())
                    .evaluationId(evaluationOpt.map(AnswerEvaluation::getId).orElse(null))
                    .question(submission.getQuestion())
                    .topic(submission.getTopic())
                    .marksLimit(submission.getMarksLimit())
                    .score(evaluationOpt.map(AnswerEvaluation::getScore).orElse(null))
                    .marksObtained(evaluationOpt.map(AnswerEvaluation::getScore).orElse(null))
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
        List<TestAttempt> attempts = completedAttempts(userId).stream()
            .filter(attempt -> !eventDate(attempt).isBefore(date) && !eventDate(attempt).isAfter(date))
            .collect(Collectors.toList());
        activity.setTestsAttempted(attempts.size());
        activity.setQuestionsAttempted(attempts.stream().mapToInt(this::questionCount).sum());
        activity.setTimeSpent(attempts.stream().mapToInt(attempt -> attempt.getTimeTaken() == null ? 0 : attempt.getTimeTaken()).sum());
        activity.setEvaluationsCompleted((int) answerEvaluationRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
            .filter(evaluation -> evaluation.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().equals(date))
            .count());
        activity.setFlashcardsReviewed((int) flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(card -> card.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().equals(date))
            .count());
        dailyActivityRepository.save(activity);
    }

    // Helper methods
    private Integer calculateCurrentStudyStreak(Set<LocalDate> dates) {
        int streak = 0;
        LocalDate currentDate = LocalDate.now(ZoneOffset.UTC);
        if (!dates.contains(currentDate) && !dates.contains(currentDate.minusDays(1))) {
            return 0;
        }
        if (!dates.contains(currentDate)) {
            currentDate = currentDate.minusDays(1);
        }
        while (dates.contains(currentDate.minusDays(streak))) {
            streak++;
        }
        return streak;
    }

    private Integer calculateLongestStudyStreak(Set<LocalDate> dates) {
        int longestStreak = 0;
        int currentStreak = 0;
        LocalDate previousDate = null;
        for (LocalDate date : new TreeSet<>(dates)) {
            if (previousDate == null || date.equals(previousDate.plusDays(1))) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }
            longestStreak = Math.max(longestStreak, currentStreak);
            previousDate = date;
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

    private List<TestAttempt> completedAttempts(UUID userId) {
        return testAttemptRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getSubmitted()))
                .collect(Collectors.toList());
    }

    private LocalDate eventDate(TestAttempt attempt) {
        OffsetDateTime timestamp = attempt.getEndTime() != null ? attempt.getEndTime() : attempt.getStartTime();
        return timestamp.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private int questionCount(TestAttempt attempt) {
        return attempt.getTest().getTotalQuestions() == null ? 0 : attempt.getTest().getTotalQuestions();
    }

    private double attemptAccuracy(TestAttempt attempt) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        if (!answers.isEmpty()) {
            return answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect())).count() * 100.0
                    / answers.size();
        }
        if (attempt.getLegacyResultId() != null) {
            return testResultRepository.findById(attempt.getLegacyResultId())
                    .map(result -> result.getTotalQuestions() == 0 ? 0.0
                            : result.getCorrectQuestions() * 100.0 / result.getTotalQuestions())
                    .orElse(valueOrZero(attempt.getPercentage()));
        }
        return valueOrZero(attempt.getPercentage());
    }

    private Set<LocalDate> getActivityDates(UUID userId) {
        Set<LocalDate> dates = new TreeSet<>();
        completedAttempts(userId).forEach(attempt -> dates.add(eventDate(attempt)));
        answerEvaluationRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .forEach(evaluation -> dates.add(evaluation.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()));
        flashcardRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .forEach(card -> dates.add(card.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()));
        return dates;
    }

    private void addLegacyTopics(Map<String, TopicAggregate> aggregates, TestAttempt attempt, UUID userId) {
        if (attempt.getLegacyResultId() == null) return;
        Optional<TestResult> result = testResultRepository.findById(attempt.getLegacyResultId());
        if (result.isEmpty() || result.get().getTopicScoresJson() == null) return;
        try {
            Map<String, Double> topicScores = objectMapper.readValue(result.get().getTopicScoresJson(),
                    new TypeReference<Map<String, Double>>() {});
            topicScores.forEach((topic, score) -> {
                String normalizedTopic = topic == null || topic.isBlank() ? "General" : topic;
                aggregates.computeIfAbsent(normalizedTopic, ignored -> new TopicAggregate()).addScore(score);
            });
        } catch (Exception exception) {
            log.warn("Could not read legacy topic scores for result {}", result.get().getId(), exception);
        }
    }

    private String inferQuestionType(GeneratedQuestion question) {
        try {
            List<String> options = objectMapper.readValue(question.getOptionsJson(), new TypeReference<List<String>>() {});
            if (options.size() <= 1) return "Numerical";
            return question.getAnswer() != null && question.getAnswer().contains(",") ? "MSQ" : "MCQ";
        } catch (Exception exception) {
            return null;
        }
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static class TopicAggregate {
        private int total;
        private int correct;

        private void add(boolean isCorrect) {
            total++;
            if (isCorrect) correct++;
        }

        private void addScore(Double score) {
            total++;
            if (score != null && score >= 50.0) correct++;
        }
    }

    private static class ActivityAggregate {
        private int tests;
        private int questions;
        private int evaluations;
        private int flashcards;

        private void addTest(int questionCount, Integer timeTaken) {
            tests++;
            questions += questionCount;
        }
    }

    private int calculateCorrectCount(UUID attemptId) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        if (!answers.isEmpty()) {
            return (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        }
        return testAttemptRepository.findById(attemptId)
            .flatMap(attempt -> attempt.getLegacyResultId() == null
                ? Optional.empty() : testResultRepository.findById(attempt.getLegacyResultId()))
            .map(TestResult::getCorrectQuestions)
            .orElse(0);
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
            .findByUserIdAndTopicAndExamTypeAndQuestionTypeAndDifficulty(
                attempt.getUser().getId(), topic, examType, questionType, difficulty)
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
        LearningStatistics stats = learningStatisticsRepository
            .findByUserIdAndTopicAndExamTypeAndQuestionTypeAndDifficulty(
                submission.getUser().getId(), topic, null, null, null)
                .orElseGet(() -> LearningStatistics.builder()
                        .user(submission.getUser())
                        .topic(topic)
                        .build());
        stats.setTotalQuestions((stats.getTotalQuestions() == null ? 0 : stats.getTotalQuestions()) + 1);
        stats.setWrongQuestions((stats.getWrongQuestions() == null ? 0 : stats.getWrongQuestions()) + 1);
        stats.setAveragePercentage(0.0);
        stats.setUpdatedAt(OffsetDateTime.now());
        learningStatisticsRepository.save(stats);
    }
}
