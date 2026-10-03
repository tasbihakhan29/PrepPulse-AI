package com.preppulse_ai.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Slf4j
public class GroqAiService {

    @Value("${app.groq.api-key:}")
    private String groqApiKey;

    @Value("${app.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public interface TokenConsumer extends Consumer<String> {
        void onComplete(String fullResponse);
        void onError(Throwable t);
    }

    /**
     * Streams chat completions from Groq.
     * Uses mock streaming in development if the API key is missing.
     */
    public void streamCompletion(String systemPrompt, String userPrompt, TokenConsumer consumer) {
        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            log.warn("GROQ_API_KEY is not configured. Falling back to mock stream completions.");
            generateMockStream(consumer);
            return;
        }

        try {
            // Build request JSON
            String requestBody = buildRequestBody(systemPrompt, userPrompt);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Sending streaming request to Groq API (model: {})...", groqModel);
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(response.body()))) {
                    StringBuilder errSb = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errSb.append(line).append("\n");
                    }
                    throw new RuntimeException("Groq API returned error status: " + response.statusCode() + ". Body: " + errSb);
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                StringBuilder fullResponseBuilder = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;

                    if (cleanLine.startsWith("data: ")) {
                        String dataValue = cleanLine.substring(6).trim();
                        if ("[DONE]".equals(dataValue)) {
                            log.info("Groq stream completed successfully.");
                            break;
                        }

                        // Extract content from JSON delta
                        String token = extractTokenContent(dataValue);
                        if (token != null) {
                            fullResponseBuilder.append(token);
                            consumer.accept(token);
                        }
                    }
                }
                consumer.onComplete(fullResponseBuilder.toString());
            }

        } catch (Exception e) {
            log.error("Error during Groq streaming:", e);
            consumer.onError(e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        // Escaping prompts
        String escapedSystem = escapeJsonString(systemPrompt);
        String escapedUser = escapeJsonString(userPrompt);

        return "{"
                + "\"model\": \"" + groqModel + "\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + escapedSystem + "\"},"
                + "  {\"role\": \"user\", \"content\": \"" + escapedUser + "\"}"
                + "],"
                + "\"response_format\": {\"type\": \"json_object\"},"
                + "\"temperature\": 0.3,"
                + "\"stream\": true"
                + "}";
    }

    private String extractTokenContent(String jsonChunk) {
        // Find "content":"..." using index search
        int contentIndex = jsonChunk.indexOf("\"content\":\"");
        if (contentIndex == -1) return null;

        int start = contentIndex + 11;
        StringBuilder tokenBuilder = new StringBuilder();
        for (int i = start; i < jsonChunk.length(); i++) {
            char c = jsonChunk.charAt(i);
            if (c == '\\') {
                if (i + 1 < jsonChunk.length()) {
                    char next = jsonChunk.charAt(i + 1);
                    if (next == '"') {
                        tokenBuilder.append('"');
                        i++;
                    } else if (next == 'n') {
                        tokenBuilder.append('\n');
                        i++;
                    } else if (next == 't') {
                        tokenBuilder.append('\t');
                        i++;
                    } else if (next == '\\') {
                        tokenBuilder.append('\\');
                        i++;
                    } else {
                        tokenBuilder.append(c);
                    }
                }
            } else if (c == '"') {
                break; // End of string value
            } else {
                tokenBuilder.append(c);
            }
        }
        return tokenBuilder.toString();
    }

    private String escapeJsonString(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void generateMockStream(TokenConsumer consumer) {
        log.info("Initializing high-fidelity Mock AI response stream...");
        String mockResponse = "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"question\": \"Under which conditions does the Dijkstra's Shortest Path algorithm fail to yield a correct topological representation?\",\n" +
                "      \"questionType\": \"MCQ\",\n" +
                "      \"options\": [\n" +
                "        \"When the source vertex has self-loops\",\n" +
                "        \"When there are negative edge weights along reachable pathways\",\n" +
                "        \"When the target vertex is isolated\",\n" +
                "        \"When edge weights are fractional values greater than zero\"\n" +
                "      ],\n" +
                "      \"correctAnswer\": \"When there are negative edge weights along reachable pathways\",\n" +
                "      \"explanation\": \"Dijkstra's greedy choice assumes that once a vertex is visited, its shortest distance path is finalized. Negative weights violate this assumption as subsequent paths could yield shorter paths.\",\n" +
                "      \"topic\": \"Graph Algorithms\",\n" +
                "      \"difficulty\": \"Medium\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"question\": \"Which normal forms eliminate transitive functional dependencies in relational database design?\",\n" +
                "      \"questionType\": \"MSQ\",\n" +
                "      \"options\": [\n" +
                "        \"Third Normal Form (3NF)\",\n" +
                "        \"Boyce-Codd Normal Form (BCNF)\",\n" +
                "        \"Fourth Normal Form (4NF)\",\n" +
                "        \"Domain-Key Normal Form (DKNF)\"\n" +
                "      ],\n" +
                "      \"correctAnswer\": \"Third Normal Form (3NF),Boyce-Codd Normal Form (BCNF)\",\n" +
                "      \"explanation\": \"Third Normal Form and Boyce-Codd Normal Form eliminate transitive dependencies through progressively stronger dependency rules.\",\n" +
                "      \"topic\": \"Database Management Systems\",\n" +
                "      \"difficulty\": \"Easy\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"question\": \"A TLB lookup takes 2 nanoseconds. What is the lookup time in nanoseconds?\",\n" +
                "      \"questionType\": \"Numerical\",\n" +
                "      \"options\": [\n" +
                "        \"\"\n" +
                "      ],\n" +
                "      \"correctAnswer\": \"2\",\n" +
                "      \"explanation\": \"The stated lookup time is 2 nanoseconds.\",\n" +
                "      \"topic\": \"Operating Systems\",\n" +
                "      \"difficulty\": \"Hard\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        new Thread(() -> {
            try {
                // Simulate typing latency
                int chunkSize = 20;
                for (int i = 0; i < mockResponse.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, mockResponse.length());
                    String chunk = mockResponse.substring(i, end);
                    consumer.accept(chunk);
                    Thread.sleep(70);
                }
                consumer.onComplete(mockResponse);
            } catch (Exception e) {
                consumer.onError(e);
            }
        }).start();
    }
}
