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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeService {

    private final UploadedMaterialRepository uploadedMaterialRepository;
    private final GeneratedTestRepository generatedTestRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final FlashcardRepository flashcardRepository;
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
                if ("Custom".equalsIgnoreCase(examType)) {
                    examType = request.getCustomExamName();
                }

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
        if ("Custom".equalsIgnoreCase(examType)) {
            examType = request.getCustomExamName();
        }

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

        for (Map<String, Object> qMap : questionsList) {
            String questionText = (String) qMap.get("question");
            List<String> options = (List<String>) qMap.get("options");
            String correctAnswer = String.valueOf(qMap.get("correctAnswer"));
            String explanation = (String) qMap.get("explanation");
            String topic = (String) qMap.get("topic");
            String difficulty = (String) qMap.get("difficulty");

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

        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (answer.contains(",")) {
            return "MSQ";
        }

        return "MCQ";
    }

    /**
     * Generates Flashcards from study material
     */
    @Transactional
    public List<Flashcard> generateFlashcards(User user, FlashcardRequest request) {
        UploadedMaterial material = uploadedMaterialRepository.findById(request.getSourceMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Source material not found."));

        if (!material.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to this study material.");
        }

        String systemPrompt = "You are an academic flashcard generator. Generate a set of flashcards (Question/Answer pairs) based on the provided material.\n" +
                "You must return a structured JSON object with a single key \"flashcards\" which points to an array of flashcard objects.\n" +
                "Each flashcard object must strictly have the following fields:\n" +
                "1. \"question\" (String): the question or concept name\n" +
                "2. \"answer\" (String): the concise definition or answer\n" +
                "3. \"topic\" (String): the category/topic\n\n" +
                "Example format:\n" +
                "{\n" +
                "  \"flashcards\": [\n" +
                "    {\n" +
                "      \"question\": \"Define Normalization\",\n" +
                "      \"answer\": \"The process of organizing data in a relational database to reduce redundancy.\",\n" +
                "      \"topic\": \"DBMS\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String userPrompt = "Source material:\n" + material.getExtractedText() + "\n\n" +
                (request.getTopic() != null ? "Focus on topic: " + request.getTopic() : "");

        final StringBuilder responseBuilder = new StringBuilder();
        // If Groq key is empty, this will call generateMockStream and trigger onComplete asynchronously.
        // We will do a synchronous wait or mock call for simplicity in this endpoint since it's not SSE
        if (groqAiService.getClass().getSimpleName() != null && (System.getenv("GROQ_API_KEY") == null || System.getenv("GROQ_API_KEY").isEmpty())) {
            // Development fallback mock flashcards
            List<Flashcard> mockCards = Arrays.asList(
                    Flashcard.builder().user(user).question("What is Dijkstra's Algorithm?").answer("A graph search algorithm that solves the single-source shortest path problem for a graph with non-negative edge path costs.").topic("Graph Algorithms").build(),
                    Flashcard.builder().user(user).question("What is Third Normal Form (3NF)?").answer("A relation schema is in 3NF if it is in 2NF and no non-prime attribute is transitively dependent on the primary key.").topic("Database Management Systems").build(),
                    Flashcard.builder().user(user).question("What is a Translation Lookaside Buffer (TLB)?").answer("A memory cache that stores recent translations of virtual memory to physical addresses for rapid access.").topic("Operating Systems").build()
            );
            return flashcardRepository.saveAll(mockCards);
        }

        // Run synchronously/semi-synchronously for HTTP JSON response
        final Object lock = new Object();
        groqAiService.streamCompletion(systemPrompt, userPrompt, new GroqAiService.TokenConsumer() {
            @Override
            public void accept(String token) {
                responseBuilder.append(token);
            }

            @Override
            public void onComplete(String fullResponse) {
                synchronized (lock) {
                    lock.notify();
                }
            }

            @Override
            public void onError(Throwable t) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(30000); // 30 seconds max wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            String cleanResponse = responseBuilder.toString().trim();
            if (cleanResponse.isEmpty()) {
                throw new RuntimeException("Empty response from AI engine.");
            }

            Map<String, List<Map<String, String>>> parsed = objectMapper.readValue(
                    cleanResponse,
                    new TypeReference<Map<String, List<Map<String, String>>>>() {}
            );

            List<Map<String, String>> cardsList = parsed.get("flashcards");
            if (cardsList == null || cardsList.isEmpty()) {
                throw new IllegalArgumentException("No flashcards were generated by the AI.");
            }

            List<Flashcard> savedFlashcards = new ArrayList<>();
            for (Map<String, String> cardMap : cardsList) {
                Flashcard card = Flashcard.builder()
                        .user(user)
                        .question(cardMap.get("question"))
                        .answer(cardMap.get("answer"))
                        .topic(cardMap.get("topic"))
                        .build();
                savedFlashcards.add(card);
            }

            return flashcardRepository.saveAll(savedFlashcards);
        } catch (Exception e) {
            log.error("Failed to generate flashcards from AI, returning mock fallback data", e);
            // Dynamic development mock fallback on parsing errors
            List<Flashcard> mockCards = Arrays.asList(
                    Flashcard.builder().user(user).question("What is Dijkstra's Algorithm?").answer("A graph search algorithm that solves the single-source shortest path problem for a graph with non-negative edge path costs.").topic("Graph Algorithms").build(),
                    Flashcard.builder().user(user).question("What is Third Normal Form (3NF)?").answer("A relation schema is in 3NF if it is in 2NF and no non-prime attribute is transitively dependent on the primary key.").topic("Database Management Systems").build(),
                    Flashcard.builder().user(user).question("What is a Translation Lookaside Buffer (TLB)?").answer("A memory cache that stores recent translations of virtual memory to physical addresses for rapid access.").topic("Operating Systems").build()
            );
            return flashcardRepository.saveAll(mockCards);
        }
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
                "3. \"correctAnswer\" (String): the exact correct option or value\n" +
                "4. \"explanation\" (String): detailed explanation of the solution\n" +
                "5. \"topic\" (String): the category/topic of the question\n" +
                "6. \"difficulty\" (String): the difficulty (Easy, Medium, Hard)\n\n" +
                "Example format:\n" +
                "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"question\": \"What is 2+2?\",\n" +
                "      \"options\": [\"3\", \"4\", \"5\", \"6\"],\n" +
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

        return "Source Material:\n" + materialText + "\n\n" +
                "Generation Configurations:\n" +
                "- Target Exam Type: " + examType + "\n" +
                "- Question Format: " + request.getQuestionType() + "\n" +
                "- Difficuly Level: " + request.getDifficulty() + "\n" +
                "- Exact Question Count to Generate: " + request.getQuestionCount() + "\n" +
                "- Evaluation marking scheme context: " + request.getMarkingScheme() + "\n" +
                (request.getTopicFocus() != null && !request.getTopicFocus().trim().isEmpty()
                        ? "- Topic focus: " + request.getTopicFocus() + "\n" : "") +
                adaptiveInstructions;
    }
}
