package com.preppulse_ai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.*;
import com.preppulse_ai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeService {

    private final UploadedMaterialRepository uploadedMaterialRepository;
    private final GeneratedTestRepository generatedTestRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final TestResultRepository testResultRepository;

    private final SupabaseStorageService supabaseStorageService;
    private final InputValidationService inputValidationService;
    private final AdaptiveLearningService adaptiveLearningService;
    private final GroqAiService groqAiService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Extracts text from PDF bytes using PDFBox
     */
    public String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Calculates SHA-256 hash of string content
     */
    public String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Uploads notes text or PDF files.
     * Extracts text, calculates hash, validates content, uploads to Supabase, and saves metadata.
     */
    @Transactional
    public UploadResponse uploadMaterial(User user, MultipartFile file, String textContent) throws IOException {
        String fileName;
        String contentType;
        long fileSize;
        byte[] fileBytes;
        String extractedText;

        if (file != null && !file.isEmpty()) {
            // PDF Upload flow
            fileName = file.getOriginalFilename();
            contentType = file.getContentType();
            fileSize = file.getSize();

            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                throw new IllegalArgumentException("Only PDF files are allowed.");
            }
            if (fileSize > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size must not exceed 5 MB.");
            }

            fileBytes = file.getBytes();
            extractedText = extractTextFromPdf(fileBytes);
        } else if (textContent != null && !textContent.trim().isEmpty()) {
            // Pasted notes flow
            fileName = "pasted_notes_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
            contentType = "text/plain";
            fileBytes = textContent.getBytes(StandardCharsets.UTF_8);
            fileSize = fileBytes.length;
            extractedText = textContent;
        } else {
            throw new IllegalArgumentException("No file or text content provided.");
        }

        // Quality Validation (Gibberish & academic checking)
        inputValidationService.validateInputQuality(extractedText);
        int score = inputValidationService.calculateAcademicConfidenceScore(extractedText);

        // Content Hashing for Cost Optimization Cache
        String contentHash = calculateHash(extractedText);

        // Upload to Supabase Storage (directly from memory, without writing to disk)
        String fileUrl = supabaseStorageService.uploadFile(user.getId(), fileName, fileBytes, contentType);

        // Save metadata
        UploadedMaterial material = UploadedMaterial.builder()
                .user(user)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(contentType)
                .fileSize(fileSize)
                .contentHash(contentHash)
                .extractedText(extractedText)
                .build();

        uploadedMaterialRepository.save(material);

        return UploadResponse.builder()
                .id(material.getId())
                .fileName(material.getFileName())
                .fileUrl(material.getFileUrl())
                .academicConfidenceScore(score)
                .extractedText(material.getExtractedText())
            .topics(detectTopics(extractedText))
                .build();
    }

    /**
     * Triggers test generation, streaming tokens to client via SseEmitter.
     * First checks cache; if match found, clones test and completes immediately.
     */
    public SseEmitter generateTestStream(User user, TestGenerationRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10 minutes timeout

        executorService.submit(() -> {
            try {
                // Fetch material
                UploadedMaterial material = uploadedMaterialRepository.findById(request.getSourceMaterialId())
                        .orElseThrow(() -> new IllegalArgumentException("Source material not found."));

                // Verify Ownership
                if (!material.getUser().getId().equals(user.getId())) {
                    throw new SecurityException("Unauthorized access to this study material.");
                }

                String contentHash = material.getContentHash();
                String examType = request.getExamType();
                validateExamContext(material.getExtractedText(), examType);

                // AI Cost Optimization: Check database cache
                List<GeneratedTest> similarTests = generatedTestRepository.findSimilarTests(
                        contentHash,
                        examType,
                        request.getDifficulty(),
                        request.getQuestionType()
                );

                if (!similarTests.isEmpty()) {
                    log.info("Cost Optimization: Matching test cached in DB! Cloning test questions...");
                    emitter.send(SseEmitter.event().name("info").data("Caching match found! Reusing existing test resources."));
                    
                    GeneratedTest cachedTest = similarTests.get(0);
                    GeneratedTest clonedTest = cloneTestForUser(cachedTest, user, material);
                    TestDto responseDto = buildTestDto(clonedTest);

                    // Push details immediately
                    emitter.send(SseEmitter.event().name("complete").data(objectMapper.writeValueAsString(responseDto)));
                    emitter.complete();
                    return;
                }

                // No matching test, call Groq completion with SSE
                emitter.send(SseEmitter.event().name("info").data("Initializing generation engine..."));
                String systemPrompt = buildSystemPrompt();
                String userPrompt = buildUserPrompt(material.getExtractedText(), examType, request, user.getId());

                groqAiService.streamCompletion(systemPrompt, userPrompt, new GroqAiService.TokenConsumer() {
                    @Override
                    public void accept(String token) {
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (Exception ex) {
                            log.error("Failed to send token event to client", ex);
                        }
                    }

                    @Override
                    public void onComplete(String fullResponse) {
                        try {
                            log.info("AI completed generation. Persisting questions in DB...");
                            GeneratedTest savedTest = saveGeneratedTest(user, material, request, fullResponse);
                            TestDto responseDto = buildTestDto(savedTest);

                            emitter.send(SseEmitter.event().name("complete").data(objectMapper.writeValueAsString(responseDto)));
                            emitter.complete();
                        } catch (Exception ex) {
                            log.error("Failed to persist questions", ex);
                            try {
                                emitter.send(SseEmitter.event().name("error").data("Failed to parse and save questions: " + ex.getMessage()));
                            } catch (Exception e) {
                                // ignore
                            }
                            emitter.completeWithError(ex);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        try {
                            emitter.send(SseEmitter.event().name("error").data("Generation error: " + t.getMessage()));
                        } catch (Exception e) {
                            // ignore
                        }
                        emitter.completeWithError(t);
                    }
                });

            } catch (Exception e) {
                log.error("Error initiating stream generation:", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("Failed: " + e.getMessage()));
                } catch (Exception ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Transactional
    protected GeneratedTest cloneTestForUser(GeneratedTest cachedTest, User user, UploadedMaterial material) {
        GeneratedTest clonedTest = GeneratedTest.builder()
                .user(user)
                .sourceMaterial(material)
                .examType(cachedTest.getExamType())
                .questionType(cachedTest.getQuestionType())
                .difficulty(cachedTest.getDifficulty())
                .totalQuestions(cachedTest.getTotalQuestions())
            .correctMarks(cachedTest.getCorrectMarks())
            .wrongMarks(cachedTest.getWrongMarks())
            .unattemptedMarks(cachedTest.getUnattemptedMarks())
                .build();

        generatedTestRepository.save(clonedTest);

        List<GeneratedQuestion> questions = generatedQuestionRepository.findAllByTestId(cachedTest.getId());
        for (GeneratedQuestion q : questions) {
            GeneratedQuestion clonedQ = GeneratedQuestion.builder()
                    .test(clonedTest)
                    .question(q.getQuestion())
                    .optionsJson(q.getOptionsJson())
                    .answer(q.getAnswer())
                    .questionType(q.getQuestionType())
                    .explanation(q.getExplanation())
                    .topic(q.getTopic())
                    .difficulty(q.getDifficulty())
                    .build();
            generatedQuestionRepository.save(clonedQ);
        }
        return clonedTest;
    }

    @Transactional
    protected GeneratedTest saveGeneratedTest(User user, UploadedMaterial material, TestGenerationRequest request, String rawJson) throws Exception {
        // Parse Groq JSON
        Map<String, List<Map<String, Object>>> parsed = objectMapper.readValue(
                rawJson,
                new TypeReference<Map<String, List<Map<String, Object>>>>() {}
        );

        List<Map<String, Object>> questionsList = parsed.get("questions");
        if (questionsList == null || questionsList.isEmpty()) {
            throw new IllegalArgumentException("No questions were generated by the AI.");
        }

        String examType = request.getExamType();

        GeneratedTest test = GeneratedTest.builder()
                .user(user)
                .sourceMaterial(material)
                .examType(examType)
                .questionType(request.getQuestionType())
                .difficulty(request.getDifficulty())
                .totalQuestions(questionsList.size())
            .correctMarks(parseMarkingScheme(request.getMarkingScheme())[0])
            .wrongMarks(parseMarkingScheme(request.getMarkingScheme())[1])
            .unattemptedMarks(parseMarkingScheme(request.getMarkingScheme())[2])
                .build();

        generatedTestRepository.save(test);

        validateGeneratedQuestions(questionsList, request);

        for (int questionIndex = 0; questionIndex < questionsList.size(); questionIndex++) {
            Map<String, Object> qMap = questionsList.get(questionIndex);
            String questionText = (String) qMap.get("question");
            List<String> options = (List<String>) qMap.get("options");
            String correctAnswer = String.valueOf(qMap.get("correctAnswer"));
            String explanation = (String) qMap.get("explanation");
            String topic = (String) qMap.get("topic");
            String difficulty = (String) qMap.get("difficulty");
            String generatedQuestionType = resolveQuestionType(qMap, request.getQuestionType(), questionIndex);

            if (difficulty == null) {
                difficulty = request.getDifficulty();
            }

            GeneratedQuestion question = GeneratedQuestion.builder()
                    .test(test)
                    .question(questionText)
                    .optionsJson(objectMapper.writeValueAsString(options))
                    .answer(correctAnswer)
                    .explanation(explanation)
                    .topic(topic)
                    .difficulty(difficulty)
                    .questionType(generatedQuestionType)
                    .build();

            generatedQuestionRepository.save(question);
        }

        return test;
    }

    private TestDto buildTestDto(GeneratedTest test) {
        List<GeneratedQuestion> questions = generatedQuestionRepository.findAllByTestId(test.getId());
        List<QuestionDto> questionDtos = new ArrayList<>();

        for (GeneratedQuestion q : questions) {
            List<String> options = new ArrayList<>();
            try {
                options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.error("Failed to parse options JSON for question ID: {}", q.getId(), e);
            }

            questionDtos.add(QuestionDto.builder()
                    .id(q.getId())
                    .question(q.getQuestion())
                    .options(options)
                    .correctAnswer(q.getAnswer())
                    .questionType(inferQuestionType(q))
                    .explanation(q.getExplanation())
                    .topic(q.getTopic())
                    .difficulty(q.getDifficulty())
                    .build());
        }

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

    private double[] parseMarkingScheme(String markingScheme) {
        double[] defaults = new double[] { 4.0, -1.0, 0.0 };

        if (markingScheme == null || markingScheme.trim().isEmpty()) {
            return defaults;
        }

        List<Double> values = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[-+]?[0-9]*\\.?[0-9]+")
                .matcher(markingScheme.replace(',', '.'));
        while (matcher.find()) {
            try {
                values.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
                // ignore malformed fragments and fall back to defaults
            }
        }

        if (!values.isEmpty()) {
            defaults[0] = values.get(0);
        }
        if (values.size() > 1) {
            defaults[1] = values.get(1);
        }
        if (values.size() > 2) {
            defaults[2] = values.get(2);
        }

        return defaults;
    }

    private List<String> detectTopics(String text) {
        Map<String, Integer> topicScores = new LinkedHashMap<>();
        Set<String> stopWords = Set.of("about", "after", "again", "against", "being", "between", "could", "from", "have", "into", "more", "other", "over", "should", "their", "there", "these", "those", "through", "under", "using", "which", "while", "where", "what", "when", "with", "would");

        java.util.regex.Matcher acronymMatcher = java.util.regex.Pattern.compile("\\b[A-Z][A-Z0-9-]{1,7}\\b").matcher(text);
        while (acronymMatcher.find()) {
            topicScores.merge(acronymMatcher.group(), 3, Integer::sum);
        }

        java.util.regex.Matcher phraseMatcher = java.util.regex.Pattern.compile("\\b(?:[A-Z][a-z]+)(?:\\s+[A-Z][a-z]+){1,2}\\b").matcher(text);
        while (phraseMatcher.find()) {
            String phrase = phraseMatcher.group().trim();
            if (!phrase.contains("The ") && !phrase.contains("What ")) {
                topicScores.merge(phrase, 2, Integer::sum);
            }
        }

        java.util.regex.Matcher wordMatcher = java.util.regex.Pattern.compile("\\b[A-Za-z][A-Za-z-]{4,}\\b").matcher(text.toLowerCase(Locale.ROOT));
        while (wordMatcher.find()) {
            String word = wordMatcher.group();
            if (!stopWords.contains(word)) {
                topicScores.merge(word, 1, Integer::sum);
            }
        }

        return topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(12)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void validateExamContext(String materialText, String examType) {
        if (examType == null || !Set.of("GATE", "UPSC", "University Exam", "SSC", "Banking").contains(examType)) {
            throw new IllegalArgumentException("Please select one of the supported exam contexts.");
        }
        if ("University Exam".equals(examType)) {
            return;
        }

        String normalized = materialText.toLowerCase(Locale.ROOT);
        Map<String, List<String>> signals = Map.of(
                "GATE", List.of("algorithm", "computer", "engineering", "circuit", "operating system", "database", "network", "calculus", "probability", "programming"),
                "UPSC", List.of("constitution", "polity", "governance", "geography", "history", "economy", "international relations", "current affairs", "environment"),
                "SSC", List.of("reasoning", "aptitude", "grammar", "comprehension", "general awareness", "quantitative", "coding-decoding"),
                "Banking", List.of("banking", "finance", "interest", "profit", "loss", "reasoning", "quantitative", "economics", "reserve bank")
        );

        int selectedMatches = signals.get(examType).stream().mapToInt(signal -> normalized.contains(signal) ? 1 : 0).sum();
        int otherMatches = signals.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(examType))
                .mapToInt(entry -> entry.getValue().stream().mapToInt(signal -> normalized.contains(signal) ? 1 : 0).sum())
                .max()
                .orElse(0);

        if (selectedMatches == 0 && (otherMatches >= 2 || detectTopics(materialText).isEmpty())) {
            throw new IllegalArgumentException("The study material does not appear relevant to the selected " + examType + " exam context.");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateGeneratedQuestions(List<Map<String, Object>> questions, TestGenerationRequest request) {
        Set<String> types = new HashSet<>();
        int relevantQuestions = 0;
        for (int index = 0; index < questions.size(); index++) {
            Map<String, Object> question = questions.get(index);
            String type = resolveQuestionType(question, request.getQuestionType(), index);
            question.put("questionType", type);
            List<String> options = question.get("options") instanceof List<?> rawOptions
                    ? rawOptions.stream().map(String::valueOf).collect(Collectors.toList())
                    : new ArrayList<>();
            String correctedAnswer = normalizeCorrectAnswer(
                    String.valueOf(question.get("question")), options, String.valueOf(question.get("correctAnswer"))
            );
            question.put("correctAnswer", correctedAnswer);
            types.add(type);

            if (request.getTopicFocus() != null && !request.getTopicFocus().isBlank()
                    && textContainsTopic(question, request.getTopicFocus())) {
                relevantQuestions++;
            }
        }

        if ("Mixed".equalsIgnoreCase(request.getQuestionType())
                && (questions.size() < 3 || !types.containsAll(Set.of("MCQ", "MSQ", "Numerical")))) {
            throw new IllegalArgumentException("Mixed question format could not produce MCQ, MSQ, and Numerical questions. Please try again.");
        }
        if (request.getTopicFocus() != null && !request.getTopicFocus().isBlank()
                && relevantQuestions < Math.max(1, (questions.size() + 1) / 2)) {
            throw new IllegalArgumentException("The generated questions did not sufficiently match the selected topic focus. Please try again.");
        }
    }

    private String resolveQuestionType(Map<String, Object> question, String requestedType, int index) {
        String generatedType = question.get("questionType") == null ? "" : String.valueOf(question.get("questionType"));
        if (Set.of("MCQ", "MSQ", "Numerical").contains(generatedType)) {
            return generatedType;
        }
        if ("Mixed".equalsIgnoreCase(requestedType)) {
            Object optionsValue = question.get("options");
            int optionCount = optionsValue instanceof List<?> options ? options.size() : 0;
            if (optionCount <= 1) return "Numerical";
            String answer = String.valueOf(question.get("correctAnswer"));
            return answer.contains(",") ? "MSQ" : (index % 3 == 1 ? "MSQ" : "MCQ");
        }
        return requestedType;
    }

    private boolean textContainsTopic(Map<String, Object> question, String topicFocus) {
        Set<String> focusTokens = Arrays.stream(topicFocus.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());
        String questionText = (String.valueOf(question.get("question")) + " " + String.valueOf(question.get("topic"))).toLowerCase(Locale.ROOT);
        return focusTokens.stream().anyMatch(questionText::contains);
    }

    private String normalizeCorrectAnswer(String question, List<String> options, String correctAnswer) {
        Double expected = deriveExpectedNumericAnswer(question);
        if (expected == null) return correctAnswer;
        for (String option : options) {
            Double optionValue = parseNumeric(option);
            if (optionValue != null && Math.abs(optionValue - expected) < 0.0001) {
                return option;
            }
        }
        return correctAnswer;
    }

    private Double deriveExpectedNumericAnswer(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher hitsMisses = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s+hits?\\s+and\\s+(\\d+(?:\\.\\d+)?)\\s+misses?")
                .matcher(normalized);
        if (hitsMisses.find() && (normalized.contains("ratio") || normalized.contains("percentage") || normalized.contains("percent"))) {
            double hits = Double.parseDouble(hitsMisses.group(1));
            double misses = Double.parseDouble(hitsMisses.group(2));
            return hits * 100.0 / (hits + misses);
        }
        return null;
    }

    private Double parseNumeric(String value) {
        try {
            return Double.parseDouble(value.replace("%", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String inferQuestionType(GeneratedQuestion question) {
        List<String> options = new ArrayList<>();
        try {
            options = objectMapper.readValue(question.getOptionsJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to infer question type for question ID: {}", question.getId(), e);
        }

        if (options.size() <= 1) {
            return "Numerical";
        }

        if (question.getQuestionType() != null && !question.getQuestionType().isBlank()) {
            return question.getQuestionType();
        }

        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (answer.contains(",")) {
            return "MSQ";
        }

        return "MCQ";
    }

    /**
     * Submit quiz results
     */
//    @Transactional
//    public TestResult submitTestResult(User user, TestResultSubmitRequest request) {
//        GeneratedTest test = generatedTestRepository.findById(request.getTestId())
//                .orElseThrow(() -> new IllegalArgumentException("Test not found."));
//
//        if (!test.getUser().getId().equals(user.getId())) {
//            throw new SecurityException("Unauthorized access to this test.");
//        }
//
//        try {
//            String topicScoresStr = objectMapper.writeValueAsString(request.getTopicScores());
//
//            TestResult result = TestResult.builder()
//                    .user(user)
//                    .test(test)
//                    .score(request.getScore())
//                    .correctQuestions(request.getCorrectQuestions())
//                    .totalQuestions(request.getTotalQuestions())
//                    .topicScoresJson(topicScoresStr)
//                    .build();
//
//            return testResultRepository.save(result);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to submit test results: " + e.getMessage());
//        }
//    }
    @Transactional
    public TestResultResponse submitTestResult(User user, TestResultSubmitRequest request) {

        GeneratedTest test = generatedTestRepository.findById(request.getTestId())
                .orElseThrow(() -> new IllegalArgumentException("Test not found."));

        if (!test.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to this test.");
        }

        try {
            String topicScoresStr = objectMapper.writeValueAsString(request.getTopicScores());

            TestResult result = TestResult.builder()
                    .user(user)
                    .test(test)
                    .score(request.getScore())
                    .correctQuestions(request.getCorrectQuestions())
                    .totalQuestions(request.getTotalQuestions())
                    .topicScoresJson(topicScoresStr)
                    .build();

            result = testResultRepository.save(result);

            int attempted = request.getTotalQuestions();
            int correct = request.getCorrectQuestions();
            int wrong = attempted - correct;

            return TestResultResponse.builder()
                    .attemptId(result.getId())
                    .testId(test.getId())
                    .examType(test.getExamType())
                    .score(result.getScore())
                    .percentage(
                            request.getTotalQuestions() == 0
                                    ? 0.0
                                    : (request.getCorrectQuestions() * 100.0 / request.getTotalQuestions())
                    )
                    .attempted(attempted)
                    .correct(correct)
                    .wrong(wrong)
                    .skipped(0)
                    .topicScores(request.getTopicScores())
                    .startTime(null)
                    .endTime(result.getCompletedAt())
                    .timeTaken(null)
                    .tabSwitchCount(0)
                    .questionReviews(null)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to submit test results", e);
        }
    }
    /**
     * Returns recommendations including weak topics
     */
    public Map<String, Object> getRecommendations(User user) {
        List<String> weakTopics = adaptiveLearningService.identifyWeakTopics(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("weakTopics", weakTopics);

        List<String> suggestions = new ArrayList<>();
        if (weakTopics.isEmpty()) {
            suggestions.add("Excellent overall performance! Maintain your streak by practicing hard difficulty questions.");
        } else {
            for (String t : weakTopics) {
                suggestions.add("Your understanding of '" + t + "' is below 70%. We recommend generating a targeted practice test focused purely on '" + t + "'.");
            }
        }
        response.put("recommendations", suggestions);
        return response;
    }

    private String buildSystemPrompt() {
        return "You are an academic test generator. You must generate a set of practice questions based strictly on the provided study materials and exam requirements.\n" +
                "You must return a structured JSON object. The JSON object must contain a single key \"questions\" which points to an array of question objects.\n" +
                "Each question object must strictly have the following fields:\n" +
                "1. \"question\" (String): the text of the question\n" +
                "2. \"options\" (Array of Strings): a list of 4 options for MCQs/MSQs. For numerical questions, provide 1 default empty option or list.\n" +
                "3. \"questionType\" (String): exactly MCQ, MSQ, or Numerical\n" +
                "4. \"correctAnswer\" (String): the exact correct option or value\n" +
                "5. \"explanation\" (String): detailed explanation of the solution\n" +
                "6. \"topic\" (String): the category/topic of the question\n" +
                "7. \"difficulty\" (String): the difficulty (Easy, Medium, Hard)\n\n" +
                "Example format:\n" +
                "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"question\": \"What is 2+2?\",\n" +
                "      \"options\": [\"3\", \"4\", \"5\", \"6\"],\n" +
                "      \"questionType\": \"MCQ\",\n" +
                "      \"correctAnswer\": \"4\",\n" +
                "      \"explanation\": \"Because 2 added to 2 equals 4.\",\n" +
                "      \"topic\": \"Arithmetic\",\n" +
                "      \"difficulty\": \"Easy\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private String buildUserPrompt(String materialText, String examType, TestGenerationRequest request, UUID userId) {
        String adaptiveInstructions = adaptiveLearningService.buildAdaptiveInstructions(userId);

        String topicInstruction = request.getTopicFocus() != null && !request.getTopicFocus().trim().isEmpty()
            ? "- Topic focus: " + request.getTopicFocus() + ". At least half of the questions must directly address this topic.\n"
            : "- Topic focus: none. Use the full source material while staying within the selected exam context.\n";
        String questionTypeInstruction = "Mixed".equalsIgnoreCase(request.getQuestionType())
            ? "- Mixed format requirement: include multiple MCQ, MSQ, and Numerical questions in the requested set.\n"
            : "- Every question must use the requested format: " + request.getQuestionType() + ".\n";

        return "Source Material (primary and authoritative source):\n" + materialText + "\n\n" +
                "Generation Configurations:\n" +
            "- Target Exam Type: " + examType + ". Every question must follow this exam's conventions and subject scope.\n" +
                "- Question Format: " + request.getQuestionType() + "\n" +
                "- Difficuly Level: " + request.getDifficulty() + "\n" +
                "- Exact Question Count to Generate: " + request.getQuestionCount() + "\n" +
                "- Evaluation marking scheme context: " + request.getMarkingScheme() + "\n" +
            topicInstruction +
            questionTypeInstruction +
                adaptiveInstructions;
    }
}
