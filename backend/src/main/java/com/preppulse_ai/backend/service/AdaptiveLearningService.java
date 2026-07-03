package com.preppulse_ai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppulse_ai.backend.entity.TestResult;
import com.preppulse_ai.backend.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveLearningService {

    private final TestResultRepository testResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final double WEAKNESS_THRESHOLD = 70.0;

    /**
     * Identifies the user's weak topics based on past test results.
     * Returns a list of weak topic names where average score is < 70%.
     */
    public List<String> identifyWeakTopics(UUID userId) {
        List<TestResult> results = testResultRepository.findAllByUserIdOrderByCompletedAtDesc(userId);
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // Map to keep track of total score and count per topic
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

        List<String> weakTopics = new ArrayList<>();
        for (String topic : topicTotalScores.keySet()) {
            double average = topicTotalScores.get(topic) / topicCounts.get(topic);
            log.info("Topic: {}, Average Score: {}% over {} test(s)", topic, average, topicCounts.get(topic));
            if (average < WEAKNESS_THRESHOLD) {
                weakTopics.add(topic);
            }
        }

        return weakTopics;
    }

    /**
     * Suggests a focus list for the next exam. If weak topics overlap with material topics,
     * they are prioritized.
     */
    public String buildAdaptiveInstructions(UUID userId) {
        List<String> weakTopics = identifyWeakTopics(userId);
        if (weakTopics.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nAdaptive Focus Notice:\n");
        sb.append("The user has previously demonstrated academic weakness (average score < 70%) in the following topics: ");
        sb.append(String.join(", ", weakTopics)).append(".\n");
        sb.append("Please adjust the question distribution dynamically: make sure approximately 40% of the questions generated cover these specific weak topics to reinforce their understanding, and make these questions slightly more instructional in the explanation fields.");

        return sb.toString();
    }
}
