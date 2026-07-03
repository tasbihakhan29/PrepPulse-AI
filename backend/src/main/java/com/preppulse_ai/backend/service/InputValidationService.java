package com.preppulse_ai.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class InputValidationService {

    private static final Set<String> ACADEMIC_KEYWORDS = new HashSet<>(Arrays.asList(
            "the", "and", "that", "with", "from", "this", "have", "definition",
            "system", "science", "exam", "test", "theory", "data", "process", "result",
            "chapter", "concept", "study", "computer", "network", "database", "programming",
            "structure", "function", "design", "method", "analysis", "learning", "knowledge",
            "question", "answer", "score", "grade", "university", "school", "course", "topic",
            "example", "standard", "model", "application", "problem", "solution", "algorithm",
            "management", "history", "physics", "chemistry", "biology", "math", "literature"
    ));

    private static final Pattern CONSONANT_SEQ = Pattern.compile("[bcdfghjklmnpqrstvwxyz]{5,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHAR = Pattern.compile("(.)\\1{4,}", Pattern.CASE_INSENSITIVE);

    /**
     * Evaluates the academic content and structures of a string.
     * Returns an Academic Confidence Score from 0 to 100.
     */
    public int calculateAcademicConfidenceScore(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        String cleaned = text.trim();
        if (cleaned.length() < 30) {
            // Content is too short to extract a valid structure
            return 10;
        }

        int score = 50; // Base score

        // 1. Basic word splits
        String[] words = cleaned.toLowerCase().split("\\s+");
        int totalWords = words.length;

        // Gibberish length check
        if (totalWords < 5) {
            return 5;
        }

        // 2. Vowel ratio check (English text is typically 35-45% vowels)
        long vowelsCount = cleaned.chars().filter(c -> "aeiouAEIOU".indexOf(c) != -1).count();
        double vowelRatio = (double) vowelsCount / cleaned.length();
        if (vowelRatio < 0.2 || vowelRatio > 0.6) {
            score -= 25; // Skewed letters indicate noise
        } else {
            score += 10;
        }

        // 3. Repeated sequences check (e.g. "aaaaa" or long consonant clusters "kdsjfghs")
        if (CONSONANT_SEQ.matcher(cleaned).find()) {
            score -= 30; // Heavy gibberish penalty
        }
        if (REPEATED_CHAR.matcher(cleaned).find()) {
            score -= 30; // Heavy spam penalty
        }

        // 4. Keyword matches
        int keywordMatches = 0;
        for (String word : words) {
            // Strip punctuation
            String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (ACADEMIC_KEYWORDS.contains(cleanWord)) {
                keywordMatches++;
            }
        }

        double keywordRatio = (double) keywordMatches / totalWords;
        if (keywordRatio > 0.08) {
            score += 25; // High educational vocab density
        } else if (keywordRatio > 0.03) {
            score += 15;
        } else {
            score -= 15; // Lack of standard words
        }

        // 5. Word Length average (English average is ~5 letters)
        double totalLength = 0;
        for (String w : words) {
            totalLength += w.length();
        }
        double avgWordLength = totalLength / totalWords;
        if (avgWordLength > 12 || avgWordLength < 3) {
            score -= 20; // Unusually long or short words (e.g. "abcxyz123")
        } else {
            score += 10;
        }

        // Keep score in bounds 0 - 100
        int finalScore = Math.max(0, Math.min(100, score));
        log.info("Calculated Academic Confidence Score: {} for text length: {}", finalScore, text.length());
        return finalScore;
    }

    /**
     * Validates input quality and throws an exception if score is below threshold (50)
     */
    public void validateInputQuality(String text) {
        int score = calculateAcademicConfidenceScore(text);
        if (score < 50) {
            throw new IllegalArgumentException(
                "We couldn't extract a valid academic structure from this input. " +
                "Please ensure your text or PDF contains relevant educational content."
            );
        }
    }
}
