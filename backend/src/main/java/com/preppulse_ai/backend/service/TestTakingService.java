package com.preppulse_ai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.*;
import com.preppulse_ai.backend.exception.ResourceNotFoundException;
import com.preppulse_ai.backend.repository.AttemptAnswerRepository;
import com.preppulse_ai.backend.repository.GeneratedQuestionRepository;
import com.preppulse_ai.backend.repository.GeneratedTestRepository;
import com.preppulse_ai.backend.repository.TestAttemptRepository;
import com.preppulse_ai.backend.repository.TestResultRepository;
import com.preppulse_ai.backend.service.HistoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestTakingService {

    private final TestAttemptRepository testAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final GeneratedTestRepository generatedTestRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final TestResultRepository testResultRepository;
    private final HistoryAnalyticsService historyAnalyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Default test duration: 60 minutes (3600 seconds)
    private static final int DEFAULT_TEST_DURATION = 3600;
    private static final double DEFAULT_CORRECT_MARKS = 4.0;
    private static final double DEFAULT_WRONG_MARKS = -1.0;
    private static final double DEFAULT_UNATTEMPTED_MARKS = 0.0;

    @Transactional
    public StartTestResponse startTest(UUID testId, User user) {
        // Check if there's an existing unsubmitted attempt
        Optional<TestAttempt> existingAttempt = testAttemptRepository
                .findByUserIdAndTestIdAndSubmittedFalse(user.getId(), testId);

        if (existingAttempt.isPresent()) {
            log.info("Resuming existing attempt for user: {}, test: {}", user.getId(), testId);
            return buildStartTestResponse(existingAttempt.get());
        }

        // Verify test exists and belongs to user
        GeneratedTest test = generatedTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (!test.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only attempt your own tests");
        }

        // Create new attempt
        TestAttempt attempt = TestAttempt.builder()
                .user(user)
                .test(test)
                .startTime(OffsetDateTime.now())
                .submitted(false)
                .tabSwitchCount(0)
                .build();

        attempt = testAttemptRepository.save(attempt);

        log.info("Started new test attempt for user: {}, test: {}, attempt: {}", user.getId(), testId, attempt.getId());

        return buildStartTestResponse(attempt);
    }

    @Transactional
    public SaveAnswerResponse saveAnswer(SaveAnswerRequest request, User user) {
        TestAttempt attempt = testAttemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        // Ownership validation
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only save answers for your own attempts");
        }

        if (attempt.getSubmitted()) {
            throw new IllegalArgumentException("Cannot save answers for submitted test");
        }

        GeneratedQuestion question = generatedQuestionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // Check if question belongs to the test
        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new IllegalArgumentException("Question does not belong to this test");
        }

        String selectedAnswer = request.getSelectedAnswer() == null ? null : request.getSelectedAnswer().trim();
        if (selectedAnswer == null || selectedAnswer.isEmpty()) {
            attemptAnswerRepository.findByAttemptId(request.getAttemptId()).stream()
                .filter(a -> a.getQuestion().getId().equals(request.getQuestionId()))
                .findFirst()
                .ifPresent(attemptAnswerRepository::delete);

            return SaveAnswerResponse.builder()
                .success(true)
                .message("Answer cleared successfully")
                .build();
        }

        // Find or create attempt answer
        Optional<AttemptAnswer> existingAnswer = attemptAnswerRepository.findByAttemptId(request.getAttemptId())
                .stream()
                .filter(a -> a.getQuestion().getId().equals(request.getQuestionId()))
                .findFirst();

        AttemptAnswer answer;
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
            answer.setSelectedAnswer(selectedAnswer);
            answer.setAnsweredAt(OffsetDateTime.now());
        } else {
            answer = AttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedAnswer(selectedAnswer)
                    .answeredAt(OffsetDateTime.now())
                    .build();
        }

        attemptAnswerRepository.save(answer);

        log.debug("Saved answer for attempt: {}, question: {}", request.getAttemptId(), request.getQuestionId());

        return SaveAnswerResponse.builder()
                .success(true)
                .message("Answer saved successfully")
                .build();
    }

    @Transactional
    public TestResultResponse submitTest(SubmitTestRequest request, User user) {
        TestAttempt attempt = testAttemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        // Ownership validation
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only submit your own attempts");
        }

        if (attempt.getSubmitted()) {
            throw new IllegalArgumentException("Test already submitted");
        }

        // Update tab switch count
        if (request.getTabSwitchCount() != null) {
            attempt.setTabSwitchCount(request.getTabSwitchCount());
        }

        // Set end time
        attempt.setEndTime(OffsetDateTime.now());
        attempt.setSubmitted(true);

        // Calculate time taken
        long timeTakenSeconds = ChronoUnit.SECONDS.between(attempt.getStartTime(), attempt.getEndTime());
        attempt.setTimeTaken((int) timeTakenSeconds);

        // Evaluate the test
        evaluateAttempt(attempt);

        // Save attempt
        attempt = testAttemptRepository.save(attempt);
        historyAnalyticsService.recordTestAttempt(attempt);

        log.info("Submitted test attempt: {}, user: {}, score: {}", attempt.getId(), user.getId(), attempt.getScore());

        return buildTestResultResponse(attempt);
    }

    public TestDto getTest(UUID testId, User user) {
        GeneratedTest test = generatedTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (!test.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only access your own tests");
        }

        return buildTestDto(test);
    }

    public TestResultResponse getTestResult(UUID attemptId, User user) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));

        // Ownership validation
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only view your own results");
        }

        return buildTestResultResponse(attempt);
    }

    public List<QuestionReviewResponse> getTestReview(UUID attemptId, User user) {
        return getTestResult(attemptId, user).getQuestionReviews();
    }

    private void evaluateAttempt(TestAttempt attempt) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        List<GeneratedQuestion> allQuestions = generatedQuestionRepository.findAllByTestId(attempt.getTest().getId());

        int correctCount = 0;
        int wrongCount = 0;
        int skippedCount = 0;
        double totalScore = 0;
        double correctMarks = resolveCorrectMarks(attempt.getTest());
        double wrongMarks = resolveWrongMarks(attempt.getTest());
        double unattemptedMarks = resolveUnattemptedMarks(attempt.getTest());
        double maxPossibleScore = Math.max(1.0, allQuestions.size() * Math.max(correctMarks, 0.0));

        // Evaluate each answer
        for (GeneratedQuestion question : allQuestions) {
            Optional<AttemptAnswer> answerOpt = answers.stream()
                    .filter(a -> a.getQuestion().getId().equals(question.getId()))
                    .findFirst();

            if (answerOpt.isPresent()) {
                AttemptAnswer answer = answerOpt.get();
                String userAnswer = answer.getSelectedAnswer();

                // Determine correctness
                boolean isCorrect = checkAnswer(question, userAnswer);
                answer.setIsCorrect(isCorrect);

                // Calculate marks
                double marksObtained;
                if (isCorrect) {
                    marksObtained = correctMarks;
                    correctCount++;
                } else {
                    marksObtained = wrongMarks;
                    wrongCount++;
                }
                answer.setMarksObtained(marksObtained);
                totalScore += marksObtained;

                attemptAnswerRepository.save(answer);
            } else {
                skippedCount++;
                totalScore += unattemptedMarks;
            }
        }

        // Calculate percentage
        double percentage = (totalScore / maxPossibleScore) * 100;
        if (percentage < 0) percentage = 0;

        attempt.setScore(totalScore);
        attempt.setPercentage(percentage);

        // Also save to test_results for dashboard compatibility
        saveTestResult(attempt, correctCount, allQuestions.size());
    }

    private boolean checkAnswer(GeneratedQuestion question, String userAnswer) {
        String correctAnswer = question.getAnswer();
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        try {
            String questionType = inferQuestionType(question);

            if ("MSQ".equals(questionType)) {
                Set<String> userAnswers = Arrays.stream(userAnswer.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
                Set<String> correctAnswers = Arrays.stream(correctAnswer.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
                return userAnswers.equals(correctAnswers);
            }

            if ("Numerical".equals(questionType)) {
                try {
                    double userNum = Double.parseDouble(userAnswer.trim());
                    double correctNum = Double.parseDouble(correctAnswer.trim());
                    return Math.abs(userNum - correctNum) < 0.01; // Allow small tolerance
                } catch (NumberFormatException e) {
                    return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
                }
            }

            return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
        } catch (Exception e) {
            log.error("Error checking answer", e);
            return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
        }
    }

    private void saveTestResult(TestAttempt attempt, int correctCount, int totalQuestions) {
        // Calculate topic-wise scores
        Map<String, Double> topicScores = calculateTopicScores(attempt);

        try {
            TestResult testResult = TestResult.builder()
                    .user(attempt.getUser())
                    .test(attempt.getTest())
                    .score(attempt.getScore())
                    .correctQuestions(correctCount)
                    .totalQuestions(totalQuestions)
                    .topicScoresJson(objectMapper.writeValueAsString(topicScores))
                    .completedAt(attempt.getEndTime())
                    .build();

            testResultRepository.save(testResult);
        } catch (Exception e) {
            log.error("Error saving test result", e);
        }
    }

    private Map<String, Double> calculateTopicScores(TestAttempt attempt) {
        Map<String, TopicStats> topicStats = new HashMap<>();
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());

        for (AttemptAnswer answer : answers) {
            String topic = answer.getQuestion().getTopic();
            if (topic == null || topic.trim().isEmpty()) {
                topic = "General";
            }

            TopicStats stats = topicStats.computeIfAbsent(topic, k -> new TopicStats());
            stats.total++;

            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                stats.correct++;
            }
        }

        Map<String, Double> topicScores = new HashMap<>();
        for (Map.Entry<String, TopicStats> entry : topicStats.entrySet()) {
            double percentage = (entry.getValue().correct * 100.0) / entry.getValue().total;
            topicScores.put(entry.getKey(), percentage);
        }

        return topicScores;
    }

    private StartTestResponse buildStartTestResponse(TestAttempt attempt) {
        List<GeneratedQuestion> questions = generatedQuestionRepository.findAllByTestId(attempt.getTest().getId());

        List<QuestionDto> questionDtos = questions.stream()
                .map(q -> QuestionDto.builder()
                        .id(q.getId())
                        .question(q.getQuestion())
                        .options(parseOptions(q.getOptionsJson()))
                        .correctAnswer(q.getAnswer())
                .questionType(inferQuestionType(q))
                        .explanation(q.getExplanation())
                        .topic(q.getTopic())
                        .difficulty(q.getDifficulty())
                        .build())
                .collect(Collectors.toList());

        // Get existing answers for resume
        List<AttemptAnswer> existingAnswers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        Map<UUID, String> answerMap = existingAnswers.stream()
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        AttemptAnswer::getSelectedAnswer
                ));

        return StartTestResponse.builder()
                .attemptId(attempt.getId())
                .testId(attempt.getTest().getId())
                .examType(attempt.getTest().getExamType())
                .questionType(attempt.getTest().getQuestionType())
                .difficulty(attempt.getTest().getDifficulty())
                .totalQuestions(attempt.getTest().getTotalQuestions())
                .startTime(attempt.getStartTime())
                .questions(questionDtos)
            .savedAnswers(answerMap)
                .duration(DEFAULT_TEST_DURATION)
                .build();
    }

        private TestDto buildTestDto(GeneratedTest test) {
        List<GeneratedQuestion> questions = generatedQuestionRepository.findAllByTestId(test.getId());

        List<QuestionDto> questionDtos = questions.stream()
            .map(q -> QuestionDto.builder()
                .id(q.getId())
                .question(q.getQuestion())
                .options(parseOptions(q.getOptionsJson()))
                .correctAnswer(q.getAnswer())
                .questionType(inferQuestionType(q))
                .explanation(q.getExplanation())
                .topic(q.getTopic())
                .difficulty(q.getDifficulty())
                .build())
            .collect(Collectors.toList());

        return TestDto.builder()
            .id(test.getId())
            .sourceMaterialId(test.getSourceMaterial() != null ? test.getSourceMaterial().getId() : null)
            .examType(test.getExamType())
            .questionType(test.getQuestionType())
            .difficulty(test.getDifficulty())
            .totalQuestions(test.getTotalQuestions())
            .questions(questionDtos)
            .build();
        }

    private TestResultResponse buildTestResultResponse(TestAttempt attempt) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
        List<GeneratedQuestion> allQuestions = generatedQuestionRepository.findAllByTestId(attempt.getTest().getId());

        int attempted = answers.size();
        int correct = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        int wrong = attempted - correct;
        int skipped = allQuestions.size() - attempted;

        Map<String, Double> topicScores = calculateTopicScores(attempt);

        List<QuestionReviewResponse> questionReviews = new ArrayList<>();
        int questionNumber = 1;

        for (GeneratedQuestion question : allQuestions) {
            Optional<AttemptAnswer> answerOpt = answers.stream()
                    .filter(a -> a.getQuestion().getId().equals(question.getId()))
                    .findFirst();

            QuestionReviewResponse.QuestionReviewResponseBuilder builder = QuestionReviewResponse.builder()
                    .questionId(question.getId())
                    .questionNumber(questionNumber++)
                    .question(question.getQuestion())
                    .options(parseOptions(question.getOptionsJson()))
                    .correctAnswer(question.getAnswer())
                    .questionType(inferQuestionType(question))
                    .explanation(question.getExplanation())
                    .topic(question.getTopic())
                    .difficulty(question.getDifficulty());

            if (answerOpt.isPresent()) {
                AttemptAnswer answer = answerOpt.get();
                builder.userAnswer(answer.getSelectedAnswer());
                builder.marksObtained(answer.getMarksObtained());
                
                if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                    builder.status("CORRECT");
                } else {
                    builder.status("WRONG");
                }
            } else {
                builder.status("SKIPPED");
                builder.marksObtained(0.0);
            }

            questionReviews.add(builder.build());
        }

        return TestResultResponse.builder()
                .attemptId(attempt.getId())
                .testId(attempt.getTest().getId())
                .examType(attempt.getTest().getExamType())
                .score(attempt.getScore())
                .percentage(attempt.getPercentage())
                .timeTaken(attempt.getTimeTaken())
                .attempted(attempted)
                .correct(correct)
                .wrong(wrong)
                .skipped(skipped)
                .startTime(attempt.getStartTime())
                .endTime(attempt.getEndTime())
                .tabSwitchCount(attempt.getTabSwitchCount())
                .topicScores(topicScores)
                .questionReviews(questionReviews)
                .build();
    }

    private String inferQuestionType(GeneratedQuestion question) {
        List<String> options = parseOptions(question.getOptionsJson());
        if (options.size() <= 1) {
            return "Numerical";
        }

        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (answer.contains(",")) {
            return "MSQ";
        }

        return "MCQ";
    }

    private double resolveCorrectMarks(GeneratedTest test) {
        return test.getCorrectMarks() != null ? test.getCorrectMarks() : DEFAULT_CORRECT_MARKS;
    }

    private double resolveWrongMarks(GeneratedTest test) {
        return test.getWrongMarks() != null ? test.getWrongMarks() : DEFAULT_WRONG_MARKS;
    }

    private double resolveUnattemptedMarks(GeneratedTest test) {
        return test.getUnattemptedMarks() != null ? test.getUnattemptedMarks() : DEFAULT_UNATTEMPTED_MARKS;
    }

    private List<String> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Error parsing options JSON", e);
            return new ArrayList<>();
        }
    }

    private static class TopicStats {
        int total = 0;
        int correct = 0;
    }
}
