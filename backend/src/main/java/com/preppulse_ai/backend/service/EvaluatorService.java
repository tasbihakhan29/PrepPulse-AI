package com.preppulse_ai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.AnswerEvaluation;
import com.preppulse_ai.backend.entity.AnswerSubmission;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.exception.ResourceNotFoundException;
import com.preppulse_ai.backend.repository.AnswerEvaluationRepository;
import com.preppulse_ai.backend.repository.AnswerSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluatorService {

    private final AnswerSubmissionRepository submissionRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final HistoryAnalyticsService historyAnalyticsService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.api.key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Transactional
    public EvaluationUploadResponse uploadAnswer(
            MultipartFile file,
            EvaluationUploadRequest request,
            User user
    ) throws IOException {
        // Validate file
        validateFile(file);

        // Generate SHA-256 hash
        String fileHash = generateSHA256Hash(file.getBytes());

        // Check for re-evaluation cache
        Optional<AnswerSubmission> cachedSubmission = submissionRepository
                .findFirstByFileHashAndQuestionAndMarksLimitAndEvaluationStatus(
                        fileHash,
                        request.getQuestion(),
                        request.getMarksLimit(),
                        "COMPLETED"
                );

        if (cachedSubmission.isPresent() && cachedSubmission.get().getUser().getId().equals(user.getId())) {
            log.info("Reusing cached evaluation for user: {}", user.getId());
            AnswerSubmission submission = cachedSubmission.get();
            return EvaluationUploadResponse.builder()
                    .submissionId(submission.getId())
                    .fileUrl(submission.getFileUrl())
                    .fileType(submission.getFileType())
                    .originalFileName(submission.getOriginalFileName())
                    .fileSize(submission.getFileSize())
                    .evaluationStatus(submission.getEvaluationStatus())
                    .build();
        }

        // Upload to Supabase Storage
        String fileUrl = supabaseStorageService.uploadFile(
                user.getId(),
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType()
        );

        // Create submission record
        AnswerSubmission submission = AnswerSubmission.builder()
                .user(user)
                .question(request.getQuestion())
                .topic(request.getTopic())
                .marksLimit(request.getMarksLimit())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileHash(fileHash)
                .evaluationStatus("PENDING")
                .build();

        submission = submissionRepository.save(submission);

        log.info("Answer submitted successfully for user: {}", user.getId());

        return EvaluationUploadResponse.builder()
                .submissionId(submission.getId())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .evaluationStatus("PENDING")
                .build();
    }

    @Transactional
    public EvaluationResponse evaluateAnswer(UUID submissionId, User user) {
        // Fetch submission with ownership validation
        AnswerSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (!submission.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only evaluate your own submissions");
        }

        // Check if already evaluated
        Optional<AnswerEvaluation> existingEvaluation = evaluationRepository.findBySubmissionId(submissionId);
        if (existingEvaluation.isPresent()) {
            AnswerEvaluation evaluation = existingEvaluation.get();
            return parseEvaluationResponse(evaluation);
        }

        try {
            // Call Gemini API
            String geminiResponse = callGeminiAPI(submission);

            // Parse response
            JsonNode responseJson = objectMapper.readTree(geminiResponse);
            JsonNode candidates = responseJson.path("candidates");
            
            if (candidates.isEmpty() || !candidates.get(0).has("content")) {
                throw new RuntimeException("Invalid Gemini API response");
            }

            String content = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Extract JSON from content (Gemini may wrap JSON in markdown)
            String jsonContent = extractJsonFromContent(content);
            JsonNode evaluationJson = objectMapper.readTree(jsonContent);

            // Build evaluation response
            EvaluationResponse response = EvaluationResponse.builder()
                    .submissionId(submissionId)
                    .score(evaluationJson.path("score").asDouble())
                    .maxMarks(evaluationJson.path("maxMarks").asDouble())
                    .conceptualAccuracy(evaluationJson.path("conceptualAccuracy").asText())
                    .technicalCorrectness(evaluationJson.path("technicalCorrectness").asText())
                    .presentation(evaluationJson.path("presentation").asText())
                    .diagramFeedback(evaluationJson.path("diagramFeedback").asText())
                    .improvements(parseStringArray(evaluationJson.path("improvements")))
                    .strengths(parseStringArray(evaluationJson.path("strengths")))
                    .weaknesses(parseStringArray(evaluationJson.path("weaknesses")))
                    .keywordsMissing(parseStringArray(evaluationJson.path("keywordsMissing")))
                    .evaluationJson(jsonContent)
                    .build();

            // Save evaluation to database
            AnswerEvaluation evaluation = AnswerEvaluation.builder()
                    .submission(submission)
                    .score(response.getScore())
                    .maxMarks(response.getMaxMarks())
                    .evaluationJson(jsonContent)
                    .build();

            evaluation = evaluationRepository.save(evaluation);
            response.setEvaluationId(evaluation.getId());

            // Update submission status
            submission.setEvaluationStatus("COMPLETED");
            submissionRepository.save(submission);
            historyAnalyticsService.recordAnswerEvaluation(submission);

            log.info("Evaluation completed successfully for submission: {}", submissionId);

            return response;

        } catch (Exception e) {
            log.error("Gemini API evaluation failed for submission: {}", submissionId, e);
            
            // Update submission status to FAILED
            submission.setEvaluationStatus("FAILED");
            submissionRepository.save(submission);
            
            throw new RuntimeException("Evaluation failed: " + e.getMessage(), e);
        }
    }

    public Page<EvaluationHistoryResponse> getEvaluationHistory(
            User user,
            String searchQuery,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AnswerSubmission> submissions;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            submissions = submissionRepository.searchByQuestion(user.getId(), searchQuery, pageable);
        } else {
            submissions = submissionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        return submissions.map(submission -> {
            Optional<AnswerEvaluation> evaluationOpt = evaluationRepository.findBySubmissionId(submission.getId());
            
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

    public EvaluationResponse getEvaluationById(UUID evaluationId, User user) {
        AnswerEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));

        // Ownership validation
        if (!evaluation.getSubmission().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only view your own evaluations");
        }

        return parseEvaluationResponse(evaluation);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPG, JPEG, PNG, WEBP, and PDF are allowed");
        }
    }

    private String generateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file hash", e);
        }
    }

    private String callGeminiAPI(AnswerSubmission submission) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new RuntimeException(
                    "Gemini API key is not configured. Set GEMINI_API_KEY in backend/.env " +
                    "(get one from https://aistudio.google.com/apikey)."
            );
        }

        byte[] fileBytes = supabaseStorageService.downloadFile(submission.getFileUrl());
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String mimeType = normalizeMimeType(submission.getFileType());
        String prompt = buildEvaluationPrompt(submission);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(Map.of(
                "inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", base64Data
                )
        ));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", parts)));
        requestBody.put("generationConfig", Map.of("responseMimeType", "application/json"));

        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    geminiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Gemini API call failed: {} - {}", e.getStatusCode(), errorBody);
            throw new RuntimeException(
                    "Gemini API error (" + e.getStatusCode() + "): " + errorBody, e
            );
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private String normalizeMimeType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return "application/octet-stream";
        }
        return fileType.toLowerCase().replace("image/jpg", "image/jpeg");
    }

    private String buildEvaluationPrompt(AnswerSubmission submission) {
        return String.format("""
                You are an expert examiner. Evaluate the attached answer image/PDF for the given question.
                
                Question: %s
                Topic: %s
                Maximum Marks: %.1f
                
                Analyze:
                1. Conceptual Accuracy
                2. Completeness
                3. Technical Correctness
                4. Structure
                5. Presentation
                6. Diagram Quality (if present)
                
                Provide a score out of %.1f marks.
                
                Return ONLY a valid JSON response in this exact format:
                {
                    "score": <score>,
                    "maxMarks": %.1f,
                    "conceptualAccuracy": "<detailed feedback>",
                    "technicalCorrectness": "<detailed feedback>",
                    "presentation": "<detailed feedback>",
                    "diagramFeedback": "<feedback if diagram present, else 'No diagram'>",
                    "improvements": ["<improvement 1>", "<improvement 2>"],
                    "strengths": ["<strength 1>", "<strength 2>"],
                    "weaknesses": ["<weakness 1>", "<weakness 2>"],
                    "keywordsMissing": ["<keyword 1>", "<keyword 2>"]
                }
                
                Do not include any markdown formatting or extra text outside the JSON.
                """,
                submission.getQuestion(),
                submission.getTopic() != null ? submission.getTopic() : "General",
                submission.getMarksLimit(),
                submission.getMarksLimit(),
                submission.getMarksLimit()
        );
    }

    private String extractJsonFromContent(String content) {
        // Remove markdown code blocks if present
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private List<String> parseStringArray(JsonNode node) {
        if (node.isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                result.add(item.asText());
            }
            return result;
        }
        return new ArrayList<>();
    }

    private EvaluationResponse parseEvaluationResponse(AnswerEvaluation evaluation) {
        try {
            JsonNode evaluationJson = objectMapper.readTree(evaluation.getEvaluationJson());
            
            return EvaluationResponse.builder()
                    .submissionId(evaluation.getSubmission().getId())
                    .evaluationId(evaluation.getId())
                    .score(evaluation.getScore())
                    .maxMarks(evaluation.getMaxMarks())
                    .conceptualAccuracy(evaluationJson.path("conceptualAccuracy").asText())
                    .technicalCorrectness(evaluationJson.path("technicalCorrectness").asText())
                    .presentation(evaluationJson.path("presentation").asText())
                    .diagramFeedback(evaluationJson.path("diagramFeedback").asText())
                    .improvements(parseStringArray(evaluationJson.path("improvements")))
                    .strengths(parseStringArray(evaluationJson.path("strengths")))
                    .weaknesses(parseStringArray(evaluationJson.path("weaknesses")))
                    .keywordsMissing(parseStringArray(evaluationJson.path("keywordsMissing")))
                    .evaluationJson(evaluation.getEvaluationJson())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse evaluation JSON", e);
            throw new RuntimeException("Failed to parse evaluation data", e);
        }
    }
}
